package com.auto.test.platform.service;

import com.auto.test.platform.common.result.Result;
import com.auto.test.platform.entity.TestCase;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 测试用例管理Service接口
 */
public interface TestCaseService extends IService<TestCase> {

    /**
     * 新增测试用例（校验：项目存在、用例）
     * @param testCase 测试用例实体
     * @return 统一响应结果
     */
    Result<?> addTestCase(TestCase testCase);

    /**
     * 修改测试用例（校验：用例存在、项目存在、用例类型合法）
     * @param testCase 测试用例实体
     * @return 统一响应结果
     */
    Result<?> updateTestCase(TestCase testCase);

    /**
     * 删除测试用例（校验：用例存在）
     * @param id 用例ID
     * @return 统一响应结果
     */
    Result<?> deleteTestCase(Long id);

    /**
     * 根据ID查询测试用例（用例存在）
     * @param id 用例ID
     * @return 统一响应结果（包含用例详情）
     */
    Result<TestCase> getTestCaseById(Long id);

    /**
     * 测试用例分页查询（支持按项目ID、用例类型筛选，模糊查询用例名称）
     * @param pageNum 当前页码（默认1）
     * @param pageSize 每页条数（默认10）
     * @param projectId 关联项目ID（可为null，查询所有项目用例）
     * @param caseType 用例类型（可为null，查询所有项目用例）
     * @param caseName 用例名称（模糊查询，可为null）
     * @return 统一响应结果（包含分页数据）
     */
    Result<IPage<TestCase>> getTestCasePage(Integer pageNum, Integer pageSize, Long projectId, Integer caseType, String caseName);

    /**
     * 修改测试用例状态（启用/禁用，校验：用例存在）
     * @param id 用例ID
     * @param status 目标状态（0-禁用，1-启用）
     * @return 统一响应结果
     */
    Result<?> updateTestCaseStatus(Long id, Integer status);
}

