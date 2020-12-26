package org.top.clientapi.sync;


/**
 * @author lubeilin
 * @date 2020/12/16
 */
public interface ValueOperations<V> {
    /**
     * 删除
     *
     * @param key key
     */
    void delete(String key);

    /**
     * 获取
     *
     * @param key key
     * @return 结果
     */
    V get(String key);

    /**
     * 修改
     *
     * @param key key
     * @param v   待修改值
     */
    void set(String key, V v);

    /**
     * 修改
     *
     * @param key          key
     * @param v            待修改值
     * @param milliseconds 过期时间
     */
    void set(String key, V v, Long milliseconds);

    /**
     * 自增1
     *
     * @param key key
     * @return 结果
     */
    long incr(String key);

    /**
     * 自增1
     *
     * @param key          key
     * @param milliseconds 过期时间
     * @return 结果
     */
    long incr(String key, Long milliseconds);

    /**
     * 自增
     *
     * @param key key
     * @param val 增加的值
     * @return 结果
     */
    long incrBy(String key, long val);

    /**
     * 自增
     *
     * @param key          key
     * @param val          增加的值
     * @param milliseconds 过期时间
     * @return 结果
     */
    long incrBy(String key, long val, Long milliseconds);

    /**
     * 自减1
     *
     * @param key key
     * @return 结果
     */
    long decr(String key);

    /**
     * 自减1
     *
     * @param key          key
     * @param milliseconds 过期时间
     * @return 结果
     */
    long decr(String key, Long milliseconds);

    /**
     * 自减
     *
     * @param key key
     * @param val 减少的值
     * @return 结果
     */
    long decrBy(String key, long val);

    /**
     * 自减
     *
     * @param key          key
     * @param val          减少的值
     * @param milliseconds 过期时间
     * @return 结果
     */
    long decrBy(String key, long val, Long milliseconds);

    /**
     * 不存在则修改
     *
     * @param key key
     * @param v   修改值
     * @return 是否成功
     */
    boolean setIfAbsent(String key, V v);

    /**
     * 不存在则修改
     *
     * @param key          key
     * @param v            修改值
     * @param milliseconds 过期时间
     * @return 是否成功
     */
    boolean setIfAbsent(String key, V v, Long milliseconds);

    /**
     * 存在则修改
     *
     * @param key key
     * @param v   修改值
     * @return 是否成功
     */
    boolean setIfPresent(String key, V v);

    /**
     * 存在则修改
     *
     * @param key          key
     * @param v            修改值
     * @param milliseconds 过期时间
     * @return 是否成功
     */
    boolean setIfPresent(String key, V v, Long milliseconds);

    /**
     * 是否存在
     *
     * @param key key
     * @return 是否存在
     */
    boolean hasKey(String key);
}
