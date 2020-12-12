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
    SET("SET", "添加"),
    DEL("DEL", "删除"),
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
        throw new RuntimeException("code不存在");
    }

}
