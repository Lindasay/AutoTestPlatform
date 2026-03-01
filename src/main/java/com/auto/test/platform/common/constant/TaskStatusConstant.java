package com.auto.test.platform.common.constant;

/**
 * 任务状态常量（对应task_execution表task_status字段，架构必备）
 */
public class TaskStatusConstant {
    /** 未执行 */
    public static final Integer UN_EXECUTED = 0;

    /** 执行中 */
    public static final Integer EXECUTING = 1;

    /** 执行成功 */
    public static final Integer SUCCESS = 2;

    /** 执行失败 */
    public static final Integer FAIL = 3;
}
