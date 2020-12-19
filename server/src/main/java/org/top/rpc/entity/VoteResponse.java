package org.top.rpc.entity;

import lombok.*;
import org.top.rpc.codec.BaseMessage;

/**
 * 投票响应数据
 *
 * @author lubeilin
 * @date 2020/11/10
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class VoteResponse extends BaseMessage {
    public static final long serialVersionUID = -521572154342549866L;
    private long id;
    /**
     * 当前任期号，以便于候选人去更新自己的任期号
     */
    private int term;
    /**
     * 候选人赢得了此张选票时为真
     */
    private boolean voteGranted;
}
