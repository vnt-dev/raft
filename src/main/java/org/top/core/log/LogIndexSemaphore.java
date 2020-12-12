package org.top.core.log;

import lombok.extern.slf4j.Slf4j;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 主节点日志写入信号量
 *
 * @author lubeilin
 * @date 2020/11/20
 */
@Slf4j
public class LogIndexSemaphore {
    private static ReentrantLock lock = new ReentrantLock();
    private static LinkedList<Node> linkedList = new LinkedList<>();
    /**
     * 当前提交的最大日志索引
     */
    private volatile static long maxIndex;
    /**
     * 需要监听的日志索引
     */
    private long index;

    public LogIndexSemaphore(long index) {
        this.index = index;
    }

    /**
     * 等待通知
     *
     * @param time 最大等待时间
     * @param unit 单位
     * @return 是否提前被唤醒
     * @throws InterruptedException 中断
     */
    public boolean await(long time, TimeUnit unit) throws InterruptedException {
        if (index <= maxIndex) {
            return true;
        }
        lock.lock();
        try {
            if (index <= maxIndex) {
                return true;
            }
            Node node = new Node(index, lock.newCondition());
            linkedList.addLast(node);
            return node.condition.await(time, unit);
        } finally {
            lock.unlock();
        }
    }

    /**
     * 唤醒当前索引往前的所有等待线程
     */
    public void signalAll() {
        lock.lock();
        try {
            Iterator<Node> iterator = linkedList.iterator();
            if (maxIndex < index) {
                maxIndex = index;
            }
            while (iterator.hasNext()) {
                Node node = iterator.next();
                if (node.index <= index) {
                    iterator.remove();
                    node.condition.signalAll();
                }
            }
        } finally {
            lock.unlock();
        }

    }

    private static class Node implements Comparable<Node> {
        long index;
        Condition condition;

        public Node(long index, Condition condition) {
            this.index = index;
            this.condition = condition;
        }

        @Override
        public int compareTo(Node o) {
            return Long.compare(index, o.index);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Node)) {
                return false;
            }
            Node node = (Node) o;
            return index == node.index;
        }

        @Override
        public int hashCode() {
            return Objects.hash(index);
        }
    }
}
