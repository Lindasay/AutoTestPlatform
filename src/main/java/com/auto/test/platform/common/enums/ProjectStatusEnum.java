package com.auto.test.platform.common.enums;

import lombok.Getter;

/**
 * 项目枚举状态（替代魔法值，对应project表status字段，架构必备）
 */
@Getter
public enum ProjectStatusEnum {
    ENABLE(1,"启用"),
    DISABLE(0,"禁用");

    private final Integer code; //状态码（与数据库字段一致）
    private final String desc; //状态描述（用于接口返回）
    ProjectStatusEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /**
     * 根据状态码获取枚举（企业级规范，便于转换）
     */
    public static ProjectStatusEnum getByCode(Integer code) {
        for (ProjectStatusEnum e : values()) {
            if(e.getCode().equals(code)){
                return e;
            }
        }
        throw new IllegalArgumentException("无效的项目状态：" + code);
    }
}
