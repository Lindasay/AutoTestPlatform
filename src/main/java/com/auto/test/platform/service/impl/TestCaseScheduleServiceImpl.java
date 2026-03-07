package com.auto.test.platform.service.impl;

import com.auto.test.platform.entity.TestCaseSchedule;
import com.auto.test.platform.mapper.TestCaseScheduleMapper;
import com.auto.test.platform.service.TestCaseScheduleService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class TestCaseScheduleServiceImpl extends ServiceImpl<TestCaseScheduleMapper, TestCaseSchedule>
        implements TestCaseScheduleService {


    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public boolean addSchedule(TestCaseSchedule schedule) {
        try {
            if (schedule == null) {
                throw new IllegalArgumentException("定时任务不能为空");
            }

            schedule.setCreateTime(LocalDateTime.now());
            schedule.setUpdateTime(LocalDateTime.now());

            // 验证任务名称是否已存在
            if (isScheduleNameExists(schedule.getScheduleName(), null)) {
                throw new RuntimeException("任务名称已存在: " + schedule.getScheduleName());
            }

            // 验证Cron表达式
            if (!validateCronExpression(schedule.getCronExpression())) {
                throw new RuntimeException("Cron表达式无效: " + schedule.getCronExpression());
            }

            return this.save(schedule);
        } catch (Exception e) {
            log.error("新增定时任务失败", e);
            throw new RuntimeException("新增定时任务失败: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean updateSchedule(TestCaseSchedule schedule) {
        try {
            if (schedule == null || schedule.getId() == null) {
                throw new IllegalArgumentException("定时任务ID不能为空");
            }

            // 验证任务是否存在
            TestCaseSchedule existingSchedule = this.getById(schedule.getId());
            if (existingSchedule == null) {
                throw new RuntimeException("定时任务不存在: " + schedule.getId());
            }

            // 验证任务名称是否已存在（排除自己）
            if (isScheduleNameExists(schedule.getScheduleName(), schedule.getId())) {
                throw new RuntimeException("任务名称已存在: " + schedule.getScheduleName());
            }

            // 验证Cron表达式
            if (!validateCronExpression(schedule.getCronExpression())) {
                throw new RuntimeException("Cron表达式无效: " + schedule.getCronExpression());
            }

            // 设置更新时间
            schedule.setUpdateTime(LocalDateTime.now());

            return this.updateById(schedule);
        } catch (Exception e) {
            log.error("修改定时任务失败", e);
            throw new RuntimeException("修改定时任务失败: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean deleteSchedule(Long id) {
        try {
            if (id == null) {
                throw new IllegalArgumentException("定时任务ID不能为空");
            }

            // 验证任务是否存在
            TestCaseSchedule existingSchedule = this.getById(id);
            if (existingSchedule == null) {
                log.warn("定时任务不存在: {}", id);
                return false;
            }

            return this.removeById(id);
        } catch (Exception e) {
            log.error("删除定时任务失败", e);
            throw new RuntimeException("删除定时任务失败: " + e.getMessage(), e);
        }
    }

    @Override
    public TestCaseSchedule getScheduleById(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("定时任务ID不能为空");
        }
        return this.getById(id);
    }

    @Override
    public List<TestCaseSchedule> getSchedulesByProjectId(Long projectId) {
        if (projectId == null) {
            throw new IllegalArgumentException("项目ID不能为空");
        }

        LambdaQueryWrapper<TestCaseSchedule> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TestCaseSchedule::getProjectId, projectId)
                .orderByDesc(TestCaseSchedule::getCreateTime);

        return this.list(queryWrapper);
    }

    @Override
    public List<TestCaseSchedule> getEnableSchedules() {
        LambdaQueryWrapper<TestCaseSchedule> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TestCaseSchedule::getStatus, 1)
                .orderByAsc(TestCaseSchedule::getCreateTime);

        return this.list(queryWrapper);
    }

    @Override
    public List<TestCaseSchedule> getAllSchedules() {
        LambdaQueryWrapper<TestCaseSchedule> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.orderByDesc(TestCaseSchedule::getCreateTime);
        return this.list(queryWrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<TestCaseSchedule> batchCreateSchedules(List<TestCaseSchedule> schedules) {
        if (schedules == null || schedules.isEmpty()) {
            return Collections.emptyList();
        }

        // 验证每个定时任务
        for (TestCaseSchedule schedule : schedules) {
            if (!StringUtils.hasText(schedule.getScheduleName())) {
                throw new RuntimeException("任务名称不能为空");
            }
            if (schedule.getProjectId() == null) {
                throw new RuntimeException("项目ID不能为空");
            }
            if (!StringUtils.hasText(schedule.getCronExpression())) {
                throw new RuntimeException("Cron表达式不能为空");
            }
            if (!validateCronExpression(schedule.getCronExpression())) {
                throw new RuntimeException("Cron表达式无效: " + schedule.getCronExpression());
            }

            // 设置默认值
            if (schedule.getStatus() == null) {
                schedule.setStatus(1);
            }
            schedule.setCreateTime(LocalDateTime.now());
            schedule.setUpdateTime(LocalDateTime.now());
        }

        // 检查重复的任务名称
        Map<String, Long> nameCountMap = new HashMap<>();
        for (TestCaseSchedule schedule : schedules) {
            String scheduleName = schedule.getScheduleName();
            nameCountMap.put(scheduleName, nameCountMap.getOrDefault(scheduleName, 0L) + 1);
        }

        for (Map.Entry<String, Long> entry : nameCountMap.entrySet()) {
            if (entry.getValue() > 1) {
                throw new RuntimeException("存在重复的任务名称: " + entry.getKey());
            }
            if (isScheduleNameExists(entry.getKey(), null)) {
                throw new RuntimeException("任务名称已存在: " + entry.getKey());
            }
        }

        // 批量保存
        boolean result = this.saveBatch(schedules);
        if (!result) {
            throw new RuntimeException("批量创建定时任务失败");
        }

        return schedules;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchDeleteSchedules(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }

        // 检查所有ID是否存在
        List<TestCaseSchedule> existingSchedules = this.listByIds(ids);
        if (existingSchedules.size() != ids.size()) {
            // 找出不存在的ID
            Set<Long> existingIds = existingSchedules.stream()
                    .map(TestCaseSchedule::getId)
                    .collect(Collectors.toSet());
            List<Long> missingIds = ids.stream()
                    .filter(id -> !existingIds.contains(id))
                    .collect(Collectors.toList());
            log.warn("以下定时任务ID不存在: {}", missingIds);
        }

        return this.baseMapper.deleteBatchIds(ids);
    }

    @Override
    public boolean saveOrUpdate(TestCaseSchedule schedule) {
        if (schedule == null) {
            throw new IllegalArgumentException("定时任务不能为空");
        }

        if (schedule.getId() == null) {
            return addSchedule(schedule);
        } else {
            return updateSchedule(schedule);
        }
    }

    @Override
    public Map<String, Object> getScheduleStats(Long projectId) {
        Map<String, Object> stats = new HashMap<>();

        LambdaQueryWrapper<TestCaseSchedule> queryWrapper = new LambdaQueryWrapper<>();
        if (projectId != null) {
            queryWrapper.eq(TestCaseSchedule::getProjectId, projectId);
        }

        // 总数
        int total = (int) this.count(queryWrapper);
        stats.put("total", total);

        // 启用数
        LambdaQueryWrapper<TestCaseSchedule> activeQuery = new LambdaQueryWrapper<>();
        if (projectId != null) {
            activeQuery.eq(TestCaseSchedule::getProjectId, projectId);
        }
        activeQuery.eq(TestCaseSchedule::getStatus, 1);
        int activeCount = (int) this.count(activeQuery);
        stats.put("activeCount", activeCount);

        // 禁用数
        int inactiveCount = total - activeCount;
        stats.put("inactiveCount", inactiveCount);

        // 启用率
        double activeRate = total > 0 ? (double) activeCount / total * 100 : 0;
        stats.put("activeRate", String.format("%.2f", activeRate));
        stats.put("activeRatePercent", String.format("%.2f%%", activeRate));

        // 最近7天创建的
        LambdaQueryWrapper<TestCaseSchedule> recentQuery = new LambdaQueryWrapper<>();
        if (projectId != null) {
            recentQuery.eq(TestCaseSchedule::getProjectId, projectId);
        }
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        recentQuery.ge(TestCaseSchedule::getCreateTime, sevenDaysAgo);
        int recentCount = (int) this.count(recentQuery);
        stats.put("recent7DaysCount", recentCount);

        return stats;
    }

    @Override
    public List<TestCaseSchedule> searchSchedules(String keyword, Long projectId, Integer status,
                                                  Integer pageNum, Integer pageSize) {
        LambdaQueryWrapper<TestCaseSchedule> queryWrapper = new LambdaQueryWrapper<>();

        // 关键词搜索
        if (StringUtils.hasText(keyword)) {
            queryWrapper.and(wrapper -> wrapper
                    .like(TestCaseSchedule::getScheduleName, keyword)
                    .or()
                    .like(TestCaseSchedule::getCronExpression, keyword)
            );
        }

        // 项目筛选
        if (projectId != null) {
            queryWrapper.eq(TestCaseSchedule::getProjectId, projectId);
        }

        // 状态筛选
        if (status != null) {
            queryWrapper.eq(TestCaseSchedule::getStatus, status);
        }

        // 排序
        queryWrapper.orderByDesc(TestCaseSchedule::getCreateTime);

        // 分页
        if (pageNum != null && pageSize != null) {
            queryWrapper.last(String.format("LIMIT %d OFFSET %d",
                    pageSize, (pageNum - 1) * pageSize));
        }

        return this.list(queryWrapper);
    }

    @Override
    public int countSchedules(String keyword, Long projectId, Integer status) {
        LambdaQueryWrapper<TestCaseSchedule> queryWrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(keyword)) {
            queryWrapper.and(wrapper -> wrapper
                    .like(TestCaseSchedule::getScheduleName, keyword)
                    .or()
                    .like(TestCaseSchedule::getCronExpression, keyword)
            );
        }

        if (projectId != null) {
            queryWrapper.eq(TestCaseSchedule::getProjectId, projectId);
        }

        if (status != null) {
            queryWrapper.eq(TestCaseSchedule::getStatus, status);
        }

        return (int) this.count(queryWrapper);
    }

    @Override
    public Integer toggleScheduleStatus(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("定时任务ID不能为空");
        }

        TestCaseSchedule schedule = this.getById(id);
        if (schedule == null) {
            throw new RuntimeException("定时任务不存在: " + id);
        }

        // 切换状态
        Integer newStatus = Objects.equals(schedule.getStatus(), 1) ? 0 : 1;
        schedule.setStatus(newStatus);
        schedule.setUpdateTime(LocalDateTime.now());

        this.updateById(schedule);
        return newStatus;
    }

    @Override
    public boolean validateCronExpression(String cronExpression) {
        if (!StringUtils.hasText(cronExpression)) {
            return false;
        }

        try {
            // 使用Spring的CronExpression验证
            return CronExpression.isValidExpression(cronExpression);
        } catch (Exception e) {
            log.warn("Cron表达式验证失败: {}", cronExpression, e);
            return false;
        }
    }

    @Override
    public List<TestCaseSchedule> getSchedulesByProjectAndCron(Long projectId, String cronExpression) {
        if (projectId == null || !StringUtils.hasText(cronExpression)) {
            return Collections.emptyList();
        }

        LambdaQueryWrapper<TestCaseSchedule> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TestCaseSchedule::getProjectId, projectId)
                .eq(TestCaseSchedule::getCronExpression, cronExpression)
                .orderByDesc(TestCaseSchedule::getCreateTime);

        return this.list(queryWrapper);
    }

    @Override
    public List<TestCaseSchedule> getSchedulesByName(String scheduleName) {
        if (!StringUtils.hasText(scheduleName)) {
            return Collections.emptyList();
        }

        LambdaQueryWrapper<TestCaseSchedule> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.like(TestCaseSchedule::getScheduleName, scheduleName)
                .orderByDesc(TestCaseSchedule::getCreateTime);

        return this.list(queryWrapper);
    }

    @Override
    public int batchToggleByProjectId(Long projectId, Integer status) {
        if (projectId == null || status == null) {
            return 0;
        }

        TestCaseSchedule schedule = new TestCaseSchedule();
        schedule.setStatus(status);
        schedule.setUpdateTime(LocalDateTime.now());

        LambdaQueryWrapper<TestCaseSchedule> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TestCaseSchedule::getProjectId, projectId);

        return this.baseMapper.update(schedule, queryWrapper);
    }

    @Override
    public List<TestCaseSchedule> getUpcomingSchedules(int minutes) {
        if (minutes <= 0) {
            minutes = 5; // 默认5分钟
        }

        // 获取所有启用的定时任务
        LambdaQueryWrapper<TestCaseSchedule> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TestCaseSchedule::getStatus, 1)
                .orderByAsc(TestCaseSchedule::getCreateTime);

        List<TestCaseSchedule> allEnabledSchedules = this.list(queryWrapper);

        if (allEnabledSchedules.isEmpty()) {
            return Collections.emptyList();
        }

        // 计算未来执行时间
        List<TestCaseSchedule> upcomingSchedules = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime maxTime = now.plusMinutes(minutes);

        for (TestCaseSchedule schedule : allEnabledSchedules) {
            try {
                // 使用Spring 5.3+的CronExpression解析
                CronExpression cronExpression = CronExpression.parse(schedule.getCronExpression());

                // 计算下次执行时间
                LocalDateTime nextExecutionTime = cronExpression.next(now);

                if (nextExecutionTime != null) {
                    // 检查是否在未来几分钟内
                    if (nextExecutionTime.isBefore(maxTime) || nextExecutionTime.isEqual(maxTime)) {
                        // 复制schedule并设置下次执行时间
                        TestCaseSchedule scheduleCopy = copySchedule(schedule);
                        scheduleCopy.setNextExecutionTime(nextExecutionTime);
                        scheduleCopy.setMinutesUntilNext(
                                (int) java.time.Duration.between(now, nextExecutionTime).toMinutes()
                        );

                        upcomingSchedules.add(scheduleCopy);
                    }
                }
            } catch (Exception e) {
                log.warn("计算定时任务下次执行时间失败: 任务ID={}, Cron={}",
                        schedule.getId(), schedule.getCronExpression(), e);
            }
        }

        // 按下次执行时间排序
        upcomingSchedules.sort(Comparator.comparing(
                schedule -> schedule.getNextExecutionTime() != null ?
                        schedule.getNextExecutionTime() : LocalDateTime.MAX
        ));

        return upcomingSchedules;
    }

    /**
     * 复制定时任务对象
     */
    private TestCaseSchedule copySchedule(TestCaseSchedule schedule) {
        TestCaseSchedule copy = new TestCaseSchedule();
        copy.setId(schedule.getId());
        copy.setScheduleName(schedule.getScheduleName());
        copy.setProjectId(schedule.getProjectId());
        copy.setCronExpression(schedule.getCronExpression());
        copy.setStatus(schedule.getStatus());
        copy.setCreateTime(schedule.getCreateTime());
        copy.setUpdateTime(schedule.getUpdateTime());
        return copy;
    }

    @Override
    public boolean isScheduleNameExists(String scheduleName, Long excludeId) {
        if (!StringUtils.hasText(scheduleName)) {
            return false;
        }

        LambdaQueryWrapper<TestCaseSchedule> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TestCaseSchedule::getScheduleName, scheduleName);

        if (excludeId != null) {
            queryWrapper.ne(TestCaseSchedule::getId, excludeId);
        }

        return this.count(queryWrapper) > 0;
    }

    @Override
    public boolean isCronExpressionUsed(String cronExpression, Long excludeId) {
        if (!StringUtils.hasText(cronExpression)) {
            return false;
        }

        LambdaQueryWrapper<TestCaseSchedule> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TestCaseSchedule::getCronExpression, cronExpression);

        if (excludeId != null) {
            queryWrapper.ne(TestCaseSchedule::getId, excludeId);
        }

        return this.count(queryWrapper) > 0;
    }
}