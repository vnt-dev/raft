package org.top.core;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.top.clientapi.entity.SubmitRequest;
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

    public void startLoop() {
        new Thread(() -> {
            log.info("追加日志线程启动");
            //noinspection InfiniteLoopStatement
            for (; ; ) {
                try {
                    SubmitRequest request = blockingQueue.take();
                    if (RaftServerData.serverStateEnum != ServerStateEnum.LEADER) {
                        blockingQueue.clear();
                        continue;
                    }
                    do {
                        PersistentStateModel model = PersistentStateModel.getModel();
                        LogEntry logEntry = new LogEntry();
                        logEntry.setKey(request.getKey());
                        logEntry.setId(request.getId());
                        logEntry.setVal(request.getVal());
                        logEntry.setOption(request.getOption());
                        model.pushLast(logEntry);
                    } while ((request = blockingQueue.poll()) != null);
                    appendEntriesComponent.broadcastAppendEntries();
                } catch (Exception e) {
                    log.info("追加日志失败", e);
                }
            }
        }, "append-log-thread").start();
    }

    public void signal(SubmitRequest msg) {
        blockingQueue.offer(msg);
    }
}
