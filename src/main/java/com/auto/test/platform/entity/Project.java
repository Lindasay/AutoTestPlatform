package com.auto.test.platform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 测试项目实体（对应Project表）
 */
@Data
@TableName("project") // 关联数据库表名
public class Project {
    @TableId(type = IdType.AUTO)
    private Long id;

    // 驼峰转下划线，无需手动指定@TableField
    private String projectName;
    private String projectDesc;
    private Integer status;

    // 核心：fill属性必须配置，且字段名和处理器中的一致（createTime）
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

}
