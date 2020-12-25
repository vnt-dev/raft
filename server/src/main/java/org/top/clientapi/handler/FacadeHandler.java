package org.top.clientapi.handler;

import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;
import org.top.clientapi.entity.SubmitRequest;
import org.top.clientapi.entity.SubmitResponse;
import org.top.core.log.OperationFacade;
import org.top.core.log.OperationFacadeImpl;
import org.top.rpc.handler.BaseMessageHandler;

/**
 * 接收客户端请求
 *
 * @author lubeilin
 * @date 2020/11/20
 */
@Slf4j
public class FacadeHandler extends BaseMessageHandler<SubmitRequest> {
    private OperationFacade facade = new OperationFacadeImpl();

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, SubmitRequest msg) {
        SubmitResponse response = facade.submit(msg);
        if (response != null) {
            ctx.writeAndFlush(response);
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        facade.open(ctx.channel());
        ctx.fireChannelActive();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        facade.close();
        ctx.fireChannelInactive();
    }

}
