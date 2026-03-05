package com.auto.test.platform.service.impl;

import com.alibaba.fastjson2.JSONObject;
import com.auto.test.platform.common.constant.ResponseCodeConstant;
import com.auto.test.platform.common.enums.CaseTypeEnum;
import com.auto.test.platform.common.exception.BusinessException;
import com.auto.test.platform.common.result.Result;
import com.auto.test.platform.common.util.AssertUtil;
import com.auto.test.platform.entity.Project;
import com.auto.test.platform.entity.TestCase;
import com.auto.test.platform.mapper.ProjectMapper;
import com.auto.test.platform.mapper.TestCaseMapper;
import com.auto.test.platform.service.TestCaseService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 测试用例管理Service实现类
 */
@Service
public class TestCaseServiceImpl extends ServiceImpl<TestCaseMapper, TestCase> implements TestCaseService {

    //注入项目Mapper（用于校验关联项目是否存在）
    @Resource
    private ProjectMapper projectMapper;

    /**
     * 新增测试用例实现
     * @param testCase 测试用例实体
     * @return 返回成功响应
     */
    @Override
    @Transactional
    public Result<?> addTestCase(TestCase testCase) {

        // 1. 参数校验
        AssertUtil.notNull(testCase.getProjectId(),"关联项目ID不能为空");
        AssertUtil.notNull(testCase.getCaseName(),"用例名称不能为空");
        AssertUtil.isTrue(testCase.getCaseName().length() <= 100,"用例名称长度不能超过100个字符");
        AssertUtil.notNull(testCase.getCaseType(),"用例类型不能为空");
        AssertUtil.notBlank(testCase.getCaseContent(),"用例内容不能为空");
        AssertUtil.isTrue(testCase.getStatus() == 0 || testCase.getStatus() ==1,"项目状态只能是启用（1）或禁用（0）");

        // 2. 校验关联项目存在
        Project project = projectMapper.selectById(testCase.getProjectId());
        AssertUtil.notNull(project,"关联的项目不存在，请先创建项目");

        // 3. 校验用例类型合法（1-接口用例，2-UI用例）
        try {
            CaseTypeEnum.getByCode(testCase.getCaseType());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ResponseCodeConstant.PARAM_ERROR,"用例类型无效，只能是接口用例（1）或UI用例（2）");
        }

        // 4. 校验用例内容格式（简单校验是否为JSON格式，避免非法内容）
        //AssertUtil.isTrue(isJson(testCase.getCaseContent()),"用例内容必须为JSON格式");
        try {
            JSONObject.parseObject(testCase.getCaseContent());
        } catch (Exception e) {
            throw new BusinessException(ResponseCodeConstant.PARAM_ERROR,"用例内容必须为合法的JSON格式");
        }

        // 5.校验同一项目下用例名称唯一（避免同一项目内用例名称重复）
        LambdaQueryWrapper<TestCase> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TestCase::getProjectId,testCase.getProjectId())
                    .eq(TestCase::getCaseName,testCase.getCaseName());
        TestCase existTestCase = baseMapper.selectOne(queryWrapper);
        AssertUtil.isTrue(existTestCase == null, ResponseCodeConstant.DATA_ALREADY_EXIST,"该项目下已存在同名测试用例，请更换");

        // 6. 新增用例
        baseMapper.insert(testCase);
        
        // 7. 返回成功响应
        return Result.success(testCase.getId(),"测试用例新增成功");
    }

    /**
     * 修改测试用例实现
     * @param testCase 测试用例实体
     * @return 返回成功响应
     */
    @Override
    @Transactional

    public Result<?> updateTestCase(TestCase testCase) {

        // 1. 参数校验
        AssertUtil.notNull(testCase.getId(),"用例ID不能为空");
        AssertUtil.notNull(testCase.getProjectId(),"关联项目ID不能为空");
        AssertUtil.notNull(testCase.getCaseName(),"用例名称不能为空");
        AssertUtil.isTrue(testCase.getCaseName().length() <= 100,"用例名称长度不能超过100个字符");
        AssertUtil.notNull(testCase.getCaseType(),"用例类型不能为空");
        AssertUtil.notBlank(testCase.getCaseContent(),"用例内容不能为空");
        AssertUtil.isTrue(testCase.getStatus() == 0 || testCase.getStatus() ==1,"项目状态只能是启用（1）或禁用（0）");
        
        // 2.校验用例存在
        TestCase existCase = baseMapper.selectById(testCase.getId());
        AssertUtil.notNull(existCase,"待修改的测试用例不存在");

        // 3.校验关联项目存在
        Project project = projectMapper.selectById(testCase.getProjectId());
        AssertUtil.notNull(project,"关联的项目不存在");

        // 4.校验用例类型合法性
        try {
            CaseTypeEnum.getByCode(testCase.getCaseType());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ResponseCodeConstant.PARAM_ERROR,"用例类型无效，只能是接口用例（1）或UI用例（0）");
        }

        // 5.校验用例内容格式
        //AssertUtil.isTrue(isJson(testCase.getCaseContent()),"用例内容必须为JSON格式");
        try {
            JSONObject.parseObject(testCase.getCaseContent());
        } catch (Exception e) {
            throw new BusinessException(ResponseCodeConstant.PARAM_ERROR,"用例内容必须是合法的JSON格式");
        }

        // 7. 校验同一项目下用例名称唯一（排除自身）
        LambdaQueryWrapper<TestCase> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TestCase::getProjectId,testCase.getProjectId())
                .eq(TestCase::getCaseName,testCase.getCaseName())
                .ne(TestCase::getId,testCase.getId()); //排除当前修改的用例
        TestCase repeatCase = baseMapper.selectOne(queryWrapper);
        AssertUtil.isTrue(repeatCase == null, ResponseCodeConstant.DATA_ALREADY_EXIST,"该项目下已存在同名的测试用例，请更换" );

        // 8.修改用例（只修改允许修改的字段）
        existCase.setProjectId(testCase.getProjectId());
        existCase.setCaseName(testCase.getCaseName());
        existCase.setCaseType(testCase.getCaseType());
        existCase.setCaseContent(testCase.getCaseContent());
        existCase.setStatus(testCase.getStatus());
        baseMapper.updateById(existCase);

        // 7.返回成功响应
        return Result.success("测试用例修改成功");
    }

    /**
     * 删除测试用例实现
     * @param id 用例ID
     * @return 返回成功响应
     */
    @Override
    @Transactional
    public Result<?> deleteTestCase(Long id) {
        // 1.参数校验
        AssertUtil.notNull(id,"用例ID不能为空");

        // 2.校验用例存在
        TestCase existCase = baseMapper.selectById(id);
        AssertUtil.notNull(existCase,"待删除的测试用例不存在");

        // 3.删除用例
        baseMapper.deleteById(id);

        // 4.返回成功响应
        return Result.success("测试用例删除成功");
    }

    /**
     * 根据ID查询测试用例实现
     * @param id 用例ID
     * @return 返回用例详情
     */
    @Override
    public Result<TestCase> getTestCaseById(Long id) {
        // 1.参数校验
        AssertUtil.notNull(id,"用例ID不能为空");

        // 2.查询并校验用例存在
        TestCase testCase = baseMapper.selectById(id);
        AssertUtil.notNull(testCase,"查询的测试用例不存在");

        // 3.返回用例详情
        return Result.success(testCase);
    }

    /**
     * 测试用例分页查询实现
     * @param pageNum 当前页码（默认1）
     * @param pageSize 每页条数（默认10）
     * @param projectId 关联项目ID（可为null，查询所有项目用例）
     * @param caseType 用例类型（可为null，查询所有项目用例）
     * @param caseName 用例名称（模糊查询，可为null）
     * @return 返回分页数据
     */
    @Override
    public Result<IPage<TestCase>> getTestCasePage(Integer pageNum, Integer pageSize, Long projectId, Integer caseType, String caseName) {

        // 1.处理分页
        if(pageNum==null || pageNum <1){
            pageNum = 1;
        }
        if(pageSize==null || pageSize <1 || pageSize > 100){
            pageSize = 10;
        }

        // 2.构建分页对象
        IPage<TestCase> page = new Page<>(pageNum,pageSize);

        // 3.构建查询条件
        LambdaQueryWrapper<TestCase> queryWrapper = new LambdaQueryWrapper<>();
        //按项目ID筛选（不为null时)
        if (projectId != null) {
            queryWrapper.eq(TestCase::getProjectId, projectId);
        }
        //按用例类型筛选
        if (caseType != null) {
            //先校验用例合法性
            try {
                CaseTypeEnum.getByCode(caseType);
            } catch (IllegalArgumentException e) {
                throw new BusinessException(ResponseCodeConstant.PARAM_ERROR,"用例类型无效");
            }
            queryWrapper.eq(TestCase::getCaseType, caseType);
        }

        //模糊查询用例名称（不为null且非空时）
        if (caseName != null && !caseName.trim().isEmpty()) {
            queryWrapper.like(TestCase::getCaseName, caseName.trim());
        }

        //按创建时间倒序排序
        queryWrapper.orderByDesc(TestCase::getCreateTime);

        // 4. 分页查询
        IPage<TestCase> casePage = baseMapper.selectPage(page, queryWrapper);

        // 5.返回分页数据
        return Result.success(casePage);
    }

    /**
     * 修改测试用例状态实现
     * @param id 用例ID
     * @param status 目标状态（0-禁用，1-启用）
     * @return 返回响应结果
     */
    @Override
    @Transactional
    public Result<?> updateTestCaseStatus(Long id, Integer status) {
        // 1.参数校验
        AssertUtil.notNull(id,"用例ID不能为空");
        AssertUtil.notNull(status,"用例状态不能为空");
        AssertUtil.isTrue(status == 0 || status == 1,"用例状态只能是启用（1）或禁用（0）" );

        // 2.校验用例存在
        TestCase existCase = baseMapper.selectById(id);
        AssertUtil.notNull(existCase,"待修改状态的测试用例不存在");

        // 3.校验状态是否一致
        if (existCase.getStatus().equals(status)) {
            return Result.success("测试用例当前已处于" + (status == 1 ? "启用":"禁用") + "状态，无需修改");
        }

        // 4.修改状态
        existCase.setStatus(status);
        baseMapper.updateById(existCase);
        // 5.返回成功响应
        return Result.success("测试用例状态已修改为" + (status == 1 ? "启用":"禁用"));
    }
}
