package com.auto.test.platform.common.constant;

import lombok.Getter;

/**
 * 测试用例类型枚举（对应test_case表case_type字段）
 * 1-接口用例  2-UI用例
 */
@Getter
public enum CaseTypeEnum {
    API_CASE(1, "接口用例"),
    UI_CASE(2, "UI用例");

    private  final Integer code; //数据库存储的数字编码
    private final String desc; //描述

    CaseTypeEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    //根据编码获取枚举（后续业务层常用）
    public static CaseTypeEnum getByCode(Integer code) {
        for (CaseTypeEnum caseTypeEnum : CaseTypeEnum.values()) {
            if (caseTypeEnum.getCode().equals(code)) {
                return caseTypeEnum;
            }
        }
        throw new RuntimeException("无效的用例类型编码：" + code);
    }

}
