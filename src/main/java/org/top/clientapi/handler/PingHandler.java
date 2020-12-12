package org.top.clientapi.handler;

import io.netty.channel.ChannelHandlerContext;
import org.top.clientapi.entity.Ping;
import org.top.clientapi.entity.Pong;
import org.top.core.RaftServerData;
import org.top.core.ServerStateEnum;
import org.top.rpc.handler.BaseMessageHandler;

/**
 * 接收客户端心跳
 *
 * @author lubeilin
 * @date 2020/11/27
 */
public class PingHandler extends BaseMessageHandler<Ping> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Ping msg) {
        if (RaftServerData.serverStateEnum != ServerStateEnum.LEADER) {
            ctx.writeAndFlush(new Pong(RaftServerData.leaderId));
        }
    }
}
