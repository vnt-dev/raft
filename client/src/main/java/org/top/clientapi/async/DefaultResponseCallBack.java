package org.top.clientapi.async;

import java.nio.charset.StandardCharsets;

/**
 * @author lubeilin
 * @date 2020/12/19
 */
public abstract class DefaultResponseCallBack implements ResponseCallback {
    private ResultCallback<?> resultCallback;


    public DefaultResponseCallBack(ResultCallback<?> resultCallback) {
        this.resultCallback = resultCallback;
    }

    @Override
    public void callback(int state, byte[] bytes) {
        if (state != 1) {
            resultCallback.fail(new String(bytes, StandardCharsets.UTF_8));
        } else {
            this.success(bytes);
        }
    }

    /**
     * 返回成功
     *
     * @param bytes
     */
    public abstract void success(byte[] bytes);
}
