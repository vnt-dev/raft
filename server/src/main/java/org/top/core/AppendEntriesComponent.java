package org.top.core;

import lombok.extern.slf4j.Slf4j;
import org.top.core.machine.KvStateMachineImpl;
import org.top.core.machine.OptionEnum;
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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * 生成各节点待接收的日志
 *
 * @author lubeilin
 * @date 2020/11/12
 */
@Slf4j
public class AppendEntriesComponent {
    private final int sendLogMaxNum = PropertiesUtil.getInt("send_log_max_num");
    private final int sendSnapshotMaxNum = PropertiesUtil.getInt("send_snapshot_max_num");
    private final int cacheLogNum = PropertiesUtil.getInt("cache_log_num");
    private final long heartbeatTime = PropertiesUtil.getLong("heartbeat") / 2;
    private SnapshotService snapshotService = new KvStateMachineImpl();
    private PersistentStateModel persistentState = PersistentStateModel.getModel();
    private SnapshotExec snapshotExec = SnapshotExec.getInstance();
    private static AppendEntriesComponent appendEntriesComponent = new AppendEntriesComponent();
    private LinkedBlockingQueue<Node> blockingQueue = new LinkedBlockingQueue<>();
    /**
     * 降低心跳频率
     */
    private Map<Node, Long> timeMap = new ConcurrentHashMap<>();

    private AppendEntriesComponent() {

    }

    public void pushHeartbeat(Node node) {
        if (!blockingQueue.contains(node)) {
            blockingQueue.offer(node);
        }
    }

    public void startHeartbeatLoop() {
        new Thread(() -> {
            for (; ; ) {
                try {
                    Node node1 = blockingQueue.take();
                    appendEntriesOne(node1);
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            }
        }, "heartbeat-thread").start();
    }

    public static AppendEntriesComponent getInstance() {
        return appendEntriesComponent;
    }

    public void broadcastAppendEntries() {
        if (cacheLogNum >= 0) {
            RpcClient.getRpcClient().sendAll(node -> appendRequest(node, false), node -> snapshotExec.unRead(node));
        }
    }

    private void appendEntriesOne(Node node) {
        RpcClient.getRpcClient().sendOne(node, n -> appendRequest(n, true), n -> snapshotExec.unRead(n));
    }


    public void broadcastLeader() throws Exception {
        LogEntry logEntry = new LogEntry();
        logEntry.setOption(OptionEnum.UP.getCode());
        logEntry.setId("");
        persistentState.pushLast(logEntry);
    }

    public BaseMessage appendRequest(Node node, boolean heartbeat) throws Exception {
        if (heartbeat) {
            if (System.currentTimeMillis() < timeMap.getOrDefault(node, 0L) + heartbeatTime) {
                return null;
            }
        }
        if (snapshotExec.isRead(node)) {
            return null;
        }

        AppendLog appendLog = getNextEntries(node);
        if (appendLog == null) {
            //正在读取快照的不重新发
            if (snapshotExec.isRead(node)) {
                return null;
            } else {
                log.info("日志落后，发送快照：{}", node);
                snapshotExec.read(node);
            }
            snapshotExec.getReadLock().lock();
            try {
                RaftServerData.leaderState.setNextIndexForNode(node, snapshotService.snapshotLastIndex() + 1);
                return snapshotReq(null);
            } finally {
                snapshotExec.getReadLock().unlock();
            }
        }
        if (!heartbeat) {
            if (appendLog.list == null || appendLog.list.size() <= cacheLogNum) {
                return null;
            }
            timeMap.put(node, System.currentTimeMillis());
        }
        AppendEntriesRequest request = new AppendEntriesRequest();
        int currentTerm = persistentState.getCurrentTerm();
        ServerStateModel serverState = RaftServerData.serverState;
        request.setTerm(currentTerm);
        request.setLeaderId(NodeGroup.MYSELF);
        //紧邻新日志条目之前的那个日志条目的索引
        request.setPreLogIndex(appendLog.pro.getIndex());
        //紧邻新日志条目之前的那个日志条目的任期
        request.setPreLogTerm(appendLog.pro.getTerm());
        if (appendLog.list != null) {
            //这里做个优化，发送出去默认就增加（假定从节点一定接收成功）
            RaftServerData.leaderState.setNextIndexForNode(node, appendLog.pro.getIndex() + 1, appendLog.list.getLast().getIndex() + 1);
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
        nextIndex = RaftServerData.leaderState.getNext(node);
        if (nextIndex > persistentState.getLastIndex()) {
            appendLog.pro = persistentState.getLast();
            return appendLog;
        }
        if (nextIndex <= snapshotService.snapshotLastIndex()) {
            //开始读取快照
            return null;
        }
        appendLog.pro = persistentState.getLog(nextIndex - 1);
        //一次最多发送sendLogMaxNum条日志
        appendLog.list = persistentState.getLogs(nextIndex, nextIndex + sendLogMaxNum);
        return appendLog;
    }

    static class AppendLog {
        LogEntry pro;
        LinkedList<LogEntry> list;
    }


}
