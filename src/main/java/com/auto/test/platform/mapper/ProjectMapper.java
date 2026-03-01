package com.auto.test.platform.mapper;

import com.auto.test.platform.entity.Project;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;


/**
 * 项目Mapper接口（数据访问层，架构mapper层核心）
 * 继承BaseMapper，获得MyBatis-Plus提供的CRUD方法（无需手动编写SQL）
 */
public interface ProjectMapper extends BaseMapper<Project> {
    // 基础CRUD方法已通过BaseMapper继承，复杂SQL可后续在xml中编写
}
