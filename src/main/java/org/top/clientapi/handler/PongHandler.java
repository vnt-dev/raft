package org.top.clientapi.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleStateEvent;
import org.top.clientapi.ApiClient;
import org.top.clientapi.entity.Ping;
import org.top.clientapi.entity.Pong;
import org.top.rpc.handler.BaseMessageHandler;

/**
 * 客户端接收服务器心跳回应
 *
 * @author lubeilin
 * @date 2020/11/27
 */
public class PongHandler extends BaseMessageHandler<Pong> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Pong msg) {
        if (msg.getRedirect() != null) {
            ApiClient.getApiClient().setLeader(msg.getRedirect());
        }
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            ctx.writeAndFlush(new Ping());
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }

}
