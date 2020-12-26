package org.top.clientapi.async;

import org.top.clientapi.entity.OperationState;

/**
 * @author lubeilin
 * @date 2020/12/19
 */
public interface ResponseCallback {
    /**
     * 接收响应
     *
     * @param state
     * @param bytes
     */
    void callback(OperationState state, byte[] bytes);
}
