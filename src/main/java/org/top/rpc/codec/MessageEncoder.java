package org.top.rpc.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import lombok.extern.slf4j.Slf4j;

/**
 * 编码器
 *
 * @author lubeilin
 * @date 2020/11/26
 */
@Slf4j
public class MessageEncoder extends MessageToByteEncoder<BaseMessage> {
    @Override
    protected void encode(ChannelHandlerContext ctx, BaseMessage msg, ByteBuf out) throws Exception {
        byte[] bytes = new ObjectData(msg).serialize();
        out.writeInt(bytes.length);
        out.writeBytes(bytes);
    }
}
