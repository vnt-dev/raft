package org.top.rpc.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * 解码器
 *
 * @author lubeilin
 * @date 2020/11/26
 */
@Slf4j
public class MessageDecoder extends ByteToMessageDecoder {
    private static final int HAND_LENGTH = 4;

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        if (in.readableBytes() < HAND_LENGTH) {
            //不够字节的直接返回，等下一轮读
            return;
        }
        //标记读位置
        in.markReaderIndex();
        int length = in.readInt();
        if (in.readableBytes() < length) {
            in.resetReaderIndex();
            return;
        }
        byte[] data = new byte[length];
        in.readBytes(data);
        BaseMessage message = new ObjectData().deserialize(data).getMessage();
        out.add(message);
    }
}
