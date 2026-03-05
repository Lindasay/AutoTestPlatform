package com.auto.test.platform.config;

import com.auto.test.platform.entity.TestCaseSchedule;
import com.auto.test.platform.service.TaskExecutionService;
import com.auto.test.platform.service.TestCaseScheduleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTask;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureTask;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 动态定时任务配置(支持新增/修改/删除定时任务)
 */
@Slf4j
@Configuration
@EnableScheduling
public class DynamicScheduleConfig implements SchedulingConfigurer, DisposableBean {

    @Autowired
    private TestCaseScheduleService scheduleService;

    @Autowired
    private TaskExecutionService taskExecutionService;

    @Autowired(required = false)
    private TransactionTemplate transactionTemplate;

    // 存储已注册的定时任务
    private final Map<Long, ScheduledFutureHolder> taskMap = new ConcurrentHashMap<>();
    // 存储schedule信息的快照
    private final Map<Long, ScheduleSnapshot> scheduleSnapshots = new ConcurrentHashMap<>();
    private ScheduledTaskRegistrar taskRegistrar;
    private final AtomicBoolean initialized = new AtomicBoolean(false);
    private final ReentrantLock lock = new ReentrantLock();
    private ScheduledExecutorService executorService;
    private volatile boolean destroyed = false;

    // 自定义任务持有器，包装 ScheduledFuture
    private static class ScheduledFutureHolder {
        private final ScheduledFuture<?> future;
        private final Trigger trigger;
        private final Runnable task;

        public ScheduledFutureHolder(ScheduledFuture<?> future, Trigger trigger, Runnable task) {
            this.future = future;
            this.trigger = trigger;
            this.task = task;
        }

        public void cancel() {
            if (future != null && !future.isCancelled()) {
                future.cancel(true);
            }
        }

        public boolean isCancelled() {
            return future == null || future.isCancelled();
        }

        public boolean isDone() {
            return future != null && future.isDone();
        }

        @Override
        public String toString() {
            return String.format("ScheduledFutureHolder{future=%s, trigger=%s}",
                    future, trigger);
        }
    }

    // Schedule快照类
    private static class ScheduleSnapshot {
        private final Long scheduleId;
        private final String scheduleName;
        private final Long projectId;
        private final String cronExpression;
        private final Integer status;
        private final LocalDateTime createTime;
        private final LocalDateTime updateTime;

        public ScheduleSnapshot(TestCaseSchedule schedule) {
            this.scheduleId = schedule.getId();
            this.scheduleName = schedule.getScheduleName();
            this.projectId = schedule.getProjectId();
            this.cronExpression = schedule.getCronExpression();
            this.status = schedule.getStatus();
            this.createTime = schedule.getCreateTime();
            this.updateTime = schedule.getUpdateTime();
        }

        public boolean isValid() {
            return scheduleId != null &&
                    projectId != null &&
                    cronExpression != null && !cronExpression.trim().isEmpty() &&
                    status != null && status == 1;
        }

        @Override
        public String toString() {
            return String.format("ScheduleSnapshot{id=%d, name='%s', projectId=%d, cron='%s', status=%d}",
                    scheduleId, scheduleName, projectId, cronExpression, status);
        }
    }

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        if (destroyed) {
            log.warn("配置已被销毁，跳过configureTasks");
            return;
        }

        lock.lock();
        try {
            if (this.taskRegistrar != null) {
                log.debug("ScheduledTaskRegistrar 已配置过");
                return;
            }

            log.info("开始配置 ScheduledTaskRegistrar...");
            this.taskRegistrar = taskRegistrar;

            // 创建线程池
            this.executorService = Executors.newScheduledThreadPool(
                    10,
                    r -> {
                        Thread thread = new Thread(r, "schedule-task-" + ThreadLocalRandom.current().nextInt(1000));
                        thread.setDaemon(false);
                        thread.setUncaughtExceptionHandler((t, e) -> {
                            log.error("定时任务线程异常: {}", t.getName(), e);
                        });
                        return thread;
                    }
            );

            // 创建TaskScheduler
            TaskScheduler taskScheduler = new org.springframework.scheduling.concurrent.ConcurrentTaskScheduler(executorService);
            this.taskRegistrar.setScheduler(taskScheduler);

            log.info("ScheduledTaskRegistrar 配置完成");

            // 初始化定时任务
            initializeScheduledTasks();

        } finally {
            lock.unlock();
        }
    }

    private void initializeScheduledTasks() {
        if (initialized.compareAndSet(false, true)) {
            // 延迟初始化
            executorService.schedule(this::safeInitScheduledTasks, 3, TimeUnit.SECONDS);
        }
    }

    private void safeInitScheduledTasks() {
        if (destroyed) {
            return;
        }

        lock.lock();
        try {
            doInitScheduledTasks();
        } catch (Exception e) {
            log.error("定时任务初始化失败", e);
            initialized.set(false);
        } finally {
            lock.unlock();
        }
    }

    private void doInitScheduledTasks() {
        if (taskRegistrar == null) {
            log.error("ScheduledTaskRegistrar 尚未初始化，无法加载定时任务");
            return;
        }

        try {
            // 清理现有任务
            clearAllTasks();

            // 查询所有启用的定时任务
            List<TestCaseSchedule> enableSchedules = scheduleService.getEnableSchedules();
            if (enableSchedules == null || enableSchedules.isEmpty()) {
                log.info("未查询到启用的定时任务");
                return;
            }

            log.info("查询到 {} 个启用的定时任务，开始加载...", enableSchedules.size());

            // 逐个加载定时任务
            int successCount = 0;
            for (TestCaseSchedule schedule : enableSchedules) {
                try {
                    if (addScheduledTaskInternal(schedule)) {
                        successCount++;
                    }
                } catch (Exception e) {
                    log.error("加载定时任务失败: {} (ID: {})",
                            schedule.getScheduleName(), schedule.getId(), e);
                }
            }

            log.info("定时任务初始化完成，成功加载 {}/{} 个任务", successCount, enableSchedules.size());

        } catch (Exception e) {
            log.error("初始化定时任务异常", e);
            throw e;
        }
    }

    private boolean addScheduledTaskInternal(TestCaseSchedule schedule) {
        if (destroyed) {
            return false;
        }

        if (schedule == null || schedule.getId() == null) {
            log.warn("定时任务对象或ID为空");
            return false;
        }

        Long scheduleId = schedule.getId();

        // 验证schedule状态
        if (schedule.getStatus() != 1) {
            log.warn("定时任务 {} 已禁用，跳过添加", schedule.getScheduleName());
            return false;
        }

        if (schedule.getProjectId() == null) {
            log.warn("定时任务 {} 项目ID为空，跳过添加", schedule.getScheduleName());
            return false;
        }

        String cronExpression = schedule.getCronExpression();
        if (cronExpression == null || cronExpression.trim().isEmpty()) {
            log.warn("定时任务 {} Cron表达式为空，跳过添加", schedule.getScheduleName());
            return false;
        }

        // 验证Cron表达式
        try {
            new CronTrigger(cronExpression);
        } catch (Exception e) {
            log.error("定时任务 {} Cron表达式无效: {}", schedule.getScheduleName(), cronExpression, e);
            return false;
        }

        lock.lock();
        try {
            // 检查是否已存在
            if (taskMap.containsKey(scheduleId)) {
                log.info("定时任务 {} 已存在，先移除旧任务", schedule.getScheduleName());
                removeScheduledTaskInternal(scheduleId);
            }

            // 创建schedule快照
            ScheduleSnapshot snapshot = new ScheduleSnapshot(schedule);
            if (!snapshot.isValid()) {
                log.error("定时任务 {} 的快照无效", schedule.getScheduleName());
                return false;
            }

            // 创建Cron触发器
            CronTrigger trigger = new CronTrigger(cronExpression);

            // 创建任务
            Runnable task = () -> {
                if (destroyed) {
                    return;
                }
                executeScheduledTask(snapshot);
            };

            // 在 Spring Boot 3.x 中注册定时任务
            ScheduledFutureHolder scheduledTask = scheduleDynamicTask(task, trigger);

            if (scheduledTask != null) {
                // 保存到任务映射
                taskMap.put(scheduleId, scheduledTask);
                scheduleSnapshots.put(scheduleId, snapshot);
                log.info("添加定时任务成功: {} (ID: {}), Cron: {}, ProjectId: {}",
                        schedule.getScheduleName(), scheduleId, cronExpression, schedule.getProjectId());
                return true;
            } else {
                log.error("创建定时任务失败: {} (ID: {})", schedule.getScheduleName(), scheduleId);
                return false;
            }

        } catch (Exception e) {
            log.error("添加定时任务异常: {} (ID: {})", schedule.getScheduleName(), scheduleId, e);
            taskMap.remove(scheduleId);
            scheduleSnapshots.remove(scheduleId);
            return false;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Spring Boot 3.x 中动态注册定时任务
     */
    private ScheduledFutureHolder scheduleDynamicTask(Runnable task, Trigger trigger) {
        if (taskRegistrar == null || taskRegistrar.getScheduler() == null) {
            log.error("TaskScheduler 未初始化");
            return null;
        }

        try {
            TaskScheduler scheduler = taskRegistrar.getScheduler();

            // 通过 TaskScheduler 注册任务
            ScheduledFuture<?> future = scheduler.schedule(task, trigger);

            if (future == null) {
                log.error("调度任务返回 null");
                return null;
            }

            // 使用 addTriggerTask 确保任务被正确管理
            taskRegistrar.addTriggerTask(task, trigger);

            // 创建自定义的任务持有器
            return new ScheduledFutureHolder(future, trigger, task);

        } catch (Exception e) {
            log.error("注册定时任务失败", e);
            return null;
        }
    }

    /**
     * 执行定时任务
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected void executeScheduledTask(ScheduleSnapshot snapshot) {
        if (destroyed || snapshot == null || !snapshot.isValid()) {
            return;
        }

        String taskName = snapshot.scheduleName;
        Long scheduleId = snapshot.scheduleId;
        Long projectId = snapshot.projectId;

        log.info("开始执行定时任务: {} (ID: {}), 项目ID: {}", taskName, scheduleId, projectId);

        long startTime = System.currentTimeMillis();
        try {
            if (transactionTemplate != null) {
                // 在新事务中执行
                transactionTemplate.execute(status -> {
                    try {
                        taskExecutionService.executeBatchByProjectId(projectId);
                        return true;
                    } catch (Exception e) {
                        status.setRollbackOnly();
                        throw e;
                    }
                });
            } else {
                // 如果没有TransactionTemplate，直接执行
                taskExecutionService.executeBatchByProjectId(projectId);
            }

            long endTime = System.currentTimeMillis();
            log.info("定时任务 {} 执行完成，耗时: {}ms", taskName, (endTime - startTime));

        } catch (Exception e) {
            log.error("定时任务 {} 执行失败", taskName, e);
        }
    }

    public boolean addScheduledTask(TestCaseSchedule schedule) {
        if (destroyed || !initialized.get()) {
            log.warn("定时任务系统未就绪");
            return false;
        }

        if (taskRegistrar == null) {
            log.error("ScheduledTaskRegistrar 未初始化");
            return false;
        }

        return addScheduledTaskInternal(schedule);
    }

    public boolean updateSchedule(TestCaseSchedule schedule) {
        if (destroyed) {
            return false;
        }

        if (schedule == null || schedule.getId() == null) {
            log.warn("定时任务对象或ID为空");
            return false;
        }

        Long scheduleId = schedule.getId();
        log.info("更新定时任务: {} (ID: {})", schedule.getScheduleName(), scheduleId);

        lock.lock();
        try {
            // 先移除旧任务
            removeScheduledTaskInternal(scheduleId);

            // 如果任务启用，则添加新任务
            if (schedule.getStatus() == 1) {
                boolean success = addScheduledTaskInternal(schedule);
                if (success) {
                    log.info("更新定时任务成功: {} (ID: {})", schedule.getScheduleName(), scheduleId);
                } else {
                    log.error("更新定时任务失败: {} (ID: {})", schedule.getScheduleName(), scheduleId);
                }
                return success;
            } else {
                log.info("定时任务 {} (ID: {}) 已禁用，仅移除旧任务", schedule.getScheduleName(), scheduleId);
                return true;
            }
        } finally {
            lock.unlock();
        }
    }

    private void removeScheduledTaskInternal(Long scheduleId) {
        if (scheduleId == null) {
            return;
        }

        // 从map中获取ScheduledFutureHolder
        ScheduledFutureHolder holder = taskMap.remove(scheduleId);
        if (holder != null) {
            try {
                holder.cancel();
                log.info("取消定时任务执行，ID: {}", scheduleId);
            } catch (Exception e) {
                log.error("取消定时任务异常，ID: {}", scheduleId, e);
            }
        }

        // 清理快照
        scheduleSnapshots.remove(scheduleId);
    }

    public boolean removeScheduledTask(Long scheduleId) {
        if (destroyed) {
            return false;
        }

        lock.lock();
        try {
            removeScheduledTaskInternal(scheduleId);
            return true;
        } finally {
            lock.unlock();
        }
    }

    public void clearAllTasks() {
        if (destroyed) {
            return;
        }

        lock.lock();
        try {
            log.info("开始清理所有定时任务，当前任务数: {}", taskMap.size());

            if (taskMap.isEmpty()) {
                return;
            }

            // 复制key列表
            List<Long> taskIds = List.copyOf(taskMap.keySet());

            for (Long scheduleId : taskIds) {
                try {
                    removeScheduledTaskInternal(scheduleId);
                } catch (Exception e) {
                    log.error("清理定时任务异常，ID: {}", scheduleId, e);
                }
            }

            log.info("清理所有定时任务完成");
        } finally {
            lock.unlock();
        }
    }

    public void reloadAllTasks() {
        if (destroyed) {
            return;
        }

        lock.lock();
        try {
            log.info("重新加载所有定时任务...");
            clearAllTasks();
            doInitScheduledTasks();
        } finally {
            lock.unlock();
        }
    }

    public int getActiveTaskCount() {
        return taskMap.size();
    }

    public boolean containsTask(Long scheduleId) {
        return scheduleId != null && taskMap.containsKey(scheduleId);
    }

    public List<Long> getActiveTaskIds() {
        return List.copyOf(taskMap.keySet());
    }

    public Map<String, Object> getTaskStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("initialized", initialized.get());
        status.put("activeTaskCount", taskMap.size());
        status.put("taskIds", getActiveTaskIds());
        status.put("destroyed", destroyed);
        status.put("schedulerInitialized", taskRegistrar != null);
        return status;
    }

    @Override
    public void destroy() throws Exception {
        log.info("开始销毁动态定时任务配置...");
        destroyed = true;

        // 清理所有定时任务
        clearAllTasks();

        // 关闭线程池
        if (executorService != null) {
            try {
                executorService.shutdown();
                if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
                log.info("定时任务线程池已关闭");
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
                log.error("关闭线程池时被中断", e);
            }
        }

        // 清理资源
        taskMap.clear();
        scheduleSnapshots.clear();
        taskRegistrar = null;
        executorService = null;

        log.info("动态定时任务配置销毁完成");
    }
}