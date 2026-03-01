package com.auto.test.platform.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

/**
 * 任务调度配置类（架构config层必备，支持定时任务、异步任务执行）
 * 后续自动化用例执行、任务调度模块依赖次配置，提前完成避免后续冲突
 */
@Configuration
@EnableScheduling //开启任务调度功能（核心注解）
public class TaskSchedulingConfig implements SchedulingConfigurer {

    /**
     * 配置任务调度线程池（企业级规范，避免单线程阻塞）
     * 作用：多任务并行执行，防止单个任务耗时过长导致其他任务延迟
     * @param taskRegistrar
     */
    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
        //核心线程数（根据服务配置调整，建议5-10）
        taskScheduler.setPoolSize(5);
        //线程名称前缀（便于日志排查，区分不同线程任务）
        taskScheduler.setThreadNamePrefix("task-scheduler-");
        //线程空闲超时时间（30秒，空闲线程自动销毁，节省资源）
        taskScheduler.setAwaitTerminationMillis(30);
        //任务执行完成后关闭线程
        taskScheduler.setWaitForTasksToCompleteOnShutdown(true);
        //初始化线程池
        taskScheduler.initialize();
        //将线程池配置到任务注册器
        taskRegistrar.setTaskScheduler(taskScheduler);
    }
}
