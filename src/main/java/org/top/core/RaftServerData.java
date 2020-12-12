package org.top.core;

import lombok.extern.slf4j.Slf4j;
import org.top.models.LeaderStateModel;
import org.top.models.PersistentStateModel;
import org.top.models.ServerStateModel;
import org.top.rpc.Node;

import java.util.concurrent.locks.ReentrantLock;

/**
 * @author lubeilin
 * @date 2020/11/17
 */
@Slf4j
public class RaftServerData {
    public static volatile ServerStateEnum serverStateEnum = ServerStateEnum.FOLLOWER;
    /**
     * 主节点
     */
    public static volatile Node leaderId;
    /**
     * 是否繁忙，为true时不会触发选举超时
     */
    public static volatile boolean isBusy = false;
    /**
     * 主节点上一次心跳的时间戳
     */
    public static volatile long heartbeatTime;
    private static volatile boolean up;

    public static void leaderUp() {
        if (serverStateEnum == ServerStateEnum.LEADER) {
            log.info("开启服务");
            up = true;
        }
    }

    public static void leaderDown() {
        up = false;
    }

    public static boolean isUp() {
        return up;
    }

    public static ServerStateModel serverState = new ServerStateModel();
    public static LeaderStateModel leaderState;

    public static void initLeader() throws Exception {
        leaderState = new LeaderStateModel(PersistentStateModel.getModel().getLastIndex());
    }

    /**
     * 选举和接收日志都会用这个锁
     */
    public static ReentrantLock lock = new ReentrantLock();
}
