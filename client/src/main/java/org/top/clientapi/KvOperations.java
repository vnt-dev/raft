package org.top.clientapi;

/**
 * @author lubeilin
 * @date 2020/12/15
 */
public interface KvOperations<V> {

    /**
     * kv 操作
     *
     * @return
     */
    ValueOperations<V> opsForValue();
}
