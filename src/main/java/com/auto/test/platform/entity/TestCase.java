package com.auto.test.platform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 测试用例实体类（与test_case表一一对应）
 */
@Data
@TableName("test_case")
public class TestCase {
    /** 用例ID，自增主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 关联项目ID（对应数据库project_id字段，外键）
     */
    @TableField("project_id")
    private Long projectId;

    /**
     * 用例名称（对应数据库case_name字段，非空）
     */
    @TableField("case_name")
    private String caseName;

    /**
     * 用例类型（对应数据库case_type字段，1-接口用例，2-UI用例）
     */
    @TableField("case_type")
    private Integer caseType;

    /**
     * 用例内容（对应数据库case_content字段，JSON格式，非空）
     */
    @TableField("case_content")
    private String caseContent;

    /**
     * 用例内容（对应数据库case_content字段，JSON格式，非空）
     */
    @TableField("status")
    private Integer status;

    /** 创建时间（自动填充） */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间（自动填充） */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
