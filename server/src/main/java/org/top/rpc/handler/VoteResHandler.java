package org.top.rpc.handler;

import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;
import org.top.core.FollowerConvert;
import org.top.core.VoteSemaphore;
import org.top.rpc.entity.VoteResponse;

/**
 * 处理投票响应
 *
 * @author lubeilin
 * @date 2020/11/14
 */
@Slf4j
public class VoteResHandler extends BaseMessageHandler<VoteResponse> {
    @Override
    public void channelActive(ChannelHandlerContext ctx0) throws Exception {
        super.channelActive(ctx0);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, VoteResponse msg) throws Exception {
        log.info("收到消息：{}", msg);
        FollowerConvert.convertFollower(msg.getTerm());
        if (msg.isVoteGranted()) {
            VoteSemaphore.vote(msg.getId());
        }
    }
}
