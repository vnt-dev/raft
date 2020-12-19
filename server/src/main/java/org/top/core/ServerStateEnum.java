package org.top.core;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author lubeilin
 * @date 2020/11/10
 */
@AllArgsConstructor
public enum ServerStateEnum {
    /**
     * 节点状态(角色)
     */
    LEADER("LEADER", "领导者"),
    CANDIDATE("CANDIDATE", "候选者"),
    FOLLOWER("FOLLOWER", "跟随者");
    @Getter
    private String code;
    @Getter
    private String desc;
}
