package org.top.clientapi.async;

/**
 * 异步执行命令
 *
 * @author lubeilin
 * @date 2020/12/19
 */
public interface AsyncValueOperations<V> {
    /**
     * 获取数据
     *
     * @param key            key
     * @param resultCallback 结果回调
     */
    void get(String key, ResultCallback<V> resultCallback);

    /**
     * 删除
     *
     * @param key            key
     * @param resultCallback 结果回调
     */
    void delete(String key, ResultCallback<Boolean> resultCallback);

    /**
     * 删除
     *
     * @param key key
     */
    void delete(String key);

    /**
     * 修改
     *
     * @param key key
     * @param v   value
     */
    void set(String key, V v);

    /**
     * 修改
     *
     * @param key            key
     * @param v              value
     * @param resultCallback 结果回调
     */
    void set(String key, V v, ResultCallback<Boolean> resultCallback);

    /**
     * 自增1
     *
     * @param key key
     */
    void incr(String key);

    /**
     * 自增1
     *
     * @param key            key
     * @param resultCallback 结果回调
     */
    void incr(String key, ResultCallback<Long> resultCallback);

    /**
     * 自增v
     *
     * @param key            key
     * @param v              value
     * @param resultCallback 结果回调
     */
    void incrBy(String key, long v, ResultCallback<Long> resultCallback);

    /**
     * 自减1
     *
     * @param key key
     */
    void decr(String key);

    /**
     * 自减1
     *
     * @param key            key
     * @param resultCallback 结果回调
     */
    void decr(String key, ResultCallback<Long> resultCallback);

    /**
     * 自减v
     *
     * @param key            key
     * @param v              value
     * @param resultCallback 结果回调
     */
    void decrBy(String key, long v, ResultCallback<Long> resultCallback);

    /**
     * 不存在时修改
     *
     * @param key key
     * @param v   value
     */
    void setIfAbsent(String key, V v);

    /**
     * 不存在时修改
     *
     * @param key            key
     * @param v              value
     * @param resultCallback 结果回调
     */
    void setIfAbsent(String key, V v, ResultCallback<Boolean> resultCallback);

    /**
     * 存在则修改
     *
     * @param key key
     * @param v   value
     */
    void setIfPresent(String key, V v);

    /**
     * 存在则修改
     *
     * @param key            key
     * @param v              value
     * @param resultCallback 结果回调
     */
    void setIfPresent(String key, V v, ResultCallback<Boolean> resultCallback);

    /**
     * 是否存在
     *
     * @param key            key
     * @param resultCallback 结果回调
     */
    void hasKey(String key, ResultCallback<Boolean> resultCallback);
}
