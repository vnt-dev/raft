package org.top.clientapi;

import org.top.clientapi.codec.DefaultValueSerializer;
import org.top.clientapi.codec.ValueSerializer;

/**
 * @author lubeilin
 * @date 2020/12/16
 */
public class BaseKvOperations<V> implements KvOperations<V> {
    private ValueSerializer<V> valueSerializer = new DefaultValueSerializer<>();
    private Class<V> entityClass;

    public BaseKvOperations(Class<V> entityClass) {
        this.entityClass = entityClass;
    }

    @Override
    public void setSerializer(ValueSerializer<V> valueSerializer) {
        this.valueSerializer = valueSerializer;
    }

    @Override
    public ValueOperations<V> opsForValue() {
        return new DefaultValueOperations<>(valueSerializer, entityClass);
    }


}
