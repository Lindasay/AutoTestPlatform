package com.auto.test.platform.controller;

import com.auto.test.platform.common.result.Result;
import com.auto.test.platform.entity.ReportData;
import com.auto.test.platform.service.ReportDataService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 聚合报告控制器（查询聚合报告，展示所有用例结果）
 */
@Slf4j
@RestController
@RequestMapping("/reportData")
@Tag(name = "聚合报告管理", description = "查询聚合报告、项目最新报告、报告详情")
public class ReportDataController {

    @Autowired
    private ReportDataService reportDataService;

    // ========== 查询项目最新聚合报告 ==========
    @GetMapping("/latest/{projectId}")
    @Operation(summary = "查询项目最新聚合报告", description = "获取指定项目的最新批量执行聚合报告")
    public Result<?> getLatestByProjectId(@PathVariable Long projectId) {
        try {
            LambdaQueryWrapper<ReportData> queryWrapper = new LambdaQueryWrapper<ReportData>()
                    .eq(ReportData::getProjectId, projectId)
                    .orderByDesc(ReportData::getCreateTime)
                    .last("LIMIT 1");

            ReportData latestReport = reportDataService.getOne(queryWrapper);
            return Result.success(latestReport);
        } catch (Exception e) {
            log.error("查询项目最新报告失败，ID：{}", projectId, e);
            return Result.fail("查询失败：" + e.getMessage());
        }
    }

    // ========== 查询聚合报告详情（包含所有用例结果） ==========
    @GetMapping("/detail/{reportId}")
    @Operation(summary = "查询聚合报告详情", description = "获取指定报告的所有用例执行结果")
    public Result<?> getReportDetail(@PathVariable Long reportId) {
        try {
            ReportData reportData = reportDataService.getById(reportId);
            if (reportData == null) {
                return Result.fail("聚合报告不存在，ID：" + reportId);
            }
            // reportContent中存储了所有用例的执行详情（JSON数组），前端可直接解析展示
            return Result.success(reportData);
        } catch (Exception e) {
            log.error("查询报告详情失败，ID：{}", reportId, e);
            return Result.fail("查询失败：" + e.getMessage());
        }
    }

    // ========== 查询项目所有聚合报告 ==========
    @GetMapping("/list/{projectId}")
    @Operation(summary = "查询项目所有聚合报告", description = "按时间降序展示项目的所有批量执行报告")
    public Result<?> listByProjectId(@PathVariable Long projectId) {
        LambdaQueryWrapper<ReportData> queryWrapper = new LambdaQueryWrapper<ReportData>()
                .eq(ReportData::getProjectId, projectId)
                .orderByDesc(ReportData::getCreateTime);
        List<ReportData> reportList = reportDataService.list(queryWrapper);
        return Result.success(reportList);
    }
}