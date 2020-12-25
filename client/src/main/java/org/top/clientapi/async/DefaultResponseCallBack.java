package org.top.clientapi.async;

import java.nio.charset.StandardCharsets;

/**
 * @author lubeilin
 * @date 2020/12/19
 */
public abstract class DefaultResponseCallBack<V> implements ResponseCallback {
    protected ResultCallback<V> resultCallback;


    public DefaultResponseCallBack(ResultCallback<V> resultCallback) {
        this.resultCallback = resultCallback;
    }

    @Override
    public void callback(int state, byte[] bytes) {
        if (state != 1) {
            if(bytes==null){
                System.out.println(state);
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
