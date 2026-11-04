package com.aisaas.billing.service;

import com.aisaas.billing.dto.*;
import com.aisaas.common.result.Result;
import com.baomidou.mybatisplus.core.metadata.IPage;

import java.util.List;

/**
 * 配额服务接口
 */
public interface QuotaService {

    // ==================== 配额配置管理 ====================

    /**
     * 创建配额配置
     */
    Result<Void> createQuotaConfig(QuotaConfigCreateDTO dto);

    /**
     * 更新配额配置
     */
    Result<Void> updateQuotaConfig(QuotaConfigUpdateDTO dto);

    /**
     * 删除配额配置
     */
    Result<Void> deleteQuotaConfig(String configId);

    /**
     * 获取配额配置详情
     */
    Result<QuotaConfigVO> getQuotaConfig(String configId);

    /**
     * 分页查询配额配置
     */
    Result<IPage<QuotaConfigVO>> queryQuotaConfig(QuotaConfigQueryDTO dto);

    /**
     * 查询所有有效配置
     */
    Result<List<QuotaConfigVO>> getAllValidConfigs();

    // ==================== 配额记录管理 ====================

    /**
     * 初始化用户配额
     */
    Result<Void> initUserQuota(Long userId, Integer userType);

    /**
     * 获取用户配额记录
     */
    Result<List<QuotaRecordVO>> getUserQuotaRecords(Long userId);

    /**
     * 获取用户指定类型的配额
     */
    Result<QuotaRecordVO> getUserQuotaByType(Long userId, Integer quotaType);

    // ==================== 配额检查与扣减 ====================

    /**
     * 检查配额
     */
    Result<QuotaCheckResultVO> checkQuota(Long userId, Integer quotaType, Long requiredAmount);

    /**
     * 批量检查配额
     */
    Result<BatchQuotaCheckResultVO> batchCheckQuota(Long userId, List<QuotaCheckItemDTO> items);

    /**
     * 扣减配额
     */
    Result<QuotaDeductResultVO> deductQuota(Long userId, Integer quotaType, Long amount, String bizType, String bizId);

    /**
     * 批量扣减配额
     */
    Result<BatchQuotaDeductResultVO> batchDeductQuota(Long userId, List<QuotaDeductItemDTO> items);

    /**
     * 回滚配额
     */
    Result<Void> rollbackQuota(Long userId, Integer quotaType, Long amount, String bizType, String bizId);

    // ==================== 配额超限处理 ====================

    /**
     * 处理配额超限
     */
    Result<QuotaExceedHandleResultVO> handleQuotaExceed(Long userId, Integer quotaType, Long exceedAmount);

    /**
     * 获取配额超限记录
     */
    Result<IPage<QuotaExceedRecordVO>> getQuotaExceedRecords(QuotaExceedQueryDTO dto);

    // ==================== 配额重置 ====================

    /**
     * 每日重置配额
     */
    Result<DailyResetResultVO> dailyResetQuota();

    /**
     * 每月重置配额
     */
    Result<MonthlyResetResultVO> monthlyResetQuota();

    /**
     * 手动重置用户配额
     */
    Result<Void> manualResetUserQuota(Long userId, Integer quotaType, Integer resetType);

    /**
     * 获取重置记录
     */
    Result<IPage<QuotaResetRecordVO>> getResetRecords(QuotaResetQueryDTO dto);
}
