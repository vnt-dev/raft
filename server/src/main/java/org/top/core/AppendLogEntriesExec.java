package org.top.core;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.top.clientapi.entity.SubmitRequest;
import org.top.exception.RaftException;
import org.top.models.LogEntry;
import org.top.models.PersistentStateModel;

import java.util.concurrent.LinkedBlockingQueue;

/**
 * 追加日志条目执行器
 *
 * @author lubeilin
 * @date 2020/12/21
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class AppendLogEntriesExec {
    private static AppendLogEntriesExec logEntriesExec = new AppendLogEntriesExec();
    private AppendEntriesComponent appendEntriesComponent = AppendEntriesComponent.getInstance();
    private LinkedBlockingQueue<SubmitRequest> blockingQueue = new LinkedBlockingQueue<>();

    public static AppendLogEntriesExec getInstance() {
        return logEntriesExec;
    }

    /**
     * 启动追加日志循环
     */
    public void startLoop() {
        new Thread(() -> {
            log.info("追加日志线程启动");
            //noinspection InfiniteLoopStatement
            //使用双重循环来动态调整一次rpc中包含的日志数量
            for (; ; ) {
                try {
                    SubmitRequest request = blockingQueue.take();
                    if (RaftServerData.serverStateEnum != ServerStateEnum.LEADER) {
                        blockingQueue.clear();
                        continue;
                    }
                    //优先将缓存的日志进行持久化
                    do {
                        PersistentStateModel model = PersistentStateModel.getModel();
                        LogEntry logEntry = new LogEntry();
                        logEntry.setKey(request.getKey());
                        logEntry.setId(request.getId());
                        logEntry.setVal(request.getVal());
                        logEntry.setOption(request.getOption());
                        model.pushLast(logEntry);
                    } while ((request = blockingQueue.poll()) != null);
                    //持久化结束才进行日志广播，如果上面循环执行完了，说明接收日志的速度<持久化日志的速度，这时候马上广播日志可以降低响应延迟
                    // 如果上面的循环一直执行，说明并发量太大已经没办法主动广播日志了，
                    // 这时候会由心跳进行被动广播，从而增大吞吐量
                    appendEntriesComponent.broadcastAppendEntries();
                } catch (Exception e) {
                    log.info("追加日志失败", e);
                }
            }
        }, "append-log-thread").start();
    }

    public void signal(SubmitRequest msg) {
        if (!blockingQueue.offer(msg)) {
            throw new RaftException("缓存日志已满");
        }
    }
}
