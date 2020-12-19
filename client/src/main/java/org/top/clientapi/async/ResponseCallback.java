package org.top.clientapi.async;

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
    void callback(int state, byte[] bytes);
}
