package com.aisaas.user.service;

import com.aisaas.user.dto.QuotaDTO;
import com.aisaas.user.entity.UserQuota;
import com.aisaas.common.result.Result;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 配额服务接口
 */
public interface QuotaService extends IService<UserQuota> {

    /**
     * 初始化用户配额
     */
    void initUserQuota(Long userId);

    /**
     * 获取用户配额信息
     */
    Result<QuotaDTO> getUserQuota(Long userId, Integer quotaType);

    /**
     * 获取用户所有配额信息
     */
    Result<List<QuotaDTO>> getUserAllQuotas(Long userId);

    /**
     * 检查用户配额是否充足
     */
    boolean checkQuota(Long userId, Integer quotaType, Long requiredAmount);

    /**
     * 扣减用户配额
     */
    Result<Void> deductQuota(Long userId, Integer quotaType, Long amount);

    /**
     * 增加用户配额
     */
    Result<Void> addQuota(Long userId, Integer quotaType, Long amount);

    /**
     * 设置用户配额上限
     */
    Result<Void> setQuotaLimit(Long userId, Integer quotaType, Long dailyLimit, Long monthlyLimit, Long totalLimit);

    /**
     * 重置每日配额使用统计
     */
    void resetDailyQuota();

    /**
     * 重置每月配额使用统计
     */
    void resetMonthlyQuota();

    /**
     * 获取配额类型名称
     */
    String getQuotaTypeName(Integer quotaType);
}
