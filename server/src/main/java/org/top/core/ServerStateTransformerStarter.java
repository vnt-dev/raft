package org.top.core;

import lombok.extern.slf4j.Slf4j;
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
            new RpcServer().start();
            new StateMachineHandlerImpl().loop();
            SnapshotExec.getInstance().saveLoop();
            AppendLogEntriesExec.getInstance().loop();
            // follower 为入口
            ServerStateTransformer followerState = AbstractServerStateTransformer.getServer(ServerStateEnum.FOLLOWER);
            followerState.execute();
        } catch (Exception e) {
            log.info(e.getMessage(), e);
        }

    }

}
