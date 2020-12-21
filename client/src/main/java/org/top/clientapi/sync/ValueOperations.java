package org.top.clientapi.sync;


/**
 * @author lubeilin
 * @date 2020/12/16
 */
public interface ValueOperations<V> {
    /**
     * 删除
     *
     * @param key
     */
    void delete(String key);

    /**
     * 获取
     *
     * @param key
     * @return
     */
    V get(String key);

    /**
     * 修改
     *
     * @param key
     * @param v
     */
    void set(String key, V v);

    /**
     * 自增1
     *
     * @param key
     * @return
     */
    long incr(String key);

    /**
     * 自增
     *
     * @param key
     * @param val
     * @return
     */
    long incrBy(String key, long val);

    /**
     * 自减1
     *
     * @param key
     * @return
     */
    long decr(String key);

    /**
     * 自减
     *
     * @param key
     * @param val
     * @return
     */
    long decrBy(String key, long val);

    /**
     * 不存在则修改
     *
     * @param key
     * @param v
     * @return
     */
    boolean setIfAbsent(String key, V v);

    /**
     * 存在则修改
     *
     * @param key
     * @param v
     * @return
     */
    boolean setIfPresent(String key, V v);

    /**
     * 是否存在
     *
     * @param key
     * @return
     */
    boolean hasKey(String key);
}
