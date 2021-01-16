package org.top.core.log;

import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;
import org.top.clientapi.entity.SubmitResponse;
import org.top.core.machine.MachineResult;

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
    private LinkedBlockingQueue<MachineResult> blockingQueue = new LinkedBlockingQueue<>();
    private Map<String, Channel> map = new ConcurrentHashMap<>();

    private LogIndexSemaphore() {

    }

    public static LogIndexSemaphore getInstance() {
        return logIndexSemaphore;
    }

    /**
     * 增加消息处理监听
     *
     * @param id      消息id
     * @param channel 连接通道
     */
    public void addListener(String id, Channel channel) {
        map.put(id, channel);
    }

    /**
     * 移除监听
     *
     * @param channel 通道
     */
    public void remove(Channel channel) {
        map.entrySet()
                .removeIf(entry -> entry.getValue().equals(channel));
    }

    /**
     * 启动消息推送循环
     */
    public void startLoop() {
        new Thread(() -> {
            //noinspection InfiniteLoopStatement
            for (; ; ) {
                try {
                    MachineResult indexData = blockingQueue.take();
                    Channel channel = map.remove(indexData.getId());
                    if (channel != null) {
                        channel.writeAndFlush(new SubmitResponse(indexData.getState(), null, indexData.getId(), indexData.getData()));
                    }
                } catch (InterruptedException e) {
                    log.error(e.getMessage(), e);
                }
            }
        }, "operation-facade-thread").start();
    }

    /**
     * 放入已处理完毕消息
     *
     * @param result 处理结果
     */
    public void offer(MachineResult result) {
        blockingQueue.offer(result);
    }
}
