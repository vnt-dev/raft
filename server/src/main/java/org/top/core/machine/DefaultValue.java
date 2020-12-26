package org.top.core.machine;

import lombok.*;

/**
 * @author lubeilin
 * @date 2020/12/25
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class DefaultValue {
    /**
     * 值
     */
    private byte[] value;
    /**
     * 过期时间
     */
    private long expireTime = -1;
}
