package org.top.rpc.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.extern.slf4j.Slf4j;
import org.top.core.*;
import org.top.core.machine.StateMachineHandler;
import org.top.core.machine.StateMachineHandlerImpl;
import org.top.models.PersistentStateModel;
import org.top.rpc.Node;
import org.top.rpc.NodeGroup;
import org.top.rpc.codec.BaseMessage;
import org.top.rpc.entity.AppendEntriesResponse;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 日志附加响应事件处理
 *
 * @author lubeilin
 * @date 2020/11/14
 */
@Slf4j
public class AppendResHandler extends BaseMessageHandler<AppendEntriesResponse> {
    protected Node followerNode;
    private StateMachineHandler stateMachineHandler = new StateMachineHandlerImpl();
    private AppendEntriesComponent appendEntriesComponent = new AppendEntriesComponent();

    @Override
    public void channelActive(ChannelHandlerContext ctx0) throws Exception {
        super.channelActive(ctx0);
        InetSocketAddress socketAddress = (InetSocketAddress) ctx0.channel().remoteAddress();
        followerNode = NodeGroup.getNodeGroup().getNode(new Node(socketAddress.getAddress().getHostAddress(), socketAddress.getPort()));
        log.info("建立连接：{}", followerNode);
        if (followerNode != null) {
            ClientNum.add();
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        if (followerNode != null) {
            SnapshotExec.getInstance().unRead(followerNode);
            ClientNum.closeNotifyAll();
        }
        log.info("断开连接：{}", followerNode);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, AppendEntriesResponse msg) throws Exception {
        if (FollowerConvert.convertFollower(msg.getTerm())) {
            return;
        }
        if (msg.isSuccess()) {
            Map<Node, Long> matchIndex = RaftServerData.leaderState.getMatchIndex();
            matchIndex.put(followerNode, msg.getIndex());
        } else {
            //noinspection SynchronizeOnNonFinalField
            synchronized (followerNode) {
                //这里做一个优化，下一个索引值由从节点传过来，返回值msg.getIndex() 为目标的最后一条日志
                //由于默认就看做成功的，所以只有失败才要设置下一个索引值
                RaftServerData.leaderState.setNextIndexForNode(followerNode, msg.getIndex() + 1);
            }
        }
        //如果存在一个满足N > commitIndex的 N，并且大多数的matchIndex[i] ≥ N成立，
        // 并且log[N].term == currentTerm成立，那么令 commitIndex 等于这个 N （5.3 和 5.4 节）
        long indexMaxN = getMaxN();
        if (indexMaxN > RaftServerData.serverState.getCommitIndex()) {
            RaftServerData.serverState.setCommitIndex(indexMaxN);
            //应用到状态机
            if (indexMaxN > RaftServerData.serverState.getLastApplied()) {
                stateMachineHandler.start();
            }
        }
    }

    /**
     * @return 如果存在一个满足N > commitIndex的 N，并且大多数的matchIndex[i] ≥ N成立，并且log[N].term == currentTerm成立，那么令 commitIndex 等于这个 N
     */
    private long getMaxN() throws Exception {
        PersistentStateModel model = PersistentStateModel.getModel();
        long indexMaxN = model.getLastIndex();
        Map<Node, Long> matchIndex = RaftServerData.leaderState.getMatchIndex();
        AtomicInteger matchServerCount = new AtomicInteger(0);
        int currentTerm = model.getCurrentTerm();
        //找到N
        for (; indexMaxN > RaftServerData.serverState.getCommitIndex(); indexMaxN--) {
            //当前节点已存在，所以初始值为1
            matchServerCount.set(1);
            long indN = indexMaxN;
            NodeGroup nodeGroup = NodeGroup.getNodeGroup();
            nodeGroup.forEach(remoteNode -> {
                if (matchIndex.get(remoteNode) >= indN) {
                    matchServerCount.incrementAndGet();
                }
            });
            //2.2 判断是否一半以上成立, 并且log[N].term == currentTerm成立
            //为什么需要log[N].term == currentTerm，论证5.4.2（提交之前任期内的日志条目） 中提到的，旧任期的日志条目即使是被复制到大多数机器上了仍然有
            // 可能被覆盖，因此只有等当前任期的日志被提交了才能一起提交前面任期的日志
            if (matchServerCount.get() >= NodeGroup.getNodeGroup().majority() &&
                    model.getLog(indexMaxN).getTerm() == currentTerm) {
                break;
            }
        }
        return indexMaxN;
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            if (RaftServerData.serverStateEnum == ServerStateEnum.LEADER) {
                BaseMessage baseMessage = appendEntriesComponent.appendRequest(followerNode, true);
                if (baseMessage != null) {
                    ctx.writeAndFlush(baseMessage);
                }
            }
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }
}
