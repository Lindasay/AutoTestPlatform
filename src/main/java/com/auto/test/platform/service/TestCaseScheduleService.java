package com.auto.test.platform.service;

import com.auto.test.platform.entity.TestCaseSchedule;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Map;

/**
 * 测试用例定时任务Service接口（业务逻辑层，架构Service层核心）
 * 继承IService。获得MyBatis-Plus提供的分页、批量操作等增强方法
 */
public interface TestCaseScheduleService extends IService<TestCaseSchedule> {

    /**
     * 新增定时任务
     */
    boolean addSchedule(TestCaseSchedule schedule);

    /**
     * 修改定时任务
     */
    boolean updateSchedule(TestCaseSchedule schedule);

    /**
     * 删除定时任务
     */
    boolean deleteSchedule(Long id);

    /**
     * 根据ID查询定时任务
     * @param id 定时任务ID
     * @return 定时任务实体
     */
    TestCaseSchedule getScheduleById(Long id);

    /**
     * 查询项目下的所有定时任务
     */
    List<TestCaseSchedule> getSchedulesByProjectId(Long projectId);

    /**
     * 查询所有启用的定时任务
     */
    List<TestCaseSchedule> getEnableSchedules();

    /**
     * 查询所有定时任务
     * @return 所有定时任务列表
     */
    List<TestCaseSchedule> getAllSchedules();

    /**
     * 批量创建定时任务
     * @param schedules 定时任务列表
     * @return 创建的定时任务列表
     */
    List<TestCaseSchedule> batchCreateSchedules(List<TestCaseSchedule> schedules);

    /**
     * 批量删除定时任务
     * @param ids 定时任务ID列表
     * @return 删除的数量
     */
    int batchDeleteSchedules(List<Long> ids);

    /**
     * 保存或更新定时任务
     * @param schedule 定时任务实体
     * @return 是否成功
     */
    boolean saveOrUpdate(TestCaseSchedule schedule);

    /**
     * 获取定时任务统计信息
     * @param projectId 项目ID
     * @return 统计信息
     */
    Map<String, Object> getScheduleStats(Long projectId);

    /**
     * 搜索定时任务
     * @param keyword 关键词
     * @param projectId 项目ID
     * @param status 状态
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @return 搜索结果的定时任务列表
     */
    List<TestCaseSchedule> searchSchedules(String keyword, Long projectId, Integer status, Integer pageNum, Integer pageSize);

    /**
     * 统计定时任务数量
     * @param keyword 关键词
     * @param projectId 项目ID
     * @param status 状态
     * @return 数量
     */
    int countSchedules(String keyword, Long projectId, Integer status);

    /**
     * 切换定时任务状态
     * @param id 定时任务ID
     * @return 切换后的状态
     */
    Integer toggleScheduleStatus(Long id);

    /**
     * 验证Cron表达式
     * @param cronExpression Cron表达式
     * @return 是否有效
     */
    boolean validateCronExpression(String cronExpression);

    /**
     * 根据项目ID和Cron表达式查询定时任务
     * @param projectId 项目ID
     * @param cronExpression Cron表达式
     * @return 定时任务列表
     */
    List<TestCaseSchedule> getSchedulesByProjectAndCron(Long projectId, String cronExpression);

    /**
     * 根据任务名称查询定时任务
     * @param scheduleName 任务名称
     * @return 定时任务列表
     */
    List<TestCaseSchedule> getSchedulesByName(String scheduleName);

    /**
     * 根据项目ID批量启用/禁用定时任务
     * @param projectId 项目ID
     * @param status 状态
     * @return 影响的数量
     */
    int batchToggleByProjectId(Long projectId, Integer status);

    /**
     * 获取即将执行的定时任务
     * @param minutes 未来几分钟内
     * @return 即将执行的定时任务列表
     */
    List<TestCaseSchedule> getUpcomingSchedules(int minutes);

    /**
     * 检查任务名称是否已存在
     * @param scheduleName 任务名称
     * @param excludeId 排除的ID
     * @return 是否存在
     */
    boolean isScheduleNameExists(String scheduleName, Long excludeId);

    /**
     * 检查Cron表达式是否被使用
     * @param cronExpression Cron表达式
     * @param excludeId 排除的ID
     * @return 是否被使用
     */
    boolean isCronExpressionUsed(String cronExpression, Long excludeId);


}
