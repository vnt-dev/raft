package org.top.core;

import org.top.rpc.Node;
import org.top.rpc.NodeGroup;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author lubeilin
 * @date 2020/12/10
 */
public class ClientNum {
    /**
     * 连接的节点数目，用于主节点判断当前是否有n/2以上节点在线
     */
    private static ConcurrentHashMap<Node, Boolean> map = new ConcurrentHashMap<>();
    private static ReentrantLock lock = new ReentrantLock();
    private static Condition condition = lock.newCondition();

    /**
     * 等待连接断开
     *
     * @throws InterruptedException 中断
     */
    public static void closeAwait() throws InterruptedException {
        lock.lock();
        try {
            if (map.size() == NodeGroup.getNodeGroup().size()
                    && RaftServerData.serverStateEnum == ServerStateEnum.LEADER) {
                condition.await();
            }
        } finally {
            lock.unlock();
        }
    }

    public static int getNum() {
        return map.size();
    }

    public static void add(Node node) {
        map.put(node, true);
    }

    /**
     * 通知连接已断开
     */
    public static void closeNotifyAll(Node node) {
        lock.lock();
        try {
            map.remove(node);
            condition.signalAll();
        } finally {
            lock.unlock();
        }
    }

    /**
     * 切换为跟随者
     */
    public static void convertFollower() {
        lock.lock();
        try {
            condition.signalAll();
        } finally {
            lock.unlock();
        }
    }
}
