package org.top.core;

import lombok.extern.slf4j.Slf4j;
import org.top.core.log.LogIndexSemaphore;
import org.top.core.machine.StateMachineHandlerImpl;
import org.top.rpc.RpcServer;

import java.io.File;

/**
 * @author lubeilin
 * @date 2020/11/10
 */
@Slf4j
public class ServerStateTransformerStarter {
    public void start() {
        try {
            log.info("version 0.0.1");
            File dataPath = new File("./data");
            if (!dataPath.exists()) {
                dataPath.mkdir();
            }
            //状态机循环
            StateMachineHandlerImpl.getInstance().startLoop();
            //快照循环
            SnapshotExec.getInstance().startLoop();
            //日志持久化循环，仅主节点使用
            AppendLogEntriesExec.getInstance().startLoop();
            //日志附加成功通知循环，仅主节点使用
            LogIndexSemaphore.getInstance().startLoop();
            //心跳发送循环
            AppendEntriesComponent.getInstance().startHeartbeatLoop();
            //raft端口监听
            new RpcServer().start();
            //raft状态轮转，从FOLLOWER开始
            ServerStateTransformer stateTransformer = AbstractServerStateTransformer.getServer(ServerStateEnum.FOLLOWER);
            for (; ; ) {
                stateTransformer.execute();
                stateTransformer = AbstractServerStateTransformer.getServer(stateTransformer.nextState());
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            System.exit(0);
        }

    }

}
