package org.top.core.machine;

/**
 * @author lubeilin
 * @date 2020/11/12
 */
public interface StateMachineHandler {

    /**
     * 提交到状态机
     *
     * @throws Exception 异常
     */
    void commit() throws Exception;

    /**
     * 状态机循环
     */
    void startLoop();

    /**
     * 启动循环
     */
    void start();

    /**
     * 暂停
     */
    void awaitPause();
}
