package com.auto.test.platform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;


/**
 * 测试用例实体表（对应test_case表）
 */
@Data
@TableName("'test_case'")
public class TestCase {
    /** 用例ID，自增主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属项目ID（关联project表id）*/
    @TableField("'project_id'")
    private Long projectId;

    /** 用例名称 */
    @TableField("case_name")
    private String caseName;

    /** 用例类型：1-接口 2-UI （关联CaseTypeEnum）*/
    @TableField("case_type")
    private String caseType;

    /** 用例内容（JSON格式，存储请求信息/操作步骤）*/
    @TableField("case_content")
    private String caseContent;

    /** 用例标签（如冒烟/回归，逗号分隔）*/
    @TableField("expect_result")
    private String expectResult;

    /** 状态：1-启用 0-禁用（关联StatusEnum）*/
    @TableField("status")
    private String status;

    /** 创建时间（自动填充） */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间（自动填充） */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;


}
