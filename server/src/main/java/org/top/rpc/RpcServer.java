package org.top.rpc;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.extern.slf4j.Slf4j;
import org.top.clientapi.handler.FacadeHandler;
import org.top.clientapi.handler.PingHandler;
import org.top.rpc.codec.MessageDecoder;
import org.top.rpc.codec.MessageEncoder;
import org.top.rpc.handler.AppendReqHandler;
import org.top.rpc.handler.SnapshotReqHandler;
import org.top.rpc.handler.VoteReqHandler;
import org.top.rpc.utils.PropertiesUtil;

/**
 * 开启端口监听
 *
 * @author lubeilin
 * @date 2020/11/17
 */
@Slf4j
public class RpcServer {
    public void start() throws InterruptedException {
        NioEventLoopGroup bossGroup = new NioEventLoopGroup();
        NioEventLoopGroup workerGroup = new NioEventLoopGroup();
        ServerBootstrap bootstrap1 = new ServerBootstrap();
        bootstrap1.group(bossGroup, workerGroup);
        bootstrap1.channel(NioServerSocketChannel.class);
        bootstrap1.childHandler(new ChannelInitializer<Channel>() {
            @Override
            protected void initChannel(Channel ch) {
                ChannelPipeline pipeline = ch.pipeline();
                pipeline.addLast(new MessageEncoder());
                pipeline.addLast(new MessageDecoder());
                pipeline.addLast(new VoteReqHandler());
                pipeline.addLast(new AppendReqHandler());
                pipeline.addLast(new SnapshotReqHandler());
                pipeline.addLast(new PingHandler());
                pipeline.addLast(new FacadeHandler());
            }
        });
        int port = PropertiesUtil.getInt("port");
        bootstrap1.bind(port).sync().channel();
        log.info("raft服务启动，端口：{}", port);
    }
}
