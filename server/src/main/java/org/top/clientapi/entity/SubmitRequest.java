package org.top.clientapi.entity;

import lombok.*;
import org.top.rpc.codec.BaseMessage;

/**
 * 客户端提交请求
 *
 * @author lubeilin
 * @date 2020/11/19
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class SubmitRequest extends BaseMessage {
    private String id;
    /**
     * 操作
     */
    private String option;
    /**
     * 过期时间
     */
    private Long expireTime;
    private byte[] key;
    private byte[] val;
}
