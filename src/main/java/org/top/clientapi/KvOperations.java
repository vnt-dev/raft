package org.top.clientapi;

import org.top.clientapi.codec.ValueSerializer;

/**
 * @author lubeilin
 * @date 2020/12/15
 */
public interface KvOperations<V> {
    /**
     * 设置序列化器
     *
     * @param valueSerializer
     */
    void setSerializer(ValueSerializer<V> valueSerializer);

    /**
     * kv 操作
     *
     * @return
     */
    ValueOperations<V> opsForValue();
}
