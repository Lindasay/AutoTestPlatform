package com.auto.test.platform.common.enums;

import lombok.Getter;

/**
 * 用例类型枚举（对应test_case表的case_type字段，架构必备）
 */
@Getter
public enum CaseTypeEnum {
    API_CASE(1,"接口用例"),
    UI_CASE(2,"UI用例");

    private final Integer code;
    private final String desc;
    CaseTypeEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /**
     * 根据状态码获取枚举
     */
    public static CaseTypeEnum getByCode(Integer code) {
        for (CaseTypeEnum caseTypeEnum : values()) {
            if(caseTypeEnum.getCode().equals(code)){
                return caseTypeEnum;
            }
        }
        throw new IllegalArgumentException("无效的用例类型：" + code);
    }
}
