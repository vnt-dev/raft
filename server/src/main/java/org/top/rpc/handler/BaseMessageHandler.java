package org.top.rpc.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.internal.TypeParameterMatcher;
import lombok.extern.slf4j.Slf4j;
import org.top.rpc.codec.BaseMessage;

/**
 * @author lubeilin
 * @date 2020/11/14
 */
@Slf4j
public abstract class BaseMessageHandler<T extends BaseMessage> extends ChannelInboundHandlerAdapter {
    private final TypeParameterMatcher matcher;

    protected BaseMessageHandler() {
        matcher = TypeParameterMatcher.find(this, BaseMessageHandler.class, "T");
    }

    public boolean acceptInboundMessage(Object msg) {
        return matcher.match(msg);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        try {
            if (acceptInboundMessage(msg)) {
                @SuppressWarnings("unchecked")
                T tMsg = (T) msg;
                channelRead0(ctx, tMsg);
            } else {
                ctx.fireChannelRead(msg);
            }
        } finally {
            ReferenceCountUtil.release(msg);
        }
    }

    /**
     * 消息处理
     *
     * @param ctx 连接
     * @param msg 消息
     * @throws Exception 异常
     */
    protected abstract void channelRead0(ChannelHandlerContext ctx, T msg) throws Exception;
}
