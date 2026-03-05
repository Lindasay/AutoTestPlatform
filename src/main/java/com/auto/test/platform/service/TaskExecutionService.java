package com.auto.test.platform.service;

import com.auto.test.platform.entity.ReportData;
import com.auto.test.platform.entity.TaskExecution;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 任务执行核心任务（定时任务会调用者这里的方法）
 */
public interface TaskExecutionService extends IService<TaskExecution> {

    /**
     * 执行单条用例（复用Controller里的逻辑）
     * @param caseId 用例ID
     * @return 包含任+报告的结果
     */
    ReportData executeSingleCase(Long caseId);

    /**
     * 批量执行项目下的所有用例（定时任务核心调用）
     * @param projectId 项目ID
     * @return 聚合报告
     */
    ReportData executeBatchByProjectId(Long projectId);

}
