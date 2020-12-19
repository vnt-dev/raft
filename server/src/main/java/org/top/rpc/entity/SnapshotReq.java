package org.top.rpc.entity;

import lombok.*;
import org.top.core.machine.snapshot.KvEntity;
import org.top.models.LogEntry;
import org.top.rpc.Node;
import org.top.rpc.codec.BaseMessage;

import java.util.List;

/**
 * 快照请求数据
 *
 * @author lubeilin
 * @date 2020/12/3
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class SnapshotReq extends BaseMessage {
    /**
     * 领导人的任期号
     */
    private int term;
    /**
     * 领导人的 Id，以便于跟随者重定向请求
     */
    private Node leaderId;
    /**
     * 快照中包含的最后日志条目的索引值
     */
    private long lastIncludedIndex;
    /**
     * 快照中包含的最后日志条目的任期号
     */
    private int lastIncludedTerm;
    /**
     * 快照分块的原始数据
     */
    private List<KvEntity> data;
    /**
     * 如果这是第一个分块则为 true
     */
    private boolean first;
    /**
     * 如果是最后一个分块，则包含往后的日志集合
     */
    private List<LogEntry> nextLogs;
}
