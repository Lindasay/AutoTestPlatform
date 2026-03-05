package com.auto.test.platform.service.impl;

import com.auto.test.platform.entity.ReportData;
import com.auto.test.platform.mapper.ReportDataMapper;
import com.auto.test.platform.service.ReportDataService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 报告数据服务实现（和TaskExecutionService联动保存/查询报告）
 */
@Slf4j
@Service
public class ReportDataServiceImpl extends ServiceImpl<ReportDataMapper, ReportData> implements ReportDataService {

    /**
     * 根据项目ID查询报告列表（前端展示报告用）
     */
    @Override
    public List<ReportData> listByProjectId(Long projectId) {
        LambdaQueryWrapper<ReportData> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ReportData::getProjectId, projectId)
                .orderByDesc(ReportData::getCreateTime); //按时间倒序（最新的在前面）

        return this.list(queryWrapper);
    }

    /**
     * 根据任务ID查询报告（关联TaskExecution）
     */
    @Override
    public ReportData getByTaskId(Long taskId) {
        LambdaQueryWrapper<ReportData> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ReportData::getTaskId, taskId);
        return this.getOne(queryWrapper);
    }

    /**
     * 删除指定项目下的所有报告（清理历史数据）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeByProjectId(Long projectId) {
        try {
            LambdaQueryWrapper<ReportData> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(ReportData::getProjectId, projectId);
            return this.remove(queryWrapper);
        } catch (Exception e) {
            log.error("删除项目ID：{} 的报告失败",projectId,e);
            return false;
        }
    }
}
