package com.auto.test.platform.service;

import com.auto.test.platform.entity.TestCaseSchedule;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
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
     * 查询项目下的所有定时任务
     */
    List<TestCaseSchedule> getSchedulesByProjectId(Long projectId);

    /**
     * 查询所有启用的定时任务
     */
    List<TestCaseSchedule> getEnableSchedules();


}
