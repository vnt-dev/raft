package org.top.core.log;

import lombok.extern.slf4j.Slf4j;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * 主节点日志写入信号量
 *
 * @author lubeilin
 * @date 2020/11/20
 */
@Slf4j
public class LogIndexSemaphore {
    private static ConcurrentHashMap<Long, IndexNode> nodeMap = new ConcurrentHashMap<>();

    public void addListener(long index) {
        nodeMap.put(index, new IndexNode(index, new Semaphore(0), null));
    }

    public IndexNode getData(long index) {
        return nodeMap.get(index);
    }

    public void remove(long index) {
        nodeMap.remove(index);
    }

    /**
     * 等待通知
     *
     * @param time 最大等待时间
     * @param unit 单位
     * @return 是否提前被唤醒
     * @throws InterruptedException 中断
     */
    public boolean await(long index, long time, TimeUnit unit) throws InterruptedException {
        IndexNode indexNode = nodeMap.get(index);
        if (indexNode == null) {
            return false;
        }
        return indexNode.semaphore.tryAcquire(time, unit);
    }

    /**
     * 唤醒第一个
     */
    public void signal(long index, boolean success, byte[] data) {
        IndexNode indexNode = nodeMap.get(index);
        if (indexNode != null) {
            indexNode.success = success;
            indexNode.data = data;
            indexNode.semaphore.release();
        }
    }

    public static class IndexNode implements Comparable<IndexNode> {
        volatile long index;
        volatile boolean success;
        volatile byte[] data;
        volatile Semaphore semaphore;

        public IndexNode(long index, Semaphore semaphore, byte[] data) {
            this.index = index;
            this.semaphore = semaphore;
            this.data = data;
        }

        @Override
        public int compareTo(IndexNode o) {
            return Long.compare(index, o.index);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof IndexNode)) {
                return false;
            }
            IndexNode indexNode = (IndexNode) o;
            return index == indexNode.index;
        }

        @Override
        public int hashCode() {
            return Objects.hash(index);
        }
    }
}
