package org.top.clientapi.async;

/**
 * @author lubeilin
 * @date 2020/12/19
 */
public interface ResultCallback<T> {
    /**
     * 成功
     *
     * @param t
     */
    void success(T t);

    /**
     * 失败
     *
     * @param msg
     */
    default void fail(String msg) {

    }
}
