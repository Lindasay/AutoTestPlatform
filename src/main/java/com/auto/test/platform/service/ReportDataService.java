package com.auto.test.platform.service;

import com.auto.test.platform.entity.ReportData;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 测试报告service
 */
public interface ReportDataService extends IService<ReportData> {

    /**
     * 根据项目ID查询报告列表
     * @param projectId 项目ID
     * @return 报告列表
     */
    List<ReportData> listByProjectId(Long projectId);

    /**
     * 根据任务ID查询报告
     * @param taskId 任务ID
     * @return 报告详情
     */
    ReportData getByTaskId(Long taskId);

    /**
     * 删除指定项目下的所有报告（清理历史数据）
     * @param projectId 项目ID
     * @return 是否删除成功
     */
    boolean removeByProjectId(Long projectId);
}
