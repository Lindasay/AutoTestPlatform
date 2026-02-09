package com.auto.test.platform.common.constant;

import lombok.Getter;

/**
 * 任务执行状态枚举（对应task_execution表execute_status字段）
 * 0-待执行  1-执行中  2-执行完成  3-执行失败
 */
@Getter
public enum TaskStatusEnum {
    PENDING(0, "待执行"),
    RUNNING(1, "执行中"),
    FINISHED(2, "执行完成"),
    FAILED(3, "执行失败");

    private final Integer code;
    private final String desc;
    TaskStatusEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static TaskStatusEnum getByCode(Integer code) {
        for (TaskStatusEnum taskStatusEnum : TaskStatusEnum.values()) {
            if (taskStatusEnum.getCode().equals(code)) {
                return taskStatusEnum;
            }
        }
        throw new RuntimeException("无效的任务状态编码：" + code);
    }
}
