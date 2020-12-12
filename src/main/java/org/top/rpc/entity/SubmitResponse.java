package org.top.rpc.entity;

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
     * 成功
     */
    public static final int SUCCESS = 1;
    /**
     * 失败
     */
    public static final int FAIL = 0;
    /**
     * 异常
     */
    public static final int ERROR = -1;
    /**
     * 转移请求
     */
    public static final int TURN = 2;
    private int code;
    /**
     * 主节点
     */
    private Node leaderId;
    /**
     * 消息id
     */
    private byte[] id;
    /**
     * 响应数据
     */
    private byte[] data;
}
