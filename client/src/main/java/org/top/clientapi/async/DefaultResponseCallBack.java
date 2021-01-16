package org.top.clientapi.async;

import org.top.clientapi.entity.OperationState;

import java.nio.charset.StandardCharsets;

/**
 * 默认响应处理
 *
 * @author lubeilin
 * @date 2020/12/19
 */
public abstract class DefaultResponseCallBack<V> implements ResponseCallback {
    protected ResultCallback<V> resultCallback;


    public DefaultResponseCallBack(ResultCallback<V> resultCallback) {
        this.resultCallback = resultCallback;
    }

    @Override
    public void callback(OperationState state, byte[] bytes) {
        if (state != OperationState.SUCCESS) {
            if (bytes == null) {
                System.out.println(state);
                resultCallback.fail(null);
                return;
            }
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
