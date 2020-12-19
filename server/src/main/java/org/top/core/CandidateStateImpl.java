package org.top.core;


import lombok.extern.slf4j.Slf4j;
import org.rocksdb.Transaction;
import org.top.models.LogEntry;
import org.top.models.PersistentStateModel;
import org.top.rpc.NodeGroup;
import org.top.rpc.RpcClient;
import org.top.rpc.entity.VoteRequest;

/**
 * 候选人
 * 1. 在转变成候选人后就立即开始选举过程
 * - 自增当前的任期号（currentTerm）
 * - 给自己投票
 * - 重置选举超时计时器
 * - 发送请求投票的 RPC 给其他所有服务器
 * 2. 如果接收到大多数服务器的选票，那么就变成领导人
 * 3. 如果接收到来自新的领导人的附加日志 RPC，转变成跟随者
 * 4. 如果选举过程超时，再次发起一轮选举
 *
 * @author lubeilin
 * @date 2020/11/10
 */
@Slf4j
public class CandidateStateImpl extends AbstractServerStateTransformer {

    @Override
    public void execute() throws Exception {
        RaftServerData.serverStateEnum = ServerStateEnum.CANDIDATE;
        VoteRequest voteRequest = new VoteRequest();
        voteRequest.setCandidateId(NodeGroup.MYSELF);
        voteRequest.setBeforehand(false);
        voteRequest.setId(System.nanoTime());
        RpcClient rpcClient = RpcClient.getRpcClient();
        rpcClient.reConn();
        PersistentStateModel model = PersistentStateModel.getModel();
        LogEntry last = model.getLast();
        Transaction transaction = model.begin();
        RaftServerData.lock.lock();
        try {
            if (RaftServerData.serverStateEnum == ServerStateEnum.CANDIDATE) {
                voteRequest.setTerm(model.incrementAndGet(transaction));
                model.setVotedFor(NodeGroup.MYSELF, transaction);
                transaction.commit();
            }
        } catch (Exception e) {
            transaction.rollback();
            throw e;
        } finally {
            RaftServerData.lock.unlock();
        }
        voteRequest.setLastLogIndex(last.getIndex());
        voteRequest.setLastLogTerm(last.getTerm());
        if (RaftServerData.serverStateEnum == ServerStateEnum.CANDIDATE) {
            VoteSemaphore.reset(voteRequest.getId());
            VoteSemaphore.vote(voteRequest.getId());
            rpcClient.sendAll(e -> voteRequest);
        }
        executeNext();
    }

    @Override
    public ServerStateEnum nextState() {
        if (RaftServerData.serverStateEnum == ServerStateEnum.FOLLOWER) {
            return ServerStateEnum.FOLLOWER;
        }
        try {
            if (VoteSemaphore.tryAcquire()) {
                return ServerStateEnum.LEADER;
            }
        } catch (InterruptedException e) {
            log.info(e.getMessage(), e);
        }
        if (RaftServerData.serverStateEnum == ServerStateEnum.FOLLOWER) {
            return ServerStateEnum.FOLLOWER;
        }
        return ServerStateEnum.CANDIDATE;
    }

}
