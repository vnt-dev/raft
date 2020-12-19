package org.top.core;

/**
 * @author lubeilin
 * @date 2020/11/10
 */
public interface ServerStateTransformer {
    /**
     * 执行前
     *
     * @return 返回为true时才进入
     */
    default boolean pro() {
        return true;
    }

    /**
     * 状态执行入口
     *
     * @throws Exception
     */
    void execute() throws Exception;

    /**
     * 获取下个状态
     *
     * @return 下一个状态
     */
    ServerStateEnum nextState();

}
