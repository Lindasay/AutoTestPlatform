package com.auto.test.platform.controller;

import com.auto.test.platform.common.result.Result;
import com.auto.test.platform.entity.ReportData;
import com.auto.test.platform.entity.TaskExecution;
import com.auto.test.platform.entity.TestCase;
import com.auto.test.platform.service.ReportDataService;
import com.auto.test.platform.service.TaskExecutionService;
import com.auto.test.platform.service.TestCaseService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 测试用例管理器（表现层，接口开发）
 * 遵循企业级接口规范，与ProjectController风格统一，调用TestCaseServie实现业务逻辑
 * 接口文档由Knife4j自动生成，支持接口调试、参数校验提示
 */
@Slf4j
@RestController
@RequestMapping("/testCase")
@Tag(name = "测试用例管理", description = "测试用例增删改查及执行接口")
public class TestCaseController {

    //注入测试用例Service，调用业务逻辑层方法（依赖注入，Spring容器自动管理）
    @Resource
    private TestCaseService testCaseService;

    @Resource
    private ReportDataService reportDataService;

    @Resource
    private TaskExecutionService taskExecutionService;


    /**
     * 新增测试用例
     * @param testCase 请求参数
     * @return testCase
     */
    @PostMapping("/add")
    @Operation(summary = "新增测试用例", description = "测试用例信息（JSON格式，需包含项目ID、用例名称等核心字段）")
    public Result<?> addTestCase(@RequestBody TestCase testCase) {
        return testCaseService.addTestCase(testCase);
    }

    /**
     * 修改测试用例
     * @param testCase 请求参数
     * @return testCase
     */
    @PutMapping("/update") // PUT请求，适配修改操作，与新增接口路径区分
    @Operation(summary = "修改测试用例", description = "传入用例ID及修改后的信息，完成测试用例修改，校验用例存在、唯一")
    public Result<?> updateTestCase(@RequestBody TestCase testCase) {
        return testCaseService.updateTestCase(testCase);
    }

    /**
     * 删除测试用例
     * @param id 请求参数
     * @return 返回删除确认信息
     */
    @DeleteMapping("/delete/{id}")
    @Operation(summary = "删除测试用例", description = "传入用例ID，删除用例")
    public Result<?> deleteTestCase(@Parameter(name = "id", description = "测试用例ID",required = true)
                                    @PathVariable Long id) {
        return testCaseService.deleteTestCase(id);
    }

    /**
     * 根据id查询测试用例
     * @param id
     * @return
     */
    @GetMapping("/get/{id}")
    @Operation(summary = "根据用例ID查询测试用例", description = "传入用例ID，查询测试用例，校验测试用例存在")
    public  Result<TestCase> getTestCaseById(@Parameter(name = "id", description = "测试用例ID",required = true)
                                             @PathVariable Long id) {
        return testCaseService.getTestCaseById(id);
    }


    /**
     * 分页查询测试用例
     */
    @GetMapping("/page")
    @Operation(summary = "测试用例分页查询", description = "传入页码、每页条数、项目ID、用例类型、用例名称，完成分页模糊查询")
    public Result<IPage<TestCase>> getTestCaseByPage(
            @Parameter(name = "pageNum", description = "当前页码，默认1", required = false)
            @RequestParam(required = false, defaultValue = "1", value = "page") Integer pageNum,
            @Parameter(name = "pageSize", description = "每页条数，默认10，最大100", required = false)
            @RequestParam(required = false, defaultValue = "10", value = "size") Integer pageSize,
            @Parameter(name = "projectId", description = "关联项目ID", required = false)
            @RequestParam(required = false) Long projectId,
            @Parameter(name = "caseType", description = "用例类型（可选，1-接口用例，2-UI用例）", required = false)
            @RequestParam(required = false) Integer caseType,
            @Parameter(name = "caseName", description = "用例名称（可选，模糊查询）", required = false)
            @RequestParam(required = false) String caseName) {

        // 参数校验
        if (pageSize > 100) {
            pageSize = 100;
        }

        return testCaseService.getTestCasePage(pageNum, pageSize, projectId, caseType, caseName);
    }


    /**
     * 修改测试用例状态
     * @param id 用例ID
     * @param status 目标状态
     * @return
     */
    @PutMapping("/update-status/{id}/{status}")
    @Operation(summary = "修改测试用例状态",description = "传入测试用例ID、目标状态（0-禁用，1-启用），修改用例状态")
    public Result<?> updateTestCaseStatus(@Parameter(name = "id",description = "测试用例ID",required = true)
                                          @PathVariable Long id,
                                          @Parameter(name = "status",description = "目标状态（0-禁用，1-启用）",required = true)
                                          @PathVariable Integer status){
        return testCaseService.updateTestCaseStatus(id,status);
    }

    /**
     * 执行接口用例（单条执行）
     */
    @PostMapping("/execute/{id}")
    @Operation(summary = "执行接口用例", description = "根据用例ID执行接口用例，记录任务和报告，生成Allure/ExtentReports报告")
    @Transactional(rollbackFor = Exception.class)
    public Result<?> executeSingle(@PathVariable Long id) {
        try {
            log.info("开始执行单条用例，ID: {}", id);

            // 1. 调用Service执行用例并获取报告
            ReportData reportData = taskExecutionService.executeSingleCase(id);
            TaskExecution task = taskExecutionService.getById(reportData.getTaskId());
            // 2. 封装返回结果
            Map<String, Object> result = new HashMap<>();
            result.put("task", task);
            result.put("report", reportData);
            result.put("executeResult", reportData.getReportContent());
            result.put("extentReportPath", reportData.getExtentReportPath());

            return Result.success(result, "用例执行完成");
        } catch (Exception e) {
            log.error("执行单条用例失败，ID: {}", id, e);
            return Result.fail("执行失败：" + e.getMessage());
        }
    }

    /**
     * 批量执行接口用例（按项目执行所有用例）
     */
    @PostMapping("/executeBatch/{projectId}")
    @Operation(summary = "批量执行项目下所有用例", description = "执行指定项目的所有接口用例，生成聚合报告")
    @Transactional(rollbackFor = Exception.class)
    public Result<?> executeBatch(@PathVariable Long projectId) {
        try {
            log.info("开始批量执行项目用例，项目ID: {}", projectId);

            // 1.获取报告数据
            ReportData reportData = taskExecutionService.executeBatchByProjectId(projectId);
            TaskExecution task = taskExecutionService.getById(reportData.getTaskId());
            // 2. 封装返回结果
            Map<String, Object> result = new HashMap<>();
            result.put("task", task);
            result.put("report", reportData);
            result.put("executeResult", reportData.getReportContent());
            result.put("extentReportPath", reportData.getExtentReportPath());

            return Result.success(result, "批量执行完成");
        } catch (Exception e) {
            log.error("批量执行用例失败，项目ID: {}", projectId, e);
            return Result.fail("批量执行失败：" + e.getMessage());
        }
    }

    /**
     * 查询任务执行历史
     */
    @GetMapping("/executionHistory/{projectId}")
    @Operation(summary = "查询任务执行历史", description = "查询指定项目的任务执行历史记录")
    public Result<List<TaskExecution>> getExecutionHistory(@PathVariable Long projectId,
                                                           @RequestParam(required = false, defaultValue = "10") Integer limit) {
        try {
            // 使用安全的分页查询
            Page<TaskExecution> page = new Page<>(1, limit);
            LambdaQueryWrapper<TaskExecution> queryWrapper = new LambdaQueryWrapper<TaskExecution>()
                    .eq(TaskExecution::getProjectId, projectId)
                    .orderByDesc(TaskExecution::getExecuteTime);

            IPage<TaskExecution> pageResult = taskExecutionService.page(page, queryWrapper);
            return Result.success(pageResult.getRecords());
        } catch (Exception e) {
            log.error("查询执行历史失败，项目ID: {}", projectId, e);
            return Result.fail("查询失败：" + e.getMessage());
        }
    }

    /**
     * 获取任务详情
     */
    @GetMapping("/taskDetail/{taskId}")
    @Operation(summary = "获取任务详情", description = "根据任务ID获取任务详情")
    public Result<TaskExecution> getTaskDetail(@PathVariable Long taskId) {
        try {
            TaskExecution task = taskExecutionService.getById(taskId);
            if (task == null) {
                return Result.fail("任务不存在");
            }
            return Result.success(task);
        } catch (Exception e) {
            log.error("获取任务详情失败，任务ID: {}", taskId, e);
            return Result.fail("获取任务详情失败：" + e.getMessage());
        }
    }

    /**
     * 获取报告详情
     */
    @GetMapping("/reportDetail/{taskId}")
    @Operation(summary = "获取任务报告详情", description = "根据任务ID获取详细的执行报告")
    public Result<ReportData> getReportDetail(@PathVariable Long taskId) {
        try {
            LambdaQueryWrapper<ReportData> queryWrapper = new LambdaQueryWrapper<ReportData>()
                    .eq(ReportData::getTaskId, taskId)
                    .orderByDesc(ReportData::getCreateTime)
                    .last("LIMIT 1");

            ReportData reportData = reportDataService.getOne(queryWrapper);
            if (reportData == null) {
                return Result.fail("该任务无报告数据");
            }
            return Result.success(reportData);
        } catch (Exception e) {
            log.error("获取报告详情失败，任务ID: {}", taskId, e);
            return Result.fail("获取报告详情失败：" + e.getMessage());
        }
    }

    /**
     * 查询最新的用例执行报告
     */
    @GetMapping("/getLatestByCaseId")
    @Operation(summary = "根据用例ID查询最新报告", description = "查询指定用例的最新执行报告")
    public Result<Map<String, Object>> getLatestByCaseId(@RequestParam("caseId") Long caseId) {
        try {
            // 1. 验证用例存在
            TestCase testCase = testCaseService.getById(caseId);
            if (testCase == null) {
                return Result.fail("用例不存在");
            }

            // 2. 查询包含该用例的最新任务
            LambdaQueryWrapper<TaskExecution> taskQuery = new LambdaQueryWrapper<TaskExecution>()
                    .eq(TaskExecution::getProjectId, testCase.getProjectId())
                    .in(TaskExecution::getTaskStatus, Arrays.asList(2, 3)) // 只查询已完成的任务
                    .orderByDesc(TaskExecution::getExecuteTime);

            List<TaskExecution> tasks = taskExecutionService.list(taskQuery);

            // 3. 查找对应任务的报告
            for (TaskExecution task : tasks) {
                LambdaQueryWrapper<ReportData> reportQuery = new LambdaQueryWrapper<ReportData>()
                        .eq(ReportData::getTaskId, task.getId())
                        .last("LIMIT 1");

                ReportData reportData = reportDataService.getOne(reportQuery);
                if (reportData != null) {
                    try {
                        // 解析报告内容，检查是否包含该用例
                        String reportContent = reportData.getReportContent();
                        if (reportContent != null && reportContent.contains("\"caseId\":" + caseId)) {
                            Map<String, Object> result = new HashMap<>();
                            result.put("caseInfo", testCase);
                            result.put("taskInfo", task);
                            result.put("reportInfo", reportData);
                            return Result.success(result);
                        }
                    } catch (Exception e) {
                        log.warn("解析报告内容失败，任务ID: {}", task.getId(), e);
                    }
                }
            }

            return Result.success(null, "该用例暂无执行记录");
        } catch (Exception e) {
            log.error("查询用例最新报告失败，caseId: {}", caseId, e);
            return Result.fail("查询失败：" + e.getMessage());
        }
    }
}
