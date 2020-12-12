package org.top.rpc.handler;

import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;
import org.top.core.*;
import org.top.exception.RaftException;
import org.top.rpc.entity.SnapshotRes;

/**
 * 快照rpc响应
 *
 * @author lubeilin
 * @date 2020/12/7
 */
@Slf4j
public class SnapshotResHandler extends BaseMessageHandler<SnapshotRes> {
    private AppendEntriesComponent component = new AppendEntriesComponent();

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, SnapshotRes msg) throws Exception {
        if (FollowerConvert.convertFollower(msg.getTerm())) {
            return;
        }
        if (RaftServerData.serverStateEnum != ServerStateEnum.LEADER) {
            throw new RaftException("状态不一致");
        }
        if (msg.isDone()) {
            log.info("最后一块");
            RaftServerData.leaderState.setNextIndexForNode(msg.getNode(), msg.getIndex());
            SnapshotExec.getInstance().unRead(msg.getNode());
        } else {
            ctx.writeAndFlush(component.snapshotReq(msg.getNext()));
        }
    }
}
