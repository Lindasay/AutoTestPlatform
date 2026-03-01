package com.auto.test.platform.service.impl;

import com.auto.test.platform.common.constant.ResponseCodeConstant;
import com.auto.test.platform.common.enums.ProjectStatusEnum;
import com.auto.test.platform.common.exception.BusinessException;
import com.auto.test.platform.common.result.Result;
import com.auto.test.platform.common.util.AssertUtil;
import com.auto.test.platform.entity.Project;
import com.auto.test.platform.entity.TestCase;
import com.auto.test.platform.mapper.ProjectMapper;
import com.auto.test.platform.mapper.TestCaseMapper;
import com.auto.test.platform.service.ProjectService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 项目管理Service实现类（业务逻辑具体实现）
 * @Service：标识为业务层组件，交给Spring容器管理
 * 继承ServiceImpl，获得IService、BaseMapper的所有方法
 */
@Service
public class ProjectServiceImpl extends ServiceImpl<ProjectMapper, Project> implements ProjectService {

    // 注入测试用例Mapper（用于校验项目是否关联用例）
    @Resource
    private TestCaseMapper testCaseMapper;

    /**
     * 新增项目实现
     * @param project 项目实体（包含项目名称、描述、状态）
     * @return
     */
    @Override
    @Transactional //事务注解：确保新增操作原子性，失败回滚
    public Result<?> addProject(Project project) {
        // 1.参数校验（使用AssertUtil工具类，简化if-else）
        AssertUtil.notBlank(project.getProjectName(), "项目名称不能为空");
        // 校验项目名称长度（不超过50位，与数据库表字段一致）
        AssertUtil.isTrue(project.getProjectName().length() <= 50, "项目名称不能超过50个字符");
        // 校验项目描述长度（不超过200位）
        if (project.getProjectDesc() != null){
            AssertUtil.isTrue(project.getProjectDesc().length() <= 200,"项目描述长度不能超过200个字符");
        }
        //校验项目状态（只能是0或1）
        AssertUtil.isTrue(project.getStatus() == 0 || project.getStatus() == 1, "项目状态只能是启用（1）或禁用（0）");

        // 2.校验项目名称唯一（数据库表唯一索引，此处再次校验，返回更友好的提示）
        LambdaQueryWrapper<Project> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Project::getProjectName, project.getProjectName());
        Project existProject = baseMapper.selectOne(queryWrapper);
        AssertUtil.isTrue(existProject == null, ResponseCodeConstant.DATA_ALREADY_EXIST,"项目名称已存在，请更换");

        // 3.新增项目（baseMapper继承自BaseMapper，直接调用insert方法）
        baseMapper.insert(project);

        // 4. 返回成功响应
        return Result.success("项目新增成功");
    }

    /**
     * 修改项目实现
     * @param project 项目实体（包含项目ID、修改后的信息）
     * @return
     */
    @Override
    @Transactional
    public Result<?> updateProject(Project project) {
        // 1.参数校验
        AssertUtil.notNull(project.getId(),"项目ID不能为空");
        AssertUtil.notNull(project.getProjectName(),"项目名称不能为空");
        AssertUtil.isTrue(project.getProjectName().length() <= 50,"项目名称不能超过50个字符");
        if (project.getProjectDesc() != null){
            AssertUtil.isTrue(project.getProjectDesc().length() <= 200,"项目描述长度不能超过200个字符");
        }
        AssertUtil.isTrue(project.getStatus() == 0 || project.getStatus() ==1,"项目状态只能是启用（1）或禁用（0）");

        // 2.校验项目存在
        Project existProject = baseMapper.selectById(project.getId());
        AssertUtil.notNull(existProject,"待修改的项目不存在");

        // 3.检验项目名称唯一（排除自身）
        LambdaQueryWrapper<Project> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Project::getProjectName, project.getProjectName())
                .ne(Project::getId, project.getId()); //不等当前项目ID
        Project repeatProject = baseMapper.selectOne(queryWrapper);
        AssertUtil.isTrue(repeatProject == null, ResponseCodeConstant.DATA_ALREADY_EXIST,"项目名称已存在，请更换");

        // 4.修改项目（只修改允许修改的字段：项目名称、描述、状态，避免修改创建时间等字段）
        existProject.setProjectName(project.getProjectName());
        existProject.setProjectDesc(project.getProjectDesc());
        existProject.setStatus(project.getStatus());
        baseMapper.updateById(existProject);

        // 5.返回成功响应
        return Result.success("项目修改成功");
    }

    /**
     * 删除项目实现
     * @param id 项目ID
     * @return
     */
    @Override
    @Transactional
    public Result<?> deleteProject(Long id) {

        // 1. 参数校验
        AssertUtil.notNull(id,"项目ID不能为空");

        // 2. 校验项目存在
        Project existProject = baseMapper.selectById(id);
        AssertUtil.notNull(existProject,"待删除的项目不存在");

        // 3.校验项目是否关联测试用例（关联用例则禁止删除，避免数据异常）
        LambdaQueryWrapper<TestCase> caseQueryWrapper = new LambdaQueryWrapper<>();
        caseQueryWrapper.eq(TestCase::getProjectId, id);
        Long caseCount = testCaseMapper.selectCount(caseQueryWrapper);
        AssertUtil.isTrue(caseCount == 0, "该项目已关联测试用例，禁止删除，请先删除关联用例");

        // 4.删除项目
        baseMapper.deleteById(id);

        // 5.返回成功响应
        return Result.success("项目删除成功");
    }

    /**
     * 根据ID查询项目实现
     * @param id 项目ID
     * @return
     */
    @Override
    public Result<Project> getProjectById(Long id) {
        // 1.参数校验
        AssertUtil.notNull(id,"项目ID不能为空");

        // 2.校验项目存在并查询
        Project existProject = baseMapper.selectById(id);
        AssertUtil.notNull(existProject,"查询的项目不存在");

        // 3.返回项目详情（统一响应格式）
        return Result.success(existProject);
    }

    /**
     * 项目分页查询实现
     * @param pageNum 当前页码（默认）
     * @param pageSize 每页条数（默认10）
     * @param projectName 项目名称（模糊查询，可为null）
     * @return
     */
    @Override
    public Result<IPage<Project>> getProjectPage(Integer pageNum, Integer pageSize, String projectName) {

        // 1. 处理分页参数（默认页码1，每页10条）
        int current = (pageNum == null || pageNum < 1) ? 1 : pageNum;
        int size = (pageSize == null || pageSize < 1) ? 10 : pageSize;
        size = Math.min(size, 100); //限制最大100条

        // 2. 构建分页对象（MyBatis-Plus分页插件）
        IPage<Project> page = new Page<>(current, size);

        // 3. 构建查询条件（模糊查询项目名称，无名称则查询所有）
        LambdaQueryWrapper<Project> queryWrapper = new LambdaQueryWrapper<>();
        if (projectName != null && !projectName.trim().isEmpty()) {
            queryWrapper.like(Project::getProjectName, projectName.trim());
        }
        // 可选：按更新时间排序，保证分页结果稳定
        queryWrapper.orderByDesc(Project::getUpdateTime);

        // 4. 关键修复：调用baseMapper.selectPage，分页插件自动计算total并注入到page中
        this.baseMapper.selectPage(page, queryWrapper);

        // 5.返回分页数据
        return Result.success(page);
    }

    /**
     * 修改项目状态
     * @param id 项目ID
     * @param status 目标状态（0-禁用，1-启用）
     * @return
     */
    @Override
    @Transactional
    public Result<?> updateProjectStatus(Long id, Integer status) {
        // 1.参数校验
        AssertUtil.notNull(id,"项目ID不能为空");
        AssertUtil.notNull(status,"项目状态不能为空");

        //校验项目状态合法性（通过枚举类，更规范）
        try {
            ProjectStatusEnum.getByCode(status);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ResponseCodeConstant.PARAM_ERROR,"项目状态无效，只能是启用（1）或禁用（0）");
        }

        //校验项目存在
        Project existProject = baseMapper.selectById(id);
        AssertUtil.notNull(existProject,"待修改状态的项目不存在");

        //校验状态是否一致(避免重复操作)
        if (existProject.getStatus().equals(status)) {
            return Result.success("项目当前已处于" + ProjectStatusEnum.getByCode(status).getDesc() + "状态，无需修改");
        }

        //修改项目状态
        existProject.setStatus(status);
        baseMapper.updateById(existProject);

        //返回成功响应
        return Result.success("项目状态已修改为" +  ProjectStatusEnum.getByCode(status).getDesc());
    }
}
