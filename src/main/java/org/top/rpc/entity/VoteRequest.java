package org.top.rpc.entity;

import lombok.*;
import org.top.rpc.Node;
import org.top.rpc.codec.BaseMessage;

/**
 * 投票请求体
 *
 * @author lubeilin
 * @date 2020/11/10
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class VoteRequest extends BaseMessage {
    public static final long serialVersionUID = -521572154342549866L;
    private long id;
    /**
     * 是否为预投票
     */
    private boolean beforehand;
    /**
     * 候选人的任期号
     */
    private int term;
    /**
     * 请求选票的候选人
     */
    private Node candidateId;

    /**
     * 候选人的最后日志条目的索引值
     */
    private long lastLogIndex;

    /**
     * 候选人最后日志条目的任期号
     */
    private long lastLogTerm;

}
