package org.top.core.machine;


import lombok.extern.slf4j.Slf4j;
import org.top.core.RaftServerData;
import org.top.core.ServerStateEnum;
import org.top.core.SnapshotExec;
import org.top.core.log.LogIndexSemaphore;
import org.top.models.LogEntry;
import org.top.models.PersistentStateModel;
import org.top.models.ServerStateModel;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author lubeilin
 * @date 2020/11/12
 */
@Slf4j
public class StateMachineHandlerImpl implements StateMachineHandler {
    private StateMachine stateMachine = new KvStateMachineImpl();

    private static Semaphore semaphore = new Semaphore(0);

    private PersistentStateModel model = PersistentStateModel.getModel();
    ThreadPoolExecutor executor = new ThreadPoolExecutor(1, 1, 1000, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(1)
            , r -> new Thread(r, "stateMachine-thread"));

    @Override
    public void commit() throws Exception {
        ServerStateModel serverState = RaftServerData.serverState;
        //所有服务器
        //如果commitIndex > lastApplied，那么就 lastApplied 加一，并把log[lastApplied]应用到状态机中（5.3 节）
        for (long i = serverState.getLastApplied() + 1; i <= serverState.getCommitIndex(); i++) {
            LogEntry last = model.getLog(i);
            stateMachine.execute(last);
            serverState.setLastApplied(serverState.getLastApplied() + 1);
            if (RaftServerData.serverStateEnum == ServerStateEnum.LEADER) {
                new LogIndexSemaphore(serverState.getLastApplied()).signalAll();
            }
        }
        //生成快照
        SnapshotExec.getInstance().apply();
    }

    @Override
    public void loop() {
        executor.execute(() -> {
            //noinspection InfiniteLoopStatement
            for (; ; ) {
                try {
                    semaphore.acquire();
                    commit();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

    }

    @Override
    public void start() {
        semaphore.release();
    }
}
