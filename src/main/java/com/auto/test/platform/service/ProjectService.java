package com.auto.test.platform.service;

import com.auto.test.platform.common.result.Result;
import com.auto.test.platform.entity.Project;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 项目管理Service接口（业务逻辑层，架构Service层核心）
 * 继承IService。获得MyBatis-Plus提供的分页、批量操作等增强方法
 */
public interface ProjectService extends IService<Project> {

    /**
     * 新增项目（业务逻辑：校验项目名称唯一、参数非空）
     * @param project 项目实体（包含项目名称、描述、状态）
     * @return 统一响应结果
     */
    Result<?> addProject(Project project);

    /**
     * 修改项目（业务逻辑：校验项目存在、项目名称唯一）
     * @param project 项目实体（包含项目ID、修改后的信息）
     * @return 统一响应结果
     */
    Result<?> updateProject(Project project);

    /**
     * 删除项目（业务逻辑：校验项目存在、禁止删除关联用例的项目）
     * @param id 项目ID
     * @return 统一响应结果
     */
    Result<?> deleteProject(Long id);

    /**
     * 根据ID查询项目（业务逻辑：校验项目存在）
     * @param id 项目ID
     * @return 统一响应结果（包含项目详情）
     */
    Result<Project> getProjectById(Long id);

    /**
     * 项目分页查询（支持模糊查询项目名称，适配前端分页展示）
     * @param pageNum 当前页码（默认）
     * @param pageSize 每页条数（默认10，最大100）
     * @param projectName 项目名称（模糊查询，可为null）
     * @return 统一响应结果（包含分页数据）
     */
    Result<IPage<Project>> getProjectPage(Integer pageNum, Integer pageSize, String projectName);

    /**
     * 修改项目状态（启用/禁用，业务逻辑：校验项目存在）
     * @param id 项目ID
     * @param status 目标状态（0-禁用，1-启用）
     * @return 统一响应结果
     */
    Result<?> updateProjectStatus(Long id, Integer status);
}
