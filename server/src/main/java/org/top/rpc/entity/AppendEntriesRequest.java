package org.top.rpc.entity;

import lombok.Data;
import org.top.models.LogEntry;
import org.top.rpc.Node;
import org.top.rpc.codec.BaseMessage;

import java.util.List;

/**
 * 日志复制请求数据
 *
 * @author lubeilin
 * @date 2020/11/17
 */
@Data
public class AppendEntriesRequest extends BaseMessage {
    public static final long serialVersionUID = -521572154342549866L;
    /**
     * 领导者的任期
     */
    private int term;

    /**
     * 领导者ID 因此跟随者可以对客户端进行重定向（译者注：跟随者根据领导者id把客户端的请求重定向到领导者，比如有时客户端把请求发给了跟随者而不是领导者）
     */
    private Node leaderId;

    /**
     * 紧邻新日志条目之前的那个日志条目的索引
     */
    private long preLogIndex;

    /**
     * 紧邻新日志条目之前的那个日志条目的任期
     */
    private long preLogTerm;

    /**
     * 需要被保存的日志条目（被当做心跳使用是 则日志条目内容为空；为了提高效率可能一次性发送多个）
     */
    private List<LogEntry> entries;

    /**
     * 领导者的已知已提交的最高的日志条目的索引，这个的作用是通知从节点提交数据，会有延迟
     */
    private long leaderCommit;
}
