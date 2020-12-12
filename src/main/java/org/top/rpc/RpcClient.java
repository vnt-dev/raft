package org.top.rpc;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;
import org.top.rpc.codec.BaseMessage;
import org.top.rpc.codec.MessageDecoder;
import org.top.rpc.codec.MessageEncoder;
import org.top.rpc.handler.AppendResHandler;
import org.top.rpc.handler.SnapshotResHandler;
import org.top.rpc.handler.VoteResHandler;
import org.top.rpc.utils.PropertiesUtil;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author lubeilin
 * @date 2020/11/17
 */
@Slf4j
public class RpcClient {
    private static RpcClient rpcClient = new RpcClient();
    private EventLoopGroup loopGroup = new NioEventLoopGroup();
    private Map<Node, Channel> map = new ConcurrentHashMap<>();
    private NodeGroup nodeGroup = NodeGroup.getNodeGroup();
    private Map<Node, ReentrantLock> nodeLockMap = new ConcurrentHashMap<>();

    private RpcClient() {

    }

    public static RpcClient getRpcClient() {
        return rpcClient;
    }

    private Channel connect(Node node) {
        return map.computeIfAbsent(node, k ->
                connect0(node)
        );
    }

    private Channel connect0(Node node) {
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(loopGroup)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel ch) {
                        ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast(new MessageEncoder());
                        pipeline.addLast(new MessageDecoder());
                        pipeline.addLast(new IdleStateHandler(0, PropertiesUtil.getLong("heartbeat"), 0, TimeUnit.MILLISECONDS));
                        pipeline.addLast(new VoteResHandler());
                        pipeline.addLast(new AppendResHandler());
                        pipeline.addLast(new SnapshotResHandler());
                    }
                });
        ChannelFuture client = bootstrap.connect(node.getIp(), node.getPort());
        try {
            return client.await().channel();
        } catch (Exception e) {
            log.info("连接失败，{}", node);
        }
        return client.channel();
    }

    public void reConn() {
        nodeGroup.parallelForEach(node -> {
            Channel channel = connect(node);
            if (!channel.isActive()) {
                Channel channelNew = connect0(node);
                map.put(node, channelNew);
            }
        });
    }

    public void sendAll(Exec exec) {
        nodeGroup.parallelForEach(node -> {
            try {
                Channel channel = connect(node);
                if (channel.isActive()) {
                    ReentrantLock lock = nodeLockMap.computeIfAbsent(node, node1 -> new ReentrantLock());
                    lock.lock();
                    try {
                        send(channel, exec.exe(node));
                    } finally {
                        lock.unlock();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public void sendAll(Exec exec, NotConnEvent event) {
        nodeGroup.parallelForEach(node -> {
            try {
                Channel channel = connect(node);
                if (channel.isActive()) {
                    ReentrantLock lock = nodeLockMap.computeIfAbsent(node, node1 -> new ReentrantLock());
                    lock.lock();
                    try {
                        send(channel, exec.exe(node));
                    } finally {
                        lock.unlock();
                    }
                } else {
                    event.exe(node);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public void send(Channel channel, BaseMessage msg) {
        if (msg == null) {
            return;
        }
        channel.writeAndFlush(msg);
    }

    public void close() {
        map.values().forEach(e -> {
            try {
                e.close().await();
            } catch (Exception ex) {
                log.info("关闭失败");
            }
        });
    }

    public interface NotConnEvent {
        /**
         * 断开事件
         *
         * @param node
         */
        void exe(Node node);
    }

    public interface Exec {
        /**
         * 发送
         *
         * @param node 节点
         * @return 发送消息
         * @throws Exception 异常
         */
        BaseMessage exe(Node node) throws Exception;
    }
}
