package org.top.core.machine;

import org.top.core.machine.snapshot.KvEntity;
import org.top.models.LogEntry;

import java.util.List;

/**
 * @author lubeilin
 * @date 2020/12/1
 */
public interface SnapshotService {
    /**
     * 生成快照
     *
     * @param logEntry 日志条目
     * @throws Exception 异常
     */
    void save(LogEntry logEntry) throws Exception;

    /**
     * 获取快照最终的索引
     *
     * @return 索引值
     * @throws Exception 操作异常
     */
    long snapshotLastIndex() throws Exception;

    /**
     * 获取快照最终的任期
     *
     * @return 任期值
     * @throws Exception 操作异常
     */
    int snapshotLastTerm() throws Exception;

    /**
     * 获取快照分块
     *
     * @param lastKey 下一个快照key 不包含自己
     * @param maxLen  最大长度
     * @return 快照分块
     */
    List<KvEntity> getSnapshotData(byte[] lastKey, int maxLen);

}
