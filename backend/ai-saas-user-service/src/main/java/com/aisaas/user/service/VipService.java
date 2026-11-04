package com.aisaas.user.service;

import com.aisaas.user.dto.VipInfoDTO;
import com.aisaas.user.entity.VipMembership;
import com.aisaas.common.result.Result;
import com.baomidou.mybatisplus.extension.service.IService;

import java.math.BigDecimal;
import java.util.List;

/**
 * VIP会员服务接口
 */
public interface VipService extends IService<VipMembership> {

    /**
     * 获取用户VIP信息
     */
    Result<VipInfoDTO> getUserVipInfo(Long userId);

    /**
     * 检查用户是否是VIP
     */
    boolean isVip(Long userId);

    /**
     * 检查用户VIP等级是否满足要求
     */
    boolean checkVipLevel(Long userId, Integer minLevel);

    /**
     * 开通VIP会员
     */
    Result<VipInfoDTO> subscribeVip(Long userId, Integer vipLevel, Integer subscribeType, 
                                     BigDecimal payAmount, Integer source, String remark);

    /**
     * 续费VIP会员
     */
    Result<VipInfoDTO> renewVip(Long userId, Integer subscribeType, BigDecimal payAmount);

    /**
     * 升级VIP会员
     */
    Result<VipInfoDTO> upgradeVip(Long userId, Integer targetLevel, BigDecimal upgradeAmount);

    /**
     * 取消VIP自动续费
     */
    Result<Void> cancelAutoRenew(Long userId);

    /**
     * 开启VIP自动续费
     */
    Result<Void> enableAutoRenew(Long userId);

    /**
     * 获取用户VIP历史记录
     */
    Result<List<VipInfoDTO>> getVipHistory(Long userId);

    /**
     * 检查并处理过期的VIP会员
     */
    void checkAndExpireVip();

    /**
     * 获取VIP等级名称
     */
    String getVipLevelName(Integer vipLevel);

    /**
     * 获取VIP订阅类型名称
     */
    String getSubscribeTypeName(Integer subscribeType);
}
