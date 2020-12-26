package org.top.core.machine;


import lombok.extern.slf4j.Slf4j;
import org.top.core.RaftServerData;
import org.top.core.ServerStateEnum;
import org.top.core.SnapshotExec;
import org.top.core.log.LogIndexSemaphore;
import org.top.models.PersistentStateModel;
import org.top.models.ServerStateModel;

import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author lubeilin
 * @date 2020/11/12
 */
@Slf4j
public class StateMachineHandlerImpl implements StateMachineHandler {
    private static Semaphore semaphore = new Semaphore(0);
    private static StateMachineHandlerImpl stateMachineHandler = new StateMachineHandlerImpl();
    private StateMachine stateMachine = new KvStateMachineImpl();
    private PersistentStateModel model = PersistentStateModel.getModel();
    private ReentrantLock lock = new ReentrantLock();

    private StateMachineHandlerImpl() {

    }

    public static StateMachineHandlerImpl getInstance() {
        return stateMachineHandler;
    }

    @Override
    public void commit() throws Exception {
        ServerStateModel serverState = RaftServerData.serverState;
        //所有服务器
        //如果commitIndex > lastApplied，那么就 lastApplied 加一，并把log[lastApplied]应用到状态机中（5.3 节）
        for (long i = serverState.getLastApplied() + 1; i <= serverState.getCommitIndex(); i++) {
            MachineResult result = stateMachine.execute(model.getLog(i));
            serverState.setLastApplied(i);
            if (RaftServerData.serverStateEnum == ServerStateEnum.LEADER) {
                LogIndexSemaphore.getInstance().offer(result);
            }
        }
        SnapshotExec.getInstance().apply();
    }

    @Override
    public void startLoop() {
        new Thread(() -> {
            //noinspection InfiniteLoopStatement
            for (; ; ) {
                try {
                    semaphore.acquire();
                    lock.lock();
                    try {
                        commit();
                    } finally {
                        lock.unlock();
                    }
                } catch (Exception e) {
                    log.error("状态机执行异常", e);
                }
            }
        }, "stateMachine-thread").start();
    }

    @Override
    public void start() {
        semaphore.release();
    }

    @Override
    public void awaitPause() {
        lock.lock();
        try {
            //获取到了一次锁，说明循环暂停了，此方法配合重置状态机使用
        } finally {
            lock.unlock();
        }
    }
}
