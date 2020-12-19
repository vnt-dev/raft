package org.top.rpc.codec;

/**
 * @author: lubeilin
 * @time: 2020/9/6
 */
public interface Serializer<T> {
    /**
     * 编码
     *
     * @param data 传输对象
     * @return 序列化后的字节数组
     */
    byte[] serialize(T data);

    /**
     * 解码
     *
     * @param bytes 字节数组
     * @param clazz 目标类
     * @return 解码后对象
     */
    T deserialize(byte[] bytes, Class<T> clazz);

    /**
     * 解码
     *
     * @param bytes 字节数组
     * @param t     目标对象
     * @return 填充后对象
     */
    T deserialize(byte[] bytes, T t);
}
