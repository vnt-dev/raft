package org.top.models;

import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.top.rpc.Node;
import org.top.rpc.NodeGroup;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 领导者（服务器）上的易失性状态 (选举后已经重新初始化)
 *
 * @author lubeilin
 * @date 2020/11/17
 */
@Getter
@Slf4j
@ToString
public class LeaderStateModel {
    /**
     * 对于每一台服务器，发送到该服务器的下一个日志条目的索引（初始值为领导者最后的日志条目的索引+1）
     */
    private Map<Node, Long> nextIndex = new ConcurrentHashMap<>();
    /**
     * 对于每一台服务器，已知的已经复制到该服务器的最高日志条目的索引（初始值为0，单调递增）
     */
    private Map<Node, Long> matchIndex = new ConcurrentHashMap<>();

    public LeaderStateModel(long index) {
        NodeGroup.getNodeGroup().forEach(node -> {
            //初始值为领导者最后的日志条目的索引+1
            nextIndex.put(node, index + 1);
            //初始值为0
            matchIndex.put(node, 0L);
        });
        log.info("初始化信息,nextIndex：{}，matchIndex：{}", nextIndex, matchIndex);
    }

    public long getNext(Node node) {
        return nextIndex.get(node);
    }

    public void setNextIndexForNode(Node node, long index) {
        nextIndex.put(node, index);
    }
}
