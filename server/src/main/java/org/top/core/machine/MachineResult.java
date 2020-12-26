package org.top.core.machine;

import lombok.*;
import org.top.clientapi.entity.OperationState;

/**
 * @author lubeilin
 * @date 2020/12/26
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class MachineResult {
    private String id;
    private OperationState state;
    private byte[] data;
}
