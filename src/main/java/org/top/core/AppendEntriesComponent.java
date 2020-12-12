package org.top.core;

import lombok.extern.slf4j.Slf4j;
import org.top.core.machine.KvStateMachineImpl;
import org.top.core.machine.SnapshotService;
import org.top.exception.RaftException;
import org.top.models.LogEntry;
import org.top.models.PersistentStateModel;
import org.top.models.ServerStateModel;
import org.top.rpc.Node;
import org.top.rpc.NodeGroup;
import org.top.rpc.RpcClient;
import org.top.rpc.codec.BaseMessage;
import org.top.rpc.entity.AppendEntriesRequest;
import org.top.rpc.entity.SnapshotReq;
import org.top.rpc.utils.PropertiesUtil;

import java.util.LinkedList;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

/**
 * 生成各节点待接收的日志
 *
 * @author lubeilin
 * @date 2020/11/12
 */
@Slf4j
public class AppendEntriesComponent {
    private SnapshotService snapshotService = new KvStateMachineImpl();
    private PersistentStateModel persistentState = PersistentStateModel.getModel();
    private SnapshotExec snapshotExec = SnapshotExec.getInstance();
    private final int sendLogMaxNum = PropertiesUtil.getInt("send_log_max_num");
    private final int sendSnapshotMaxNum = PropertiesUtil.getInt("send_snapshot_max_num");
    /**
     * 缓存待发送的日志
     */
    private static ConcurrentLinkedQueue<LogEntry> cacheLogs = new ConcurrentLinkedQueue<>();

    public void broadcastAppendEntries() {
        RpcClient.getRpcClient().sendAll(node -> appendRequest(node, false), node -> snapshotExec.unRead(node));
    }

    public void broadcastAppendEntriesOrHeart() {
        RpcClient.getRpcClient().sendAll(node -> appendRequest(node, true), node -> snapshotExec.unRead(node));
    }

    public BaseMessage appendRequest(Node node, boolean heartbeat) throws Exception {
        if (snapshotExec.isRead(node)) {
            return null;
        }

        AppendLog appendLog = getNextEntries(node);
        if (appendLog == null) {
            //正在读取快照的不重新发
            //noinspection SynchronizationOnLocalVariableOrMethodParameter
            synchronized (node) {
                if (snapshotExec.isRead(node)) {
                    return null;
                } else {
                    log.info("日志落后，发送快照：{}", node);
                    snapshotExec.read(node);
                }
            }
            snapshotExec.getReadLock().lock();
            try {
                RaftServerData.leaderState.setNextIndexForNode(node, snapshotService.snapshotLastIndex() + 1);
                return snapshotReq(null);
            } finally {
                snapshotExec.getReadLock().unlock();
            }
        }
        if (!heartbeat && appendLog.list == null) {
            return null;
        }
        AppendEntriesRequest request = new AppendEntriesRequest();
        int currentTerm = persistentState.getCurrentTerm();
        ServerStateModel serverState = RaftServerData.serverState;
        request.setTerm(currentTerm);
        request.setLeaderId(NodeGroup.MYSELF);

        if (appendLog.pro != null) {
            //紧邻新日志条目之前的那个日志条目的索引
            request.setPreLogIndex(appendLog.pro.getIndex());
            //紧邻新日志条目之前的那个日志条目的任期
            request.setPreLogTerm(appendLog.pro.getTerm());
        }
        //需要被保存的日志条目（被当做心跳使用时 则日志条目内容为空；为了提高效率可能一次性发送多个）
        request.setEntries(appendLog.list);
        //领导者的已知已提交的最高的日志条目的索引
        request.setLeaderCommit(serverState.getCommitIndex());
        if (RaftServerData.serverStateEnum != ServerStateEnum.LEADER) {
            throw new RaftException("状态不一致");
        }
        return request;
    }

    public SnapshotReq snapshotReq(byte[] lastKey) throws Exception {
        SnapshotReq snapshotReq = new SnapshotReq();
        snapshotReq.setFirst(lastKey == null);
        snapshotReq.setLastIncludedIndex(snapshotService.snapshotLastIndex());
        snapshotReq.setLastIncludedTerm(snapshotService.snapshotLastTerm());
        snapshotReq.setLeaderId(NodeGroup.MYSELF);
        snapshotReq.setTerm(persistentState.getCurrentTerm());
        snapshotReq.setData(snapshotService.getSnapshotData(lastKey, sendSnapshotMaxNum));
        if (snapshotReq.getData().size() < sendSnapshotMaxNum) {
            long nextIndex = snapshotReq.getLastIncludedIndex() + 1;
            snapshotReq.setNextLogs(persistentState.getLogs(nextIndex, nextIndex + sendSnapshotMaxNum - snapshotReq.getData().size()));
        }
        return snapshotReq;
    }

    /**
     * 获取下一批 日志条目
     */
    private AppendLog getNextEntries(Node node) throws Exception {
        AppendLog appendLog = new AppendLog();
        long nextIndex;
        //noinspection SynchronizationOnLocalVariableOrMethodParameter
        synchronized (node) {
            nextIndex = RaftServerData.leaderState.getNext(node);
            if (nextIndex > persistentState.getLastIndex()) {
                appendLog.pro = persistentState.getLast();
                return appendLog;
            }
            if (nextIndex <= snapshotService.snapshotLastIndex()) {
                //开始读取快照
                return null;
            }
            //一次最多发送500条日志
            appendLog.list = persistentState.getLogs(nextIndex, nextIndex + sendLogMaxNum);
            //这里做个优化，发送出去默认就增加（假定从节点一定接收成功）
            RaftServerData.leaderState.setNextIndexForNode(node, appendLog.list.getLast().getIndex() + 1);
            log.info("node:{},log:{}   ", node, appendLog.list.stream().map(LogEntry::getIndex).collect(Collectors.toList()));
        }
        appendLog.pro = persistentState.getLog(nextIndex - 1);

        return appendLog;
    }

    static class AppendLog {
        LogEntry pro;
        LinkedList<LogEntry> list;
    }


}
