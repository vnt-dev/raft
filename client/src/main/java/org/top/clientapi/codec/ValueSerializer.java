package org.top.clientapi.codec;

/**
 * @author lubeilin
 * @date 2020/12/16
 */
public interface ValueSerializer<T> {
    /**
     * 编码
     *
     * @param t
     * @return
     */
    byte[] serialize(T t);

    /**
     * 解码
     *
     * @param bytes
     * @param tClass
     * @return
     */
    T deserialize(byte[] bytes, Class<T> tClass);
}
