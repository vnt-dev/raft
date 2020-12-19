package org.top.rpc.codec;


import io.protostuff.LinkedBuffer;
import io.protostuff.ProtostuffIOUtil;
import io.protostuff.Schema;
import io.protostuff.runtime.RuntimeSchema;


/**
 * 序列化器
 *
 * @author: lubeilin
 * @time: 2020/9/6
 */
public class ProtoBufSerializer<T> implements Serializer<T> {
    @Override
    public byte[] serialize(T data) {
        LinkedBuffer buffer = null;
        try {
            @SuppressWarnings("unchecked")
            Schema<T> schema = RuntimeSchema.getSchema((Class<T>) data.getClass());
            buffer = LinkedBuffer.allocate(LinkedBuffer.DEFAULT_BUFFER_SIZE);
            return ProtostuffIOUtil.toByteArray(data, schema, buffer);
        } finally {
            if (buffer != null) {
                buffer.clear();
            }
        }
    }

    @Override
    public T deserialize(byte[] bytes, Class<T> clazz) {
        try {
            Schema<T> schema = RuntimeSchema.getSchema(clazz);
            T t = clazz.getDeclaredConstructor().newInstance();
            ProtostuffIOUtil.mergeFrom(bytes, t, schema);
            return t;
        } catch (Exception e) {
            throw new RuntimeException("解码错误", e);
        }
    }

    @Override
    public T deserialize(byte[] bytes, T t) {
        @SuppressWarnings("unchecked")
        Schema<T> schema = (Schema<T>) RuntimeSchema.getSchema(t.getClass());
        ProtostuffIOUtil.mergeFrom(bytes, t, schema);
        return t;
    }
}
