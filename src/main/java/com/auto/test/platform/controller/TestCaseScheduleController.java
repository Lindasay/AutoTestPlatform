package com.auto.test.platform.controller;

import com.auto.test.platform.common.result.Result;
import com.auto.test.platform.config.DynamicScheduleConfig;
import com.auto.test.platform.entity.TestCaseSchedule;
import com.auto.test.platform.service.TestCaseScheduleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    public Result<?> addSchedule(@RequestBody TestCaseSchedule schedule) {
        try {
            boolean result = scheduleService.addSchedule(schedule);
            if (result) {
                // 动态添加到定时任务
                dynamicScheduleConfig.addScheduledTask(schedule);
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
                dynamicScheduleConfig.addScheduledTask(schedule);
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
             dynamicScheduleConfig.removeScheduledTask(id);
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
    public Result<?> getAllSchedules() {
        try {
            List<TestCaseSchedule> enableSchedules = scheduleService.getEnableSchedules();
            return Result.success(enableSchedules);
        } catch (Exception e) {
            log.error("查询所有启用的定时任务异常：", e);
            return Result.fail("查询所有启用的定时任务异常：" +  e.getMessage());
        }
    }

}
