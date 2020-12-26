package org.top.core.machine;


import org.top.core.machine.snapshot.KvEntity;
import org.top.models.LogEntry;

import java.util.List;

/**
 * 状态机中的数据一定是已提交的，但是从节点的数据会有延迟
 *
 * @author lubeilin
 * @date 2020/11/12
 */
public interface StateMachine {
    /**
     * 执行状态机
     *
     * @param logEntryModel 日志
     * @return 数据
     * @throws Exception 异常
     */
    MachineResult execute(LogEntry logEntryModel) throws Exception;

    /**
     * 存储快照数据
     *
     * @param data  kv集合
     * @param term  最后任期
     * @param index 最后索引
     * @throws Exception 操作异常
     */
    void execute(List<KvEntity> data, int term, long index) throws Exception;

    /**
     * 获取数据
     *
     * @param key key
     * @return 数据
     * @throws Exception 执行异常
     */
    byte[] get(byte[] key) throws Exception;

    /**
     * 重置状态机
     *
     * @throws Exception 操作异常
     */
    void reset() throws Exception;
}
