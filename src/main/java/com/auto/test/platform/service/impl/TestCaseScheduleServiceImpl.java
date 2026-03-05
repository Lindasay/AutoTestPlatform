package com.auto.test.platform.service.impl;

import com.auto.test.platform.entity.TestCaseSchedule;
import com.auto.test.platform.mapper.TestCaseScheduleMapper;
import com.auto.test.platform.service.TestCaseScheduleService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 测试用例定时任务层实现类
 */
@Service
public class TestCaseScheduleServiceImpl extends ServiceImpl<TestCaseScheduleMapper, TestCaseSchedule> implements TestCaseScheduleService {

    @Override
    public boolean addSchedule(TestCaseSchedule schedule) {
        schedule.setStatus(1); //默认启用
        return this.save(schedule);
    }

    @Override
    public boolean updateSchedule(TestCaseSchedule schedule) {
        return this.updateById(schedule);
    }

    @Override
    public boolean deleteSchedule(Long id) {
        return this.removeById(id);
    }

    @Override
    public List<TestCaseSchedule> getSchedulesByProjectId(Long projectId) {
        return this.lambdaQuery().eq(TestCaseSchedule::getProjectId, projectId).list();
    }

    @Override
    public List<TestCaseSchedule> getEnableSchedules() {
        return this.baseMapper.selectEnableSchedules();
    }
}
