package org.top.core.log;

import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;
import org.top.clientapi.entity.SubmitResponse;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * 主节点日志写入信号量
 *
 * @author lubeilin
 * @date 2020/11/20
 */
@Slf4j
public class LogIndexSemaphore {
    private static LogIndexSemaphore logIndexSemaphore = new LogIndexSemaphore();
    private LinkedBlockingQueue<IndexData> blockingQueue = new LinkedBlockingQueue<>();
    private Map<String, Channel> map = new ConcurrentHashMap<>();

    private LogIndexSemaphore() {

    }

    public static LogIndexSemaphore getInstance() {
        return logIndexSemaphore;
    }

    public void addListener(String id, Channel channel) {
        map.put(id, channel);
    }

    public void remove(Channel channel) {
        map.entrySet()
                .removeIf(entry -> entry.getValue().equals(channel));
    }

    public void startLoop() {
        new Thread(() -> {
            //noinspection InfiniteLoopStatement
            for (; ; ) {
                try {
                    IndexData indexData = blockingQueue.take();
                    Channel channel = map.remove(indexData.id);
                    if (channel != null) {
                        if (indexData.success) {
                            channel.writeAndFlush(new SubmitResponse(SubmitResponse.SUCCESS, null, indexData.id, indexData.data));
                        } else {
                            channel.writeAndFlush(new SubmitResponse(SubmitResponse.FAIL, null, indexData.id, indexData.data));
                        }
                    }
                } catch (InterruptedException e) {
                    log.error(e.getMessage(), e);
                }
            }
        }, "operation-facade-thread").start();
    }

    public void offer(String id, boolean success, byte[] data) {
        IndexData indexData = new IndexData(id, success, data);
        blockingQueue.offer(indexData);
    }

    static class IndexData {
        private volatile String id;
        private volatile boolean success;
        private volatile byte[] data;

        public IndexData(String id, boolean success, byte[] data) {
            this.id = id;
            this.success = success;
            this.data = data;
        }
    }
}
