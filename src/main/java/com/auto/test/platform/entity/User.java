package com.auto.test.platform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;


/**
 * 用户实体类（对应user表）
 */
@Data
@TableName("user")
public class User {

    //用户ID
    @TableId(type=IdType.AUTO)
    private Long id;

    //用户名（唯一）
    @TableField("username")
    private String userName;

    //密码（加密存储）
    @TableField("password")
    private String password;

    //用户角色（admin：管理员，developer：开发人员，tester：测试人员）
    @TableField("role")
    private String role;

    //状态（1.启用，0：禁用）
    @TableField("status")
    private Integer status;

    //创建时间
    @TableField(value = "create_time", fill = FieldFill.INSERT) // 插入时自动填充
    private LocalDateTime createTime;

    //更新时间
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE) // 插入、更新时自动填充
    private LocalDateTime updateTime;
}
