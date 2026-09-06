package com.aisaas.billing.service;

import com.aisaas.billing.dto.*;
import com.aisaas.common.result.Result;
import com.baomidou.mybatisplus.core.metadata.IPage;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Token使用记录服务接口
 */
public interface TokenUsageService {

    /**
     * 记录Token使用
     */
    Result<Void> recordTokenUsage(TokenUsageRecordDTO dto);

    /**
     * 批量记录Token使用
     */
    Result<Void> batchRecordTokenUsage(BatchTokenUsageDTO dto);

    /**
     * 分页查询用户的Token使用记录
     */
    Result<IPage<TokenUsageRecordVO>> queryUserTokenUsage(TokenUsageQueryDTO dto);

    /**
     * 查询Token使用统计
     */
    Result<TokenUsageStatisticsVO> getTokenUsageStatistics(TokenUsageStatisticsQueryDTO dto);

    /**
     * 查询每日统计
     */
    Result<DailyStatisticsVO> getDailyStatistics(Long userId, LocalDate startDate, LocalDate endDate);

    /**
     * 按模型统计
     */
    Result<ModelStatisticsVO> getModelStatistics(Long userId, LocalDate startDate, LocalDate endDate);

    /**
     * 按操作类型统计
     */
    Result<OperationTypeStatisticsVO> getOperationTypeStatistics(Long userId, LocalDate startDate, LocalDate endDate);

    /**
     * 计算费用
     */
    Result<CostCalculationVO> calculateCost(CostCalculationDTO dto);

    /**
     * 获取用户的未计费金额
     */
    Result<BigDecimal> getUnbilledAmount(Long userId);

    /**
     * 导出使用记录
     */
    Result<String> exportUsageRecords(TokenUsageExportDTO dto);
}
