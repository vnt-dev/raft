package org.top.clientapi.entity;

import lombok.*;
import org.top.rpc.Node;
import org.top.rpc.codec.BaseMessage;

/**
 * 客户端提交响应
 *
 * @author lubeilin
 * @date 2020/11/19
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class SubmitResponse extends BaseMessage {
    public static final long serialVersionUID = -521572154342549866L;
    /**
     * 执行状态
     */
    private OperationState state;
    /**
     * 主节点
     */
    private Node leaderId;
    /**
     * 消息id
     */
    private String id;
    /**
     * 响应数据
     */
    private byte[] data;
}
