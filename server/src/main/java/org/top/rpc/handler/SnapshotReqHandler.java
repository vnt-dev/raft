package org.top.rpc.handler;

import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;
import org.top.core.FollowerConvert;
import org.top.core.RaftServerData;
import org.top.core.SnapshotExec;
import org.top.core.machine.KvStateMachineImpl;
import org.top.core.machine.StateMachineHandlerImpl;
import org.top.models.PersistentStateModel;
import org.top.rpc.NodeGroup;
import org.top.rpc.entity.SnapshotReq;
import org.top.rpc.entity.SnapshotRes;

/**
 * 接收者实现：
 * <p>
 * 如果term < currentTerm就立即回复
 * 如果是第一个分块（offset 为 0）就创建一个新的快照
 * 在指定偏移量写入数据
 * 如果 done 是 false，则继续等待更多的数据
 * 保存快照文件，丢弃具有较小索引的任何现有或部分快照
 * 如果现存的日志条目与快照中最后包含的日志条目具有相同的索引值和任期号，则保留其后的日志条目并进行回复 ?
 * 丢弃整个日志
 * 使用快照重置状态机（并加载快照的集群配置）
 *
 * @author lubeilin
 * @date 2020/12/3
 */
@Slf4j
public class SnapshotReqHandler extends BaseMessageHandler<SnapshotReq> {
    private static KvStateMachineImpl kvStateMachine = new KvStateMachineImpl();

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, SnapshotReq msg) throws Exception {
        log.info("{}", msg.getLastIncludedIndex());
        RaftServerData.isBusy = true;
        RaftServerData.lock.lock();
        try {
            FollowerConvert.convertFollower(msg.getTerm());
            PersistentStateModel stateModel = PersistentStateModel.getModel();
            int term = msg.getTerm();
            int currentTerm = stateModel.getCurrentTerm();
            if (term < currentTerm) {
                ctx.writeAndFlush(new SnapshotRes(currentTerm, NodeGroup.MYSELF, null, 0, false));
                log.info("任期过低:{},当前任期：{}", term, currentTerm);
                return;
            }
            RaftServerData.heartbeatTime = System.currentTimeMillis();
            RaftServerData.leaderId = msg.getLeaderId();
            if (msg.isFirst()) {
                RaftServerData.serverState.setCommitIndex(0);
                RaftServerData.serverState.setLastApplied(0);

            }
            SnapshotExec.getInstance().getWriteLock().lock();
            try {
                if (msg.isFirst()) {
                    //等待状态机循环停止
                    StateMachineHandlerImpl.getInstance().awaitPause();
                    //重置状态机和快照
                    kvStateMachine.reset();
                    //重置日志
                    stateModel.reset();
                    log.info("重置数据结束");
                }
                if (msg.getData() != null) {
                    kvStateMachine.execute(msg.getData(), msg.getLastIncludedTerm(), msg.getLastIncludedIndex());
                }
                if (msg.getNextLogs() != null) {
                    log.info("最后一块");
                    //接收完之后设置已提交的日志到快照的索引处
                    RaftServerData.serverState.setLastApplied(msg.getLastIncludedIndex());
                    RaftServerData.serverState.setCommitIndex(msg.getLastIncludedIndex());
                    //添加下一批日志
                    stateModel.addLogAll(msg.getNextLogs());
                    ctx.writeAndFlush(new SnapshotRes(currentTerm, NodeGroup.MYSELF, null, msg.getNextLogs().get(msg.getNextLogs().size() - 1).getIndex() + 1, true));
                } else {
                    ctx.writeAndFlush(new SnapshotRes(currentTerm, NodeGroup.MYSELF, msg.getData().get(msg.getData().size() - 1).getKey(), 0, false));
                }
            } finally {
                SnapshotExec.getInstance().getWriteLock().unlock();
            }

        } catch (Exception e) {
            log.info("错误：{}", msg);
            throw e;
        } finally {
            RaftServerData.lock.unlock();
            RaftServerData.isBusy = false;
        }

    }
}
