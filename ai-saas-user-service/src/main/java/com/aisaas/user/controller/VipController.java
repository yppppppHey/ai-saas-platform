package com.aisaas.user.controller;

import com.aisaas.common.result.Result;
import com.aisaas.user.dto.VipInfoDTO;
import com.aisaas.user.entity.VipMembership;
import com.aisaas.user.mapper.VipMembershipMapper;
import com.aisaas.user.service.VipService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * VIP会员控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/vip")
@RequiredArgsConstructor
public class VipController {

    private final VipService vipService;

    /**
     * 获取当前用户的VIP信息
     */
    @GetMapping("/me")
    public Result<VipInfoDTO> getMyVipInfo(@RequestAttribute("userId") Long userId) {
        return vipService.getUserVipInfo(userId);
    }

    /**
     * 检查是否是VIP
     */
    @GetMapping("/me/check")
    public Result<Boolean> checkIsVip(@RequestAttribute("userId") Long userId) {
        return Result.success(vipService.isVip(userId));
    }

    /**
     * 开通VIP
     */
    @PostMapping("/me/subscribe")
    public Result<VipInfoDTO> subscribeVip(@RequestAttribute("userId") Long userId,
                                          @RequestParam Integer vipLevel,
                                          @RequestParam Integer subscribeType,
                                          @RequestParam BigDecimal payAmount) {
        return vipService.subscribeVip(userId, vipLevel, subscribeType, payAmount, 1, null);
    }

    /**
     * 续费VIP
     */
    @PostMapping("/me/renew")
    public Result<VipInfoDTO> renewVip(@RequestAttribute("userId") Long userId,
                                      @RequestParam Integer subscribeType,
                                      @RequestParam BigDecimal payAmount) {
        return vipService.renewVip(userId, subscribeType, payAmount);
    }

    /**
     * 升级VIP
     */
    @PostMapping("/me/upgrade")
    public Result<VipInfoDTO> upgradeVip(@RequestAttribute("userId") Long userId,
                                        @RequestParam Integer targetLevel,
                                        @RequestParam BigDecimal upgradeAmount) {
        return vipService.upgradeVip(userId, targetLevel, upgradeAmount);
    }

    /**
     * 取消自动续费
     */
    @PostMapping("/me/cancel-auto-renew")
    public Result<Void> cancelAutoRenew(@RequestAttribute("userId") Long userId) {
        return vipService.cancelAutoRenew(userId);
    }

    /**
     * 开启自动续费
     */
    @PostMapping("/me/enable-auto-renew")
    public Result<Void> enableAutoRenew(@RequestAttribute("userId") Long userId) {
        return vipService.enableAutoRenew(userId);
    }

    /**
     * 获取VIP历史记录
     */
    @GetMapping("/me/history")
    public Result<List<VipInfoDTO>> getVipHistory(@RequestAttribute("userId") Long userId) {
        return vipService.getVipHistory(userId);
    }

    // ==================== 管理员接口 ====================

    /**
     * 获取用户VIP信息（管理员）
     */
    @GetMapping("/user/{userId}")
    public Result<VipInfoDTO> getUserVipInfo(@PathVariable Long userId) {
        return vipService.getUserVipInfo(userId);
    }

    /**
     * 为用户开通VIP（管理员）
     */
    @PostMapping("/admin/{userId}/grant")
    public Result<VipInfoDTO> grantVip(@PathVariable Long userId,
                                      @RequestParam Integer vipLevel,
                                      @RequestParam Integer subscribeType) {
        return vipService.subscribeVip(userId, vipLevel, subscribeType, BigDecimal.ZERO, 3, "管理员赠送");
    }

    /**
     * 取消用户VIP（管理员）
     */
    @PostMapping("/admin/{userId}/revoke")
    public Result<Void> revokeVip(@PathVariable Long userId) {
        VipMembership vip = vipMembershipMapper.selectValidByUserId(userId);
        if (vip != null) {
            vip.setStatus(0);
            vip.setUpdatedAt(LocalDateTime.now());
            vipMembershipMapper.updateById(vip);
        }
        return Result.success();
    }
}
