package com.auto.test.platform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 项目实体类（与Project表一一对应，架构entity层核心）
 * 字段注释与数据表注释一致，便于维护
 */
@Data //Lombok注解，自动生成getter、setter、toString等方法
@TableName("project") // 指定对应数据库表名（与MySQL表名一致）
public class Project {
    /**
     * 项目ID（主键，自增，对应数据库id字段）
     */
    @TableId(type = IdType.AUTO) //主键自增，与数据库表主键策略一致
    private Long id;

    /**
     * 项目名称（对应数据库project_name字段，非空、唯一）
     */
    @TableField("project_name") // 指定对应数据库字段名（下划线转驼峰可省略，此处显式指定更规范）
    private String projectName;

    /**
     * 项目描述（对应数据库project_desc字段）
     */
    @TableField("project_desc")
    private String projectDesc;

    /**
     * 项目状态（对应数据库status字段，0-禁用，1-启用，默认1）
     */
    @TableField("status")
    private Integer status;

    /**
     * 创建时间（对应数据库create_time字段，自动填充，无需手动设置）
     * 核心：fill属性必须配置，且字段名和处理器中的一致（createTime）
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT) // 插入时自动填充
    private LocalDateTime createTime;

    /**
     * 更新时间（对应数据库update_time字段，插入、更新时自动填充）
     */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE) // 插入、更新时自动填充
    private LocalDateTime updateTime;

}
