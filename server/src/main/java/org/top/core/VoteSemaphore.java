package org.top.core;

import org.top.rpc.NodeGroup;
import org.top.rpc.utils.PropertiesUtil;

import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * 投票信号量
 *
 * @author lubeilin
 * @date 2020/11/26
 */
public class VoteSemaphore {
    private static long start = PropertiesUtil.getLong("election_out_time_start");
    private static long end = PropertiesUtil.getLong("election_out_time_end");
    /**
     * 记录当前票数
     */
    private static Semaphore semaphore = new Semaphore(0);
    /**
     * 当前选举id
     */
    private static volatile long thisId;

    /**
     * 重置选票，开启一轮新的选举
     *
     * @param id 选举id
     */
    public static synchronized void reset(long id) {
        thisId = id;
        semaphore.drainPermits();
    }

    /**
     * 投票，只有选举id一致投票才有效
     *
     * @param id 选举id
     */
    public static synchronized void vote(long id) {
        if (thisId == id) {
            semaphore.release();
        }
    }

    /**
     * 等待投票结束，获取选举结果
     *
     * @return 选举结果
     * @throws InterruptedException
     */
    public static boolean tryAcquire() throws InterruptedException {
        long outTime = ThreadLocalRandom.current().nextLong(start, end);
        return semaphore.tryAcquire(NodeGroup.getNodeGroup().majority(), outTime, TimeUnit.MILLISECONDS);
    }
}
