package org.top.clientapi.entity;

/**
 * @author lubeilin
 * @date 2020/12/25
 */
public enum OperationState {
    /**
     * 执行成功
     */
    SUCCESS,
    /**
     * 执行失败
     */
    FAIL,
    /**
     * 系统异常
     */
    ERROR,
    /**
     * 主节点转移
     */
    LEADER_TURN,

}
