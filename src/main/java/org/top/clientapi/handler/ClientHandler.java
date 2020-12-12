package org.top.clientapi.handler;

import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;
import org.top.clientapi.ResultEntity;
import org.top.rpc.entity.SubmitResponse;
import org.top.rpc.handler.BaseMessageHandler;

/**
 * 客户端接收请求响应
 *
 * @author lubeilin
 * @date 2020/11/20
 */
@Slf4j
public class ClientHandler extends BaseMessageHandler<SubmitResponse> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, SubmitResponse msg) {
        ResultEntity.release(msg);
    }
}
