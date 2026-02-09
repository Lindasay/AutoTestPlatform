package com.auto.test.platform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 报告数据实体（对应report_data表，已适配MySQL默认值问题）
 */
@Data
@TableName("report_data")
public class ReportData {
    /** 报告ID，自增主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联任务执行表id（唯一） */
    @TableField("task_id")
    private Long taskId;

    @TableField("project_id")
    private Long projectId;

    @TableField("total_case")
    private Integer totalCase;

    @TableField("success_case")
    private Integer successCase;

    @TableField("fail_case")
    private Integer failCase;

    @TableField("success_rate")
    private BigDecimal successRate;

    /** Allure报告存储路径（绝对路径/相对路径） */
    @TableField("report_path")
    private String reportPath;

    /** 创建时间（自动填充） */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
