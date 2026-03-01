package com.auto.test.platform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 任务执行实体类（与task_execution表一一对应）
 */
@Data
@TableName("task_execution")
public class TaskExecution {

    /**
     * 任务ID（主键，自增）
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 关联项目ID（对应数据库project_id字段）
     */
    @TableField("project_id")
    private Long projectId;

    /**
     * 任务名称（对应数据库task_name字段，非空）
     */
    @TableField("task_name")
    private String taskName;

    /**
     * 任务状态（对应数据库task_status字段，0-未执行，1-执行中，2-执行成功，3-执行失败）
     */
    @TableField("task_status")
    private Integer taskStatus;

    /**
     * 执行时间（对应数据库execute_time字段）
     */
    @TableField("execute_time")
    private LocalDateTime executeTime;

    /**
     * 执行时长（单位：毫秒，对应数据库execute_duration字段）
     */
    @TableField("execute_duration")
    private Integer executeDuration;

    /**
     * 创建时间（自动填充）
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间（自动填充）
     */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
