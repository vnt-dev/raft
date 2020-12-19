package org.top.clientapi;

import org.top.clientapi.codec.DefaultValueSerializer;
import org.top.clientapi.codec.ValueSerializer;

/**
 * @author lubeilin
 * @date 2020/12/16
 */
public class BaseKvOperations<V> implements KvOperations<V> {
    private ValueOperations<V> valueOperations;

    public BaseKvOperations(Class<V> entityClass) {
        this(entityClass, new DefaultValueSerializer<>());
    }

    public BaseKvOperations(Class<V> entityClass, ValueSerializer<V> valueSerializer) {
        valueOperations = new DefaultValueOperations<>(valueSerializer, entityClass);
    }

    @Override
    public ValueOperations<V> opsForValue() {
        return this.valueOperations;
    }


}
