package com.auto.test.platform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 任务执行实体（对应task_execution表）
 */
@Data
@TableName("task_execution")
public class TaskExecution {

    /** 任务ID，自增主键*/
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属项目ID（关联project表id）*/
    @TableField("project_id")
    private Long projectId;

    /** 任务名称 */
    @TableField("task_name")
    private String taskName;


    @TableField("case_ids")
    private String caseIds;

    /** 执行状态：0-待执行 1-执行中 2-执行完成 3-执行失败（关联TaskStatusEnum） */
    @TableField("execute_status")
    private Integer executeStatus;

    @TableField("execute_time")
    private LocalDateTime executeTime;

    @TableField("execute_duration")
    private Integer executeDuration;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
