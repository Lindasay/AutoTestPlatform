package com.auto.test.platform.mapper;

import com.auto.test.platform.entity.TestCaseSchedule;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import java.util.List;

/**
 * 定时任务Mapper接口
 * 继承BaseMapper，获得MyBatis-Plus提供的CRUD方法（无需手动编写SQL）
 */
public interface TestCaseScheduleMapper extends BaseMapper<TestCaseSchedule> {
    // 查询所有启用的定时任务
    List<TestCaseSchedule> selectEnableSchedules();
}
