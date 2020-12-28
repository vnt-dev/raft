package org.top.core.machine;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

/**
 * 操作枚举
 *
 * @author lubeilin
 * @date 2020/11/12
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public enum OptionEnum {

    /**
     * 操作
     */
    GET("GET", "获取"),
    EXPIRE("EXPIRE", "设置过期时间"),
    SET("SET", "添加"),
    DEL("DEL", "删除"),
    COMPARE_AND_DEL("COMPARE_AND_DEL", "比较再删除"),
    INCR("INCR", "自增"),
    RESET_INCR("RESET_INCR", "重置再自增"),
    DECR("DECR", "自减"),
    RESET_DECR("RESET_DECR", "重置再自减"),
    SET_IF_ABSENT("SET_IF_ABSENT", "不存在时修改"),
    SET_IF_PRESENT("SET_IF_PRESENT", "存在时修改"),
    HAS_KEY("HAS_KEY", "是否存在"),
    UP("UP", "主节点上线"),
    ;

    /**
     * 枚举code
     */
    @Getter
    private String code;

    /**
     * 描述
     */
    @Getter
    private String desc;

    /**
     * 根据code获取枚举【不忽略大小写】
     *
     * @param code 枚举code
     * @return 枚举
     */
    public static OptionEnum getByCode(String code) {
        for (OptionEnum anEnum : OptionEnum.values()) {
            if (StringUtils.equals(anEnum.getCode(), code)) {
                return anEnum;
            }
        }
        return null;
    }

}
