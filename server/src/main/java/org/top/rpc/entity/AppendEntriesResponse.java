package org.top.rpc.entity;

import lombok.*;
import org.top.rpc.codec.BaseMessage;

/**
 * 日志复制响应数据
 *
 * @author lubeilin
 * @date 2020/11/17
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class AppendEntriesResponse extends BaseMessage {
    public static final long serialVersionUID = -521572154342549866L;
    private int term;
    private long index;
    private boolean success;
}
