package org.top.rpc;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.top.clientapi.util.PropertiesUtil;
import org.top.rpc.codec.BaseMessage;
import org.top.rpc.codec.MessageDecoder;
import org.top.rpc.codec.MessageEncoder;
import org.top.rpc.entity.Node;
import org.top.rpc.handler.ClientHandler;
import org.top.rpc.handler.PongHandler;

import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * @author lubeilin
 * @date 2020/11/25
 */
@Slf4j
public class ApiClient {
    private static ApiClient apiClient = new ApiClient();
    private EventLoopGroup loopGroup = new NioEventLoopGroup();
    private Map<Node, Channel> map = new ConcurrentHashMap<>();
    private ArrayList<Node> nodeList = new ArrayList<>();
    private volatile Node leader;
    private int index = 0;

    private ApiClient() {
        String nodesStr = PropertiesUtil.getString("nodes");
        if (StringUtils.isNotEmpty(nodesStr)) {
            String[] nodeStrArr = nodesStr.split(",");
            for (String nodeStr : nodeStrArr) {
                String[] ipAndPort = nodeStr.split(":");
                Node node = new Node(ipAndPort[0], Integer.parseInt(ipAndPort[1]));
                nodeList.add(node);
            }
        }
    }

    public static ApiClient getApiClient() {
        return apiClient;
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
                .option(ChannelOption.SO_REUSEADDR, true)
                .handler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel ch) {
                        ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast(new MessageEncoder());
                        pipeline.addLast(new MessageDecoder());
                        pipeline.addLast(new IdleStateHandler(0, 5000, 0, TimeUnit.MILLISECONDS));
                        pipeline.addLast(new PongHandler());
                        pipeline.addLast(new ClientHandler());
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


    public void send(BaseMessage msg) {
        Channel channel = check(0);
        channel.writeAndFlush(msg);
    }

    public void setLeader(Node leader) {
        if (!Objects.equals(leader, this.leader)) {
            synchronized (this) {
                this.leader = leader;
            }
        }
    }

    public void resetLeader() {
        synchronized (this) {
            index = (index + 1) % nodeList.size();
            leader = null;
        }
    }

    private synchronized Channel check(int num) {
        if (num > nodeList.size()) {
            throw new RuntimeException("连接失败");
        }
        if (leader == null) {
            leader = nodeList.get(index);
        }
        Channel channel = connect(leader);
        if (!channel.isActive()) {
            index = (index + 1) % nodeList.size();
            leader = null;
            return check(num + 1);
        }
        return channel;
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
