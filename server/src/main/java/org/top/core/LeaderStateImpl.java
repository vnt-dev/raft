package org.top.core;


import lombok.extern.slf4j.Slf4j;
import org.top.models.PersistentStateModel;
import org.top.rpc.NodeGroup;
import org.top.rpc.RpcClient;

/**
 * 领导人
 * 1. 一旦成为领导人：发送空的附加日志 RPC（心跳）给其他所有的服务器；在一定的空余时间之后不停的重复发送，以阻止跟随者超时（5.2 节）
 * 2. 如果接收到来自客户端的请求：附加条目到本地日志中，在条目被应用到状态机后响应客户端（5.3 节）
 * 3. 如果对于一个跟随者，最后日志条目的索引值大于等于 nextIndex，那么：发送从 nextIndex 开始的所有日志条目
 * - 如果成功：更新相应跟随者的 nextIndex 和 matchIndex
 * - 如果因为日志不一致而失败，减少 nextIndex 重试
 * 4. 如果存在一个满足N > commitIndex的 N，并且大多数的matchIndex[i] ≥ N成立，并且log[N].term == currentTerm成立，那么令 commitIndex 等于这个 N （5.3 和 5.4 节）（持久化数据）
 *
 * @author lubeilin
 * @date 2020/11/10
 */
@Slf4j
public class LeaderStateImpl extends AbstractServerStateTransformer {
    @Override
    public void execute() throws Exception {
        RaftServerData.lock.lock();
        try {
            if (RaftServerData.serverStateEnum == ServerStateEnum.CANDIDATE) {
                log.info("领导者,任期：{}", PersistentStateModel.getModel().getCurrentTerm());
                RaftServerData.serverStateEnum = ServerStateEnum.LEADER;
            } else {
                return;
            }
        } finally {
            RaftServerData.lock.unlock();
        }
        RaftServerData.leaderId = NodeGroup.MYSELF;

        RaftServerData.initLeader();
        AppendEntriesComponent.getInstance().broadcastLeader();
        while (RaftServerData.serverStateEnum == ServerStateEnum.LEADER) {
            //不断重连
            RpcClient.getRpcClient().reConn();
            try {
                if (ClientNum.getNum() >= NodeGroup.getNodeGroup().size()) {
                    //所有节点都在线，直接进入等待
                    ClientNum.closeAwait();
                } else {
                    Thread.sleep(50);
                }
            } catch (InterruptedException e) {
                log.info("领导者等待异常", e);
            }
        }
        RaftServerData.leaderDown();
    }

    @Override
    public ServerStateEnum nextState() {
        return ServerStateEnum.FOLLOWER;
    }
}
