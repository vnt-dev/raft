package org.top.rpc.handler;

import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;
import org.top.core.FollowerConvert;
import org.top.core.RaftServerData;
import org.top.core.machine.KvStateMachineImpl;
import org.top.core.machine.SnapshotService;
import org.top.core.machine.StateMachineHandler;
import org.top.core.machine.StateMachineHandlerImpl;
import org.top.models.LogEntry;
import org.top.models.PersistentStateModel;
import org.top.models.ServerStateModel;
import org.top.rpc.entity.AppendEntriesRequest;
import org.top.rpc.entity.AppendEntriesResponse;

/**
 * 日志附加事件处理
 * 接收者的实现：
 * <p>
 * 1. 如果领导者的任期 小于 接收者的当前任期 返回假
 * 2. 如果接收者日志中没有包含这样一个条目（即prevLogIndex和prevLogTerm没有匹配上） 返回假
 * 3. 如果一个已经存在的条目和新条目（译者注：即刚刚接收到的日志条目）发生了冲突（因为索引相同，任期不同），那么就删除这个已经存在的条目以及它之后的所有条目
 * 4. 追加日志中尚未存在的任何新条目
 * 5. 如果领导者的已知已经提交的最高的日志条目的索引 大于 接收者的已知已经提交的最高的日志条目的索引 则把 接收者的已知已经提交的最高的日志条目的索引 重置为 领导者的已知已经提交的最高的日志条目的索引 或者是 上一个新条目的索引 取两者的最小值（提交日志）
 *
 * @author lubeilin
 * @date 2020/11/14
 */
@Slf4j
public class AppendReqHandler extends BaseMessageHandler<AppendEntriesRequest> {
    private StateMachineHandler stateMachineHandler = new StateMachineHandlerImpl();
    private SnapshotService snapshotService = new KvStateMachineImpl();

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, AppendEntriesRequest msg) throws Exception {
        //执行代码期间不会进入候选者状态
        RaftServerData.isBusy = true;
        RaftServerData.lock.lock();
        if (msg.getEntries() != null) {
            log.info("pre index:{},log:{}", msg.getPreLogIndex(), msg.getEntries().size());
        }
        try {
            FollowerConvert.convertFollower(msg.getTerm());
            PersistentStateModel stateModel = PersistentStateModel.getModel();
            ServerStateModel serverState = RaftServerData.serverState;
            int term = msg.getTerm();
            int currentTerm = stateModel.getCurrentTerm();
            LogEntry last = stateModel.getLast();
            if (term < currentTerm) {
                ctx.writeAndFlush(new AppendEntriesResponse(currentTerm, last.getIndex(), false));
                log.info("任期过低:{},当前任期：{}", term, currentTerm);
                return;
            }
            RaftServerData.heartbeatTime = System.currentTimeMillis();
            RaftServerData.leaderId = msg.getLeaderId();
            //如果接收者日志中没有包含这样一个条目 即该条目的任期在preLogIndex上能和prevLogTerm匹配上 则返回假，并且附带上需要接受的下一条日志
            if (msg.getPreLogIndex() > last.getIndex()) {
                log.info("日志落后，getPreLogIndex:{},getIndex:{}", msg.getPreLogIndex(), last.getIndex());
                ctx.writeAndFlush(new AppendEntriesResponse(currentTerm, last.getIndex(), false));
                return;
            } else if (snapshotService.snapshotLastIndex() < msg.getPreLogIndex()) {
                //进了快照的数据肯定没错，所以不需要管
                LogEntry preLog = stateModel.getLog(msg.getPreLogIndex());
                if (preLog.getTerm() != msg.getPreLogTerm()) {
                    log.info("日志不匹配，PreLogIndex:{},PreLogTerm:{},localTerm{}", msg.getPreLogIndex(), msg.getPreLogTerm(), preLog.getTerm());
                    ctx.writeAndFlush(new AppendEntriesResponse(currentTerm, msg.getPreLogIndex(), false));
                    return;
                }
            }
            //如果一个已经存在的条目和新条目（译者注：即刚刚接收到的日志条目）发生了冲突（因为索引相同，任期不同），那么就删除这个已经存在的条目以及它之后的所有条目
            //追加日志中尚未存在的任何新条目
            if (msg.getEntries() != null && !msg.getEntries().isEmpty()) {
                //已提交的日志不判断
                stateModel.addLogs(msg.getEntries(), serverState.getCommitIndex());
            }
            last = stateModel.getLast();
            if (msg.getLeaderCommit() > serverState.getCommitIndex()) {
                serverState.setCommitIndex(Math.min(msg.getLeaderCommit(), last.getIndex()));
                if (serverState.getCommitIndex() > serverState.getLastApplied()) {
                    stateMachineHandler.start();
                }
            }
            ctx.writeAndFlush(new AppendEntriesResponse(currentTerm, last.getIndex(), true));
        } finally {
            RaftServerData.lock.unlock();
            RaftServerData.isBusy = false;
        }
    }

}
