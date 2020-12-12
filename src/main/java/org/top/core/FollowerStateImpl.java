package org.top.core;

import lombok.extern.slf4j.Slf4j;
import org.top.models.LogEntry;
import org.top.models.PersistentStateModel;
import org.top.rpc.NodeGroup;
import org.top.rpc.RpcClient;
import org.top.rpc.entity.VoteRequest;
import org.top.rpc.utils.PropertiesUtil;

import java.util.concurrent.ThreadLocalRandom;


/**
 * 跟随者
 * 1. 响应来自候选人和领导者的请求
 * 2. 如果在超过选举超时时间的情况之前没有收到当前领导人（即该领导人的任期需与这个跟随者的当前任期相同）的心跳/附加日志，或者是给某个候选人投了票，就自己变成候选人（心跳超时就进入下个状态）
 *
 * @author lubeilin
 * @date 2020/11/10
 */
@Slf4j
public class FollowerStateImpl extends AbstractServerStateTransformer {

    @Override
    public void execute() throws Exception {
        log.info("跟随者");
        RpcClient.getRpcClient().close();
        long start = PropertiesUtil.getLong("election_out_time_start");
        long end = PropertiesUtil.getLong("election_out_time_end");
        RaftServerData.serverStateEnum = ServerStateEnum.FOLLOWER;
        for (; ; ) {
            long time = ThreadLocalRandom.current().nextLong(start, end);
            try {
                Thread.sleep(time);
            } catch (InterruptedException e) {
                log.info("跟随者等待异常", e);
            }
            if (!RaftServerData.isBusy && System.currentTimeMillis() - RaftServerData.heartbeatTime > time) {
                log.info("主节点无心跳，发起预投票");
                try {
                    if (votePro(time)) {
                        break;
                    } else {
                        log.info("预投票失败");
                    }
                } catch (Exception e) {
                    log.info("跟随者预投票", e);
                }
            }
        }
        executeNext();
    }

    private boolean votePro(long time) throws Exception {
        VoteRequest voteRequest = new VoteRequest();
        voteRequest.setId(System.nanoTime());
        PersistentStateModel model = PersistentStateModel.getModel();
        int term = model.getCurrentTerm();
        LogEntry last = model.getLast();
        voteRequest.setTerm(term + 1);
        if (System.currentTimeMillis() - RaftServerData.heartbeatTime < time || RaftServerData.isBusy) {
            return false;
        }
        voteRequest.setCandidateId(NodeGroup.MYSELF);
        voteRequest.setBeforehand(true);
        voteRequest.setLastLogIndex(last.getIndex());
        voteRequest.setLastLogTerm(last.getTerm());
        VoteSemaphore.reset(voteRequest.getId());
        VoteSemaphore.vote(voteRequest.getId());
        log.info("{}", voteRequest);
        RpcClient rpcClient = RpcClient.getRpcClient();
        rpcClient.reConn();
        rpcClient.sendAll(e -> voteRequest);
        boolean rs = VoteSemaphore.tryAcquire();
        if (!rs) {
            RpcClient.getRpcClient().close();
        }
        return rs;
    }

    @Override
    public ServerStateEnum nextState() {
        return ServerStateEnum.CANDIDATE;
    }

}
