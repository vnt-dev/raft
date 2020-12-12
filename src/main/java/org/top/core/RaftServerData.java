package org.top.core;

import org.top.models.LeaderStateModel;
import org.top.models.PersistentStateModel;
import org.top.models.ServerStateModel;
import org.top.rpc.Node;

import java.util.concurrent.locks.ReentrantLock;

/**
 * @author lubeilin
 * @date 2020/11/17
 */
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
