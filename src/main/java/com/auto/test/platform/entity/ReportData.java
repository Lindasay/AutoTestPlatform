package com.auto.test.platform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 测试报告实体类（与report_data表一一对应）
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

    /**
     * 关联项目ID（对应数据库project_id字段）
     */
    @TableField("project_id")
    private Long projectId;

    /**
     * 通过用例数（对应数据库pass_count字段）
     */
    @TableField("pass_count")
    private Integer passCount;

    /**
     * 失败用例数（对应数据库fail_count字段）
     */
    @TableField("fail_count")
    private Integer failCount;

    /**
     * 总用例数（对应数据库total_count字段）
     */
    @TableField("total_count")
    private Integer totalCount;

    /**
     * 通过率（对应数据库pass_rate字段，保留2位小数）
     */
    @TableField("pass_rate")
    private BigDecimal passRate;

    /**
     * 报告详情（对应数据库report_content字段，JSON格式）
     */
    @TableField("report_content")
    private String reportContent;

    /**
     * 创建时间（自动填充）
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
