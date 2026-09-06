package com.aisaas.user.service.impl;

import com.aisaas.common.constant.ResultCode;
import com.aisaas.common.result.Result;
import com.aisaas.user.dto.VipInfoDTO;
import com.aisaas.user.entity.UserAccount;
import com.aisaas.user.entity.VipMembership;
import com.aisaas.user.mapper.VipMembershipMapper;
import com.aisaas.user.service.VipService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

/**
 * VIP会员服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VipServiceImpl extends ServiceImpl<VipMembershipMapper, VipMembership> implements VipService {

    private final com.aisaas.user.mapper.UserAccountMapper userAccountMapper;
    private final VipMembershipMapper vipMembershipMapper;

    @Override
    public Result<VipInfoDTO> getUserVipInfo(Long userId) {
        VipMembership vip = vipMembershipMapper.selectValidByUserId(userId);
        return Result.success(convertToVipInfoDTO(vip));
    }

    @Override
    public boolean isVip(Long userId) {
        return vipMembershipMapper.countValidByUserId(userId) > 0;
    }

    @Override
    public boolean checkVipLevel(Long userId, Integer minLevel) {
        VipMembership vip = vipMembershipMapper.selectValidByUserId(userId);
        return vip != null && vip.getVipLevel() >= minLevel;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<VipInfoDTO> subscribeVip(Long userId, Integer vipLevel, Integer subscribeType, 
                                           BigDecimal payAmount, Integer source, String remark) {
        // 检查用户是否已有有效VIP
        VipMembership existVip = vipMembershipMapper.selectValidByUserId(userId);
        if (existVip != null) {
            return Result.error(ResultCode.BUSINESS_ERROR, "用户已开通VIP，请先升级或续费");
        }

        // 计算到期时间
        LocalDateTime startAt = LocalDateTime.now();
        LocalDateTime expireAt = calculateExpireAt(startAt, subscribeType);

        // 创建VIP会员记录
        VipMembership vip = new VipMembership();
        vip.setUserId(userId);
        vip.setVipLevel(vipLevel);
        vip.setVipName(getVipLevelName(vipLevel));
        vip.setSubscribeType(subscribeType);
        vip.setPayAmount(payAmount);
        vip.setStartAt(startAt);
        vip.setExpireAt(expireAt);
        vip.setAutoRenew(0);
        vip.setStatus(1);
        vip.setSource(source);
        vip.setRemark(remark);
        vip.setIsDeleted(0);
        vip.setCreatedAt(LocalDateTime.now());
        vip.setUpdatedAt(LocalDateTime.now());

        vipMembershipMapper.insert(vip);

        log.info("用户开通VIP成功: userId={}, vipLevel={}, subscribeType={}", userId, vipLevel, subscribeType);
        return Result.success(convertToVipInfoDTO(vip));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<VipInfoDTO> renewVip(Long userId, Integer subscribeType, BigDecimal payAmount) {
        VipMembership currentVip = vipMembershipMapper.selectValidByUserId(userId);
        if (currentVip == null) {
            return Result.error(ResultCode.BUSINESS_ERROR, "用户未开通VIP");
        }

        // 在当前到期时间基础上续期
        LocalDateTime startAt = currentVip.getExpireAt().isAfter(LocalDateTime.now()) 
                ? currentVip.getExpireAt() : LocalDateTime.now();
        LocalDateTime expireAt = calculateExpireAt(startAt, subscribeType);

        currentVip.setSubscribeType(subscribeType);
        currentVip.setPayAmount(currentVip.getPayAmount().add(payAmount));
        currentVip.setExpireAt(expireAt);
        currentVip.setUpdatedAt(LocalDateTime.now());

        updateById(currentVip);

        log.info("用户续费VIP成功: userId={}, subscribeType={}", userId, subscribeType);
        return Result.success(convertToVipInfoDTO(currentVip));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<VipInfoDTO> upgradeVip(Long userId, Integer targetLevel, BigDecimal upgradeAmount) {
        VipMembership currentVip = vipMembershipMapper.selectValidByUserId(userId);
        if (currentVip == null) {
            return Result.error(ResultCode.BUSINESS_ERROR, "用户未开通VIP");
        }

        if (currentVip.getVipLevel() >= targetLevel) {
            return Result.error(ResultCode.BUSINESS_ERROR, "目标VIP等级必须高于当前等级");
        }

        currentVip.setVipLevel(targetLevel);
        currentVip.setVipName(getVipLevelName(targetLevel));
        currentVip.setPayAmount(currentVip.getPayAmount().add(upgradeAmount));
        currentVip.setUpdatedAt(LocalDateTime.now());

        updateById(currentVip);

        log.info("用户升级VIP成功: userId={}, targetLevel={}", userId, targetLevel);
        return Result.success(convertToVipInfoDTO(currentVip));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> cancelAutoRenew(Long userId) {
        VipMembership vip = vipMembershipMapper.selectValidByUserId(userId);
        if (vip == null) {
            return Result.error(ResultCode.BUSINESS_ERROR, "用户未开通VIP");
        }

        vip.setAutoRenew(0);
        vip.setUpdatedAt(LocalDateTime.now());
        updateById(vip);

        log.info("用户取消自动续费成功: userId={}", userId);
        return Result.success();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> enableAutoRenew(Long userId) {
        VipMembership vip = vipMembershipMapper.selectValidByUserId(userId);
        if (vip == null) {
            return Result.error(ResultCode.BUSINESS_ERROR, "用户未开通VIP");
        }

        vip.setAutoRenew(1);
        vip.setUpdatedAt(LocalDateTime.now());
        updateById(vip);

        log.info("用户开启自动续费成功: userId={}", userId);
        return Result.success();
    }

    @Override
    public Result<List<VipInfoDTO>> getVipHistory(Long userId) {
        List<VipMembership> list = vipMembershipMapper.selectByUserId(userId);
        List<VipInfoDTO> dtoList = list.stream()
                .map(this::convertToVipInfoDTO)
                .collect(Collectors.toList());
        return Result.success(dtoList);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @org.springframework.scheduling.annotation.Scheduled(cron = "0 0 * * * ?")
    public void checkAndExpireVip() {
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<VipMembership> wrapper =
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        wrapper.eq(VipMembership::getStatus, 1)
               .isNotNull(VipMembership::getExpireAt)
               .lt(VipMembership::getExpireAt, java.time.LocalDateTime.now());
        java.util.List<VipMembership> expired = vipMembershipMapper.selectList(wrapper);
        if (expired.isEmpty()) {
            return;
        }
        int count = 0;
        for (VipMembership membership : expired) {
            membership.setStatus(0); // 0-已过期
            membership.setUpdatedAt(java.time.LocalDateTime.now());
            vipMembershipMapper.updateById(membership);
            count++;
            // 降级用户VIP标识，保证后续鉴权/配额立即生效
            try {
                UserAccount user = userAccountMapper.selectById(membership.getUserId());
                if (user != null && user.getVipLevel() != null && user.getVipLevel() > 0) {
                    user.setVipLevel(0);
                    user.setUpdatedAt(java.time.LocalDateTime.now());
                    userAccountMapper.updateById(user);
                }
            } catch (Exception e) {
                log.warn("VIP过期后降级用户等级失败: userId={}", membership.getUserId(), e);
            }
        }
        log.info("VIP过期处理完成: 共 {} 条会员到期", count);
    }

    @Override
    public String getVipLevelName(Integer vipLevel) {
        if (vipLevel == null) return "未知";
        switch (vipLevel) {
            case 1: return "普通VIP";
            case 2: return "高级VIP";
            case 3: return "至尊VIP";
            default: return "未知";
        }
    }

    @Override
    public String getSubscribeTypeName(Integer subscribeType) {
        if (subscribeType == null) return "未知";
        switch (subscribeType) {
            case 1: return "月付";
            case 2: return "季付";
            case 3: return "年付";
            case 4: return "永久";
            default: return "未知";
        }
    }

    /**
     * 计算到期时间
     */
    private LocalDateTime calculateExpireAt(LocalDateTime startAt, Integer subscribeType) {
        switch (subscribeType) {
            case 1: // 月付
                return startAt.plus(1, ChronoUnit.MONTHS);
            case 2: // 季付
                return startAt.plus(3, ChronoUnit.MONTHS);
            case 3: // 年付
                return startAt.plus(1, ChronoUnit.YEARS);
            case 4: // 永久
                return LocalDateTime.of(2099, 12, 31, 23, 59, 59);
            default:
                return startAt.plus(1, ChronoUnit.MONTHS);
        }
    }

    /**
     * 转换为DTO
     */
    private VipInfoDTO convertToVipInfoDTO(VipMembership vip) {
        VipInfoDTO dto = new VipInfoDTO();
        if (vip == null) {
            dto.setIsVip(false);
            return dto;
        }

        dto.setIsVip(vip.isValid());
        dto.setVipLevel(vip.getVipLevel());
        dto.setVipName(vip.getVipName());
        dto.setSubscribeType(vip.getSubscribeType());
        dto.setStartAt(vip.getStartAt());
        dto.setExpireAt(vip.getExpireAt());
        dto.setAutoRenew(vip.getAutoRenew() == 1);

        // 计算剩余天数
        if (vip.getExpireAt() != null && vip.getExpireAt().isAfter(LocalDateTime.now())) {
            dto.setRemainingDays(ChronoUnit.DAYS.between(LocalDateTime.now(), vip.getExpireAt()));
        } else {
            dto.setRemainingDays(0L);
        }

        return dto;
    }
}
