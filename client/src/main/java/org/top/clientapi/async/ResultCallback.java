package org.top.clientapi.async;

/**
 * 不能在此回调中使用同步命令
 * 执行耗时较长的逻辑时建议另起线程，否则会阻塞后续命令执行
 *
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
        System.out.println(msg);
    }
}
