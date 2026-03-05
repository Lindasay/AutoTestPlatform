package com.auto.test.platform.service.impl;

import com.alibaba.fastjson2.JSON;
import com.auto.test.platform.common.exception.BusinessException;
import com.auto.test.platform.common.result.Result;
import com.auto.test.platform.engine.ApiExecuteEngine;
import com.auto.test.platform.entity.ReportData;
import com.auto.test.platform.entity.TaskExecution;
import com.auto.test.platform.entity.TestCase;
import com.auto.test.platform.mapper.TaskExecutionMapper;
import com.auto.test.platform.mapper.TestCaseMapper;
import com.auto.test.platform.service.ReportDataService;
import com.auto.test.platform.service.TaskExecutionService;
import com.auto.test.platform.service.TestCaseService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.management.RuntimeErrorException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 任务执行服务实现（抽离Controller的执行逻辑，供定时任务复用）
 */
@Slf4j
@Service
public class TaskExecutionServiceImpl extends ServiceImpl<TaskExecutionMapper,TaskExecution> implements TaskExecutionService {

    @Autowired
    private TestCaseService testCaseService;
    @Autowired
    private ApiExecuteEngine apiExecuteEngine;
    @Autowired
    private ReportDataService reportDataService;


    /**
     * 执行单条用例
     * @param caseId 用例ID
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReportData executeSingleCase(Long caseId) {

        // 1. 验证用例存在
        TestCase testCase = testCaseService.getById(caseId);
        if (testCase == null) {
            throw new RuntimeException("用例不存在，ID：" + caseId);
        }

        if (!testCase.getCaseType().equals(1)) {
            throw new RuntimeException("该用例不是接口用例（case_type=1），无法执行");
        }

        if (testCase.getStatus() != null && testCase.getStatus() == 0) {
            throw new RuntimeException("该用例已被禁用，请启用后执行");
        }

        // 2. 记录任务执行
        TaskExecution task = new TaskExecution();
        task.setProjectId(testCase.getProjectId());
        task.setTaskName("单例执行-" + testCase.getCaseName() + "-" + System.currentTimeMillis());
        task.setTaskStatus(1);   // 1-执行中
        task.setExecuteTime(LocalDateTime.now());
        this.save(task);
        log.info("任务执行记录创建成功，任务ID: {}", task.getId());

        // 3. 执行单条用例
        ApiExecuteEngine.ExecuteResult singleResult = apiExecuteEngine.execute(testCase);
        log.info("用例执行完成，用例ID: {}, 结果: {}", caseId, singleResult.getSuccess());

        // 4. 更新任务状态
        task.setTaskStatus(singleResult.getSuccess() ? 2 : 3); // 2-成功, 3-失败
        task.setExecuteDuration(singleResult.getExecuteDuration());
        this.updateById(task);

        // 5. 保存报告数据
        ReportData reportData = buildReportDataForSingle(singleResult, testCase.getProjectId(), task.getId());
        reportDataService.save(reportData);
        log.info("报告数据保存成功，报告ID: {}", reportData.getId());
        return reportData;
    }

    /**
     * 批量执行项目下的所有用例（定时任务核心调用）
     * @param projectId 项目ID
     * @return 聚合报告
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReportData executeBatchByProjectId(Long projectId) {

        // 1. 查询该项目下所有接口用例
        LambdaQueryWrapper<TestCase> queryWrapper = new LambdaQueryWrapper<TestCase>()
                .eq(TestCase::getProjectId, projectId)
                .eq(TestCase::getCaseType, 1)  // 仅执行接口用例
                .eq(TestCase::getStatus, 1);   // 只执行启用状态的用例
        
        List<TestCase> testCases = testCaseService.list(queryWrapper);
        log.info("查询到项目下接口用例数: {}", testCases.size());
        
        if (testCases.isEmpty()) {
            throw new RuntimeException("该项目下无启用的接口用例（case_type=1, status=1）");
        }

        // 2. 记录批量执行任务
        TaskExecution task = new TaskExecution();
        task.setProjectId(projectId);
        task.setTaskName("批量执行-" + testCases.size() + "条用例-" + System.currentTimeMillis());
        task.setTaskStatus(1); // 1-执行中
        task.setExecuteTime(LocalDateTime.now());
        this.save(task);
        log.info("批量执行任务创建成功，任务ID: {}, 用例数: {}", task.getId(), testCases.size());

        // 3. 批量执行用例
        ApiExecuteEngine.BatchExecuteResult batchResult = apiExecuteEngine.executeBatch(testCases);
        log.info("批量执行完成，成功: {}, 失败: {}, 通过率: {}%",
                batchResult.getPassCount(), batchResult.getFailCount(),
                batchResult.getPassRate() != null ? batchResult.getPassRate().toString() : "0.00");

        // 4. 更新任务状态
        boolean allSuccess = batchResult.getFailCount() == 0;
        task.setTaskStatus(allSuccess ? 2 : 3); // 2-成功, 3-失败
        task.setExecuteDuration(batchResult.getTotalDuration());
        this.updateById(task);

        // 5. 保存聚合报告
        ReportData reportData = buildReportDataForBatch(batchResult, projectId, task.getId());
        reportDataService.save(reportData);
        log.info("聚合报告保存成功，报告ID: {}", reportData.getId());
        
        return reportData;
    }

    /**
     * 构建单条用例报告数据
     */
    private ReportData buildReportDataForSingle(ApiExecuteEngine.ExecuteResult singleResult, Long projectId, Long taskId) {
        ReportData reportData = new ReportData();
        // 这里复制你Controller里该方法的所有逻辑
        reportData.setProjectId(projectId);
        reportData.setTaskId(taskId);
        reportData.setTotalCount(1);
        reportData.setPassCount(singleResult.getSuccess() ? 1 : 0);
        reportData.setFailCount(singleResult.getSuccess() ? 0 : 1);
        reportData.setPassRate(BigDecimal.valueOf(singleResult.getSuccess() ? 100.0 : 0.0));
        reportData.setReportContent(buildSingleReportContent(singleResult));
        reportData.setCreateTime(LocalDateTime.now());
        reportData.setExtentReportPath(singleResult.getExtentReportPath());
        return reportData;
    }

    /**
     * 构建用例批量执行报告数据
     * @param batchResult
     * @param projectId
     * @param taskId
     * @return
     */
    private ReportData buildReportDataForBatch(ApiExecuteEngine.BatchExecuteResult batchResult, Long projectId, Long taskId) {
        ReportData reportData = new ReportData();
        // 这里复制你Controller里该方法的所有逻辑
        reportData.setProjectId(projectId);
        reportData.setTaskId(taskId);
        reportData.setTotalCount(batchResult.getTotalCount());
        reportData.setPassCount(batchResult.getPassCount());
        reportData.setFailCount(batchResult.getFailCount());
        reportData.setPassRate(batchResult.getPassRate());
        reportData.setReportContent(buildBatchReportContent(batchResult));
        reportData.setCreateTime(LocalDateTime.now());
        reportData.setExtentReportPath(batchResult.getExtentReportPath());
        return reportData;
    }

    private String buildSingleReportContent(ApiExecuteEngine.ExecuteResult singleResult) {
        // 构建详细的报告内容
        Map<String, Object> content= new HashMap<>();
        content.put("caseId", singleResult.getCaseId());
        content.put("caseName", singleResult.getCaseName());
        content.put("executeTime", singleResult.getExecuteTime());
        content.put("duration", singleResult.getExecuteDuration());
        content.put("success", singleResult.getSuccess());
        content.put("errorMsg", singleResult.getErrorMsg());
        content.put("responseData", singleResult.getResponseData());
        content.put("extentReportPath", singleResult.getExtentReportPath());
        return JSON.toJSONString(content);
    }

    private String buildBatchReportContent(ApiExecuteEngine.BatchExecuteResult batchResult) {
        // 通过率
        BigDecimal passRate = batchResult.getPassRate() != null ?
                batchResult.getPassRate().setScale(2, RoundingMode.HALF_UP) :
                new BigDecimal("0.00").setScale(2, RoundingMode.HALF_UP);

        // 构建详细的报告内容
        Map<String, Object> content = new HashMap<>();
        content.put("executeTime", batchResult.getExecuteTime());
        content.put("totalDuration", batchResult.getTotalDuration());
        content.put("averageDuration", batchResult.getAverageDuration());
        content.put("passRate", passRate);
        content.put("totalCount", batchResult.getTotalCount());
        content.put("passCount", batchResult.getPassCount());
        content.put("failCount", batchResult.getFailCount());
        content.put("extentReportPath", batchResult.getExtentReportPath());
        content.put("caseResults", batchResult.getCaseResults());
        return JSON.toJSONString(content);
    }
}
