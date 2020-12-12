package org.top.rpc.entity;

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
    private byte[] id;
    private String option;
    private byte[] key;
    private byte[] val;
}
