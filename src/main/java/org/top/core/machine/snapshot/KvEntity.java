package org.top.core.machine.snapshot;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * kv结构
 *
 * @author lubeilin
 * @date 2020/12/1
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class KvEntity {
    private byte[] key;
    private byte[] val;
}
