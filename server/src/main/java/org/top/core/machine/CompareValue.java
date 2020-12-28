package org.top.core.machine;

import lombok.*;

/**
 * 用于传递需要比较的值
 *
 * @author lubeilin
 * @date 2020/12/28
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class CompareValue {
    private byte[] compare;
    private byte[] update;
}
