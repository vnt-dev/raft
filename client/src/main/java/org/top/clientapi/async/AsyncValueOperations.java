package org.top.clientapi.async;

/**
 * @author lubeilin
 * @date 2020/12/19
 */
public interface AsyncValueOperations<V> {
    /**
     * 获取数据
     *
     * @param key
     * @param resultCallback
     */
    void get(String key, ResultCallback<V> resultCallback);

    /**
     * 删除
     *
     * @param key
     * @param resultCallback
     */
    void delete(String key, ResultCallback<Boolean> resultCallback);
}
