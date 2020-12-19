package org.top.core;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.top.core.machine.KvStateMachineImpl;
import org.top.core.machine.SnapshotService;
import org.top.exception.RaftException;
import org.top.models.LogEntry;
import org.top.models.PersistentStateModel;
import org.top.rpc.Node;
import org.top.rpc.utils.PropertiesUtil;

import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 快照生成类
 *
 * @author lubeilin
 * @date 2020/12/2
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SnapshotExec {
    private static SnapshotExec snapshotExec = new SnapshotExec();
    private final int snapshotNum = PropertiesUtil.getInt("snapshot_num");
    /**
     * 暂停生成快照的时间
     */
    private final long waitTime = PropertiesUtil.getLong("snapshot_wait_time");
    ThreadPoolExecutor executor = new ThreadPoolExecutor(1, 1, 1000, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(1)
            , r -> {
        Thread thread = new Thread(r, "snapshot-thread");
        //快照线程设置较低优先级
        thread.setPriority(2);
        return thread;
    });
    private SnapshotService snapshotService = new KvStateMachineImpl();
    private PersistentStateModel model = PersistentStateModel.getModel();
    private ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    @Getter
    private Lock readLock = readWriteLock.readLock();
    @Getter
    private Lock writeLock = readWriteLock.writeLock();
    private Condition conditionRun = writeLock.newCondition();
    /**
     * 节点开始读取日志
     */
    private Map<Node, Boolean> readMap = new ConcurrentHashMap<>();
    /**
     * 最后一次结束读取日志的时间戳
     */
    private volatile long readEndTime;

    public static SnapshotExec getInstance() {
        return snapshotExec;
    }

    public void saveLoop() {
        executor.execute(() -> {
            log.info("快照生成线程启动");
            //noinspection InfiniteLoopStatement
            for (; ; ) {
                try {
                    run();
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            }
        });
    }


    public void run() throws Exception {
        writeLock.lock();
        try {
            long index = snapshotService.snapshotLastIndex();
            if (index > 0) {
                try {
                    LogEntry logEntry = model.getLog(index);
                    snapshotService.save(logEntry);
                    model.remove(index - 1);
                } catch (RaftException e) {
                    log.info("命令已执行");
                }
            }
            //noinspection InfiniteLoopStatement
            for (; ; ) {
                index++;
                //有节点在读需要暂停，接着生成的快照和旧快照会冲突
                // 上一次读取结束的时间没超过预期值也要暂停，防止节点还没来得及同步新数据又生成了新的快照
                // 最后一条已经应用到状态机的日志和快照太接近也需要暂停
                while (readMap.size() > 0
                        || readEndTime > System.currentTimeMillis() - waitTime
                        || RaftServerData.serverState.getLastApplied() <= index + snapshotNum / 10) {
                    conditionRun.await();
                    index = snapshotService.snapshotLastIndex() + 1;
                }
                LogEntry logEntry = model.getLog(index);
                snapshotService.save(logEntry);
                log.info("生成快照：{}", logEntry);
                model.remove(index - 1);
            }
        } finally {
            writeLock.unlock();
        }
    }

    public void apply() throws Exception {
        if (readMap.size() <= 0
                && RaftServerData.serverState.getLastApplied() >= snapshotService.snapshotLastIndex() + snapshotNum
                && readEndTime <= System.currentTimeMillis() - waitTime
                && writeLock.tryLock()) {
            try {
                conditionRun.signal();
            } finally {
                writeLock.unlock();
            }
        }
    }

    /**
     * 开始读取快照
     *
     * @param node 节点
     */
    public void read(Node node) {
        readMap.put(node, true);
    }

    /**
     * 是否在读取快照
     *
     * @param node 节点
     * @return true/false
     */
    public boolean isRead(Node node) {
        return readMap.containsKey(node);
    }

    /**
     * 结束读取快照
     * 这里要延迟释放，避免又生成了新快照把旧日志删掉了，照成节点日志再次落后而需要发送快照
     *
     * @param node 节点
     */
    public void unRead(Node node) {
        if (readMap.remove(node) != null) {
            readEndTime = System.currentTimeMillis();
        }
    }
}
