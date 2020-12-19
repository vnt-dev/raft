package org.top.clientapi.codec;

import org.top.rpc.codec.ProtoBufSerializer;

/**
 * @author lubeilin
 * @date 2020/12/16
 */
public class DefaultValueSerializer<T> implements ValueSerializer<T> {
    private ProtoBufSerializer<T> serializer = new ProtoBufSerializer<>();

    @Override
    public byte[] serialize(T t) {
        return serializer.serialize(t);
    }

    @Override
    public T deserialize(byte[] bytes, Class<T> tClass) {
        return serializer.deserialize(bytes, tClass);
    }
}
