package org.top.rpc.handler;

import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;
import org.rocksdb.Transaction;
import org.top.core.FollowerConvert;
import org.top.core.RaftServerData;
import org.top.core.ServerStateEnum;
import org.top.models.LogEntry;
import org.top.models.PersistentStateModel;
import org.top.rpc.Node;
import org.top.rpc.entity.VoteRequest;
import org.top.rpc.entity.VoteResponse;
import org.top.rpc.utils.PropertiesUtil;

/**
 * 处理投票请求
 * 接收者实现
 * 1. 如果term < currentTerm返回 false
 * 2. 如果 votedFor 为空或者为 candidateId，并且候选人的日志至少和自己一样新，那么就投票给他
 *
 * @author lubeilin
 * @date 2020/11/14
 */
@Slf4j
public class VoteReqHandler extends BaseMessageHandler<VoteRequest> {
    private long hTime = PropertiesUtil.getLong("election_out_time");

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, VoteRequest msg) throws Exception {
        log.info("收到消息：{}", msg);
        if (msg.isBeforehand()) {
            //超过这个时间间隔没收到心跳才响应投票
            if (System.currentTimeMillis() - RaftServerData.heartbeatTime <= hTime) {
                log.info("未超过响应投票时间：{}", msg);
                return;
            }
        } else {
            FollowerConvert.convertFollower(msg.getTerm());
        }
        if (RaftServerData.serverStateEnum != ServerStateEnum.FOLLOWER) {
            log.info("不是从节点不响应：{}", msg);
            return;
        }
        PersistentStateModel model = PersistentStateModel.getModel();
        int currentTerm = model.getCurrentTerm();
        if (currentTerm > msg.getTerm()) {
            ctx.writeAndFlush(new VoteResponse(msg.getId(), currentTerm, false));
        } else if (notVoteOther(msg, model.getVotedFor()) && upToDate(msg, model.getLast())) {
            RaftServerData.heartbeatTime = System.currentTimeMillis();
            if (msg.isBeforehand()) {
                Transaction transaction = model.begin();
                try {
                    model.setVotedFor(msg.getCandidateId(), transaction);
                    transaction.commit();
                } catch (Exception e) {
                    transaction.rollback();
                    throw e;
                }
            }
            ctx.writeAndFlush(new VoteResponse(msg.getId(), currentTerm, true));
        } else {
            ctx.writeAndFlush(new VoteResponse(msg.getId(), currentTerm, false));
        }
    }

    private boolean notVoteOther(VoteRequest voteRequest,
                                 Node votedFor) {
        return votedFor == null
                || votedFor.equals(voteRequest.getCandidateId());
    }

    private boolean upToDate(VoteRequest voteRequest,
                             LogEntry logEntry) {
        return logEntry.getTerm() <= voteRequest.getLastLogTerm() &&
                logEntry.getIndex() <= voteRequest.getLastLogIndex();
    }
}
