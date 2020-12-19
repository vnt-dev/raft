package org.top.models;

import lombok.Data;
import org.top.core.machine.snapshot.SnapshotLoad;

/**
 * 所有服务器上的易失性状态
 *
 * @author lubeilin
 * @date 2020/11/17
 */
@Data
public class ServerStateModel {
    /**
     * 已知已提交的最高的日志条目的索引，初始值为快照最后索引，单调递增
     */
    private volatile long commitIndex = 0;
    /**
     * 已经被应用到状态机的最高的日志条目的索引，初始值为快照最后索引，单调递增
     */
    private volatile long lastApplied = 0;

    public ServerStateModel() {
        try {
            lastApplied = commitIndex = new SnapshotLoad().getIndex();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
