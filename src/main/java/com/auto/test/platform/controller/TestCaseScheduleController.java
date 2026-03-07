package com.auto.test.platform.controller;

import com.auto.test.platform.common.result.Result;
import com.auto.test.platform.config.DynamicScheduleConfig;
import com.auto.test.platform.entity.TestCaseSchedule;
import com.auto.test.platform.service.TestCaseScheduleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 定时任务配置窗口
 */
@Slf4j
@RestController
@RequestMapping("/schedule")
@Tag(name = "定时任务配置")
public class TestCaseScheduleController {

    @Autowired
    private TestCaseScheduleService scheduleService;
    @Autowired
    private DynamicScheduleConfig dynamicScheduleConfig;

    /**
     * 新增定时任务
     */
    @PostMapping("/add")
    @Operation(summary = "新增定时任务")
    public Result<?> addSchedule(@Valid @RequestBody TestCaseSchedule schedule) {
        try {
            //设置默认值
            if (schedule.getStatus() == null) {
                schedule.setStatus(1); //默认启用
            }

            boolean result = scheduleService.addSchedule(schedule);
            if (result) {
                // 动态添加到定时任务
                try {
                    dynamicScheduleConfig.addScheduledTask(schedule);
                } catch (Exception e) {
                    log.warn("动态添加定时任务失败，但不影响持久化：{}",e.getMessage());
                }

                // 返回创建的数据
                Map<String, Object> response = new HashMap<>();
                response.put("id", schedule.getId());
                response.put("scheduleName", schedule.getScheduleName());
                response.put("projectId", schedule.getProjectId());
                response.put("cronExpression", schedule.getCronExpression());
                response.put("status", schedule.getStatus());

                return Result.success("新增定时任务成功");
            } else {
                return Result.fail("新增定时任务失败");
            }
        } catch (Exception e) {
            log.error("新增定时任务异常", e);
            return Result.fail("新增定时任务异常：" + e.getMessage());
        }
    }

    /**
     * 修改定时任务
     */
    @PutMapping("/update")
    @Operation(summary = "修改定时任务")
    public Result<?> updateSchedule(@RequestBody TestCaseSchedule schedule) {
        try {
            boolean result = scheduleService.updateSchedule(schedule);
            if (result) {
                //动态更新定时任务
                try {
                    dynamicScheduleConfig.addScheduledTask(schedule);
                } catch (Exception e) {
                    log.warn("动态更新定时任务失败：{}",e.getMessage());
                }
                return Result.success("修改定时任务成功");
            }else {
                return Result.fail("修改定时任务失败");
            }
        }catch (Exception e){
            log.error("修改定时任务异常", e);
            return Result.fail("修改定时任务异常：" + e.getMessage());
        }
    }

    /**
     * 删除定时任务
     */
    @DeleteMapping("/delete/{id}")
    @Operation(summary = "删除定时任务")
    public Result<?> deleteSchedule(@PathVariable("id") Long id) {
        try {
            boolean result = scheduleService.deleteSchedule(id);
            if (result) {
                //动态移除定时任务
                try {
                    dynamicScheduleConfig.removeScheduledTask(id);
                } catch (Exception e) {
                    log.warn("动态移除定时任务失败：{}", e.getMessage());
                }
                return Result.success("删除定时任务成功");
            }else {
                return Result.fail("删除定时任务失败");
            }
        }catch (Exception e){
            log.error("删除定时任务异常",e);
            return Result.fail("删除定时任务异常：" +  e.getMessage());
        }
    }

    /**
     * 查询项目下的定时任务
     */
    @GetMapping("/list/{projectId}")
    @Operation(summary = "查询项目下的定时任务")
    public Result<?> getSchedulesByProjectId(@PathVariable("projectId") Long projectId) {
        try {
            List<TestCaseSchedule> list = scheduleService.getSchedulesByProjectId(projectId);
            return Result.success(list);
        }catch (Exception e){
            log.error("查询定时任务异常",e);
            return Result.fail("查询定时任务异常：" + e.getMessage());
        }
    }

    /**
     * 查询所有启用的定时任务
     */
    @GetMapping("/getEnableList")
    @Operation(summary = "查询所有定时任务")
    public Result<?> getEnableSchedules() {
        try {
            List<TestCaseSchedule> enableSchedules = scheduleService.getEnableSchedules();
            return Result.success(enableSchedules);
        } catch (Exception e) {
            log.error("查询所有启用的定时任务异常：", e);
            return Result.fail("查询所有启用的定时任务异常：" +  e.getMessage());
        }
    }

    /**
     * 开关定时任务状态
     */
    @PutMapping("/toggle/{id}")
    @Operation(summary = "开关定时任务状态")
    public Result<?> toggleScheduleStatus(@PathVariable("id") Long id) {
        try {
            Integer newStatus = scheduleService.toggleScheduleStatus(id);
            if (newStatus != null) {
                Map<String, Object> response = new HashMap<>();
                response.put("id", id);
                response.put("newStatus", newStatus);
                response.put("statusText", newStatus == 1 ? "启用" : "禁用");

                return Result.success(response, "定时任务状态切换成功");
            } else {
                return Result.fail("定时任务状态切换失败");
            }
        } catch (Exception e) {
            log.error("开关定时任务状态异常", e);
            return Result.fail("开关定时任务状态异常：" + e.getMessage());
        }
    }

    /**
     * 验证Cron表达式
     */
    @PostMapping("/validateCron")
    @Operation(summary = "验证Cron表达式")
    public Result<?> validateCronExpression(@RequestBody Map<String, String> request) {
        try {
            String cronExpression = request.get("cronExpression");
            if (cronExpression == null || cronExpression.trim().isEmpty()) {
                return Result.fail("Cron表达式不能为空");
            }

            boolean isValid = scheduleService.validateCronExpression(cronExpression);

            Map<String, Object> response = new HashMap<>();
            response.put("cronExpression", cronExpression);
            response.put("isValid", isValid);
            response.put("message", isValid ? "Cron表达式有效" : "Cron表达式无效");

            return Result.success(response);
        } catch (Exception e) {
            log.error("验证Cron表达式异常", e);
            return Result.fail("验证Cron表达式异常：" + e.getMessage());
        }
    }
}
