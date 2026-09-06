package com.aisaas.user.service.impl;

import com.aisaas.common.constant.ResultCode;
import com.aisaas.common.result.Result;
import com.aisaas.user.dto.QuotaDTO;
import com.aisaas.user.entity.UserQuota;
import com.aisaas.user.mapper.UserQuotaMapper;
import com.aisaas.user.service.QuotaService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 配额服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QuotaServiceImpl extends ServiceImpl<UserQuotaMapper, UserQuota> implements QuotaService {

    private final UserQuotaMapper userQuotaMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void initUserQuota(Long userId) {
        // 初始化对话Token配额
        createDefaultQuota(userId, 1, 10000L, 300000L, 0L);
        
        // 初始化任务次数配额
        createDefaultQuota(userId, 2, 10L, 300L, 0L);
        
        // 初始化存储空间配额 (单位: MB)
        createDefaultQuota(userId, 3, 100L, 3000L, 0L);
        
        log.info("初始化用户配额成功: userId={}", userId);
    }

    private void createDefaultQuota(Long userId, Integer quotaType, Long dailyLimit, Long monthlyLimit, Long totalLimit) {
        UserQuota quota = new UserQuota();
        quota.setUserId(userId);
        quota.setQuotaType(quotaType);
        quota.setDailyLimit(dailyLimit);
        quota.setMonthlyLimit(monthlyLimit);
        quota.setTotalLimit(totalLimit);
        quota.setDailyUsed(0L);
        quota.setMonthlyUsed(0L);
        quota.setTotalUsed(0L);
        quota.setResetDay(1);
        quota.setEffectiveAt(LocalDateTime.now());
        quota.setIsDeleted(0);
        quota.setCreatedAt(LocalDateTime.now());
        quota.setUpdatedAt(LocalDateTime.now());
        
        userQuotaMapper.insert(quota);
    }

    @Override
    public Result<QuotaDTO> getUserQuota(Long userId, Integer quotaType) {
        UserQuota quota = userQuotaMapper.selectByUserIdAndType(userId, quotaType);
        if (quota == null) {
            return Result.error(ResultCode.NOT_FOUND, "配额信息不存在");
        }
        return Result.success(convertToQuotaDTO(quota));
    }

    @Override
    public Result<List<QuotaDTO>> getUserAllQuotas(Long userId) {
        List<UserQuota> quotas = userQuotaMapper.selectByUserId(userId);
        List<QuotaDTO> dtoList = new ArrayList<>();
        for (UserQuota quota : quotas) {
            dtoList.add(convertToQuotaDTO(quota));
        }
        return Result.success(dtoList);
    }

    @Override
    public boolean checkQuota(Long userId, Integer quotaType, Long requiredAmount) {
        UserQuota quota = userQuotaMapper.selectByUserIdAndType(userId, quotaType);
        if (quota == null) {
            return false;
        }
        return !quota.isExceeded() && (quota.getDailyLimit() - quota.getDailyUsed()) >= requiredAmount;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> deductQuota(Long userId, Integer quotaType, Long amount) {
        if (amount <= 0) {
            return Result.error(ResultCode.BAD_REQUEST, "扣减量必须大于0");
        }

        UserQuota quota = userQuotaMapper.selectByUserIdAndType(userId, quotaType);
        if (quota == null) {
            return Result.error(ResultCode.NOT_FOUND, "配额信息不存在");
        }

        if (quota.isExceeded()) {
            return Result.error(ResultCode.QUOTA_EXCEEDED, "配额已超限");
        }

        if ((quota.getDailyLimit() - quota.getDailyUsed()) < amount) {
            return Result.error(ResultCode.QUOTA_EXCEEDED, "剩余配额不足");
        }

        int result = userQuotaMapper.incrementUsed(userId, quotaType, amount);
        if (result <= 0) {
            return Result.error(ResultCode.INTERNAL_ERROR, "配额扣减失败");
        }

        log.info("扣减用户配额成功: userId={}, quotaType={}, amount={}", userId, quotaType, amount);
        return Result.success();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> addQuota(Long userId, Integer quotaType, Long amount) {
        if (amount <= 0) {
            return Result.error(ResultCode.BAD_REQUEST, "增加量必须大于0");
        }

        UserQuota quota = userQuotaMapper.selectByUserIdAndType(userId, quotaType);
        if (quota == null) {
            return Result.error(ResultCode.NOT_FOUND, "配额信息不存在");
        }

        // 直接修改限制值，而不是使用量
        quota.setTotalLimit(quota.getTotalLimit() + amount);
        quota.setUpdatedAt(LocalDateTime.now());
        updateById(quota);

        log.info("增加用户配额成功: userId={}, quotaType={}, amount={}", userId, quotaType, amount);
        return Result.success();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> setQuotaLimit(Long userId, Integer quotaType, Long dailyLimit, Long monthlyLimit, Long totalLimit) {
        UserQuota quota = userQuotaMapper.selectByUserIdAndType(userId, quotaType);
        if (quota == null) {
            return Result.error(ResultCode.NOT_FOUND, "配额信息不存在");
        }

        if (dailyLimit != null && dailyLimit >= 0) {
            quota.setDailyLimit(dailyLimit);
        }
        if (monthlyLimit != null && monthlyLimit >= 0) {
            quota.setMonthlyLimit(monthlyLimit);
        }
        if (totalLimit != null && totalLimit >= 0) {
            quota.setTotalLimit(totalLimit);
        }

        quota.setUpdatedAt(LocalDateTime.now());
        updateById(quota);

        log.info("设置用户配额限制成功: userId={}, quotaType={}", userId, quotaType);
        return Result.success();
    }

    @Override
    public void resetDailyQuota() {
        userQuotaMapper.resetDailyUsed();
        log.info("重置每日配额统计完成");
    }

    @Override
    public void resetMonthlyQuota() {
        userQuotaMapper.resetMonthlyUsed();
        log.info("重置每月配额统计完成");
    }

    @Override
    public String getQuotaTypeName(Integer quotaType) {
        if (quotaType == null) return "未知";
        switch (quotaType) {
            case 1: return "对话Token";
            case 2: return "任务次数";
            case 3: return "存储空间";
            default: return "未知";
        }
    }

    /**
     * 转换为DTO
     */
    private QuotaDTO convertToQuotaDTO(UserQuota quota) {
        QuotaDTO dto = new QuotaDTO();
        dto.setQuotaType(quota.getQuotaType());
        dto.setQuotaTypeName(getQuotaTypeName(quota.getQuotaType()));
        dto.setDailyLimit(quota.getDailyLimit());
        dto.setMonthlyLimit(quota.getMonthlyLimit());
        dto.setTotalLimit(quota.getTotalLimit());
        dto.setDailyUsed(quota.getDailyUsed());
        dto.setMonthlyUsed(quota.getMonthlyUsed());
        dto.setTotalUsed(quota.getTotalUsed());
        dto.setDailyRemaining(quota.getDailyLimit() - quota.getDailyUsed());
        dto.setMonthlyRemaining(quota.getMonthlyLimit() - quota.getMonthlyUsed());
        dto.setExceeded(quota.isExceeded());
        return dto;
    }
}
