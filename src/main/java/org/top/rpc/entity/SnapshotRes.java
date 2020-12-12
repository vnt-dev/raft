package org.top.rpc.entity;

import lombok.*;
import org.top.rpc.Node;
import org.top.rpc.codec.BaseMessage;

/**
 * 快照响应数据
 *
 * @author lubeilin
 * @date 2020/12/3
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class SnapshotRes extends BaseMessage {
    /**
     * 当前任期号（currentTerm），便于领导人更新自己
     */
    private int term;

    private Node node;
    /**
     * 下一个key
     */
    private byte[] next;
    /**
     * 下一个索引值
     */
    private long index;
    /**
     * 是否结束快照传输
     */
    private boolean done;
}
