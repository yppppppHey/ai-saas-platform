package com.aisaas.user.controller;

import com.aisaas.common.result.Result;
import com.aisaas.user.dto.QuotaDTO;
import com.aisaas.user.service.QuotaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 配额控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/quota")
@RequiredArgsConstructor
public class QuotaController {

    private final QuotaService quotaService;

    /**
     * 获取用户的所有配额信息
     */
    @GetMapping("/me")
    public Result<List<QuotaDTO>> getMyQuotas(@RequestAttribute("userId") Long userId) {
        return quotaService.getUserAllQuotas(userId);
    }

    /**
     * 获取指定类型的配额信息
     */
    @GetMapping("/me/{quotaType}")
    public Result<QuotaDTO> getMyQuota(@RequestAttribute("userId") Long userId,
                                        @PathVariable Integer quotaType) {
        return quotaService.getUserQuota(userId, quotaType);
    }

    /**
     * 获取指定用户的配额信息（管理员）
     */
    @GetMapping("/user/{userId}")
    public Result<List<QuotaDTO>> getUserQuotas(@PathVariable Long userId) {
        return quotaService.getUserAllQuotas(userId);
    }

    /**
     * 增加用户配额（管理员）
     */
    @PostMapping("/user/{userId}/add")
    public Result<Void> addQuota(@PathVariable Long userId,
                                  @RequestParam Integer quotaType,
                                  @RequestParam Long amount) {
        return quotaService.addQuota(userId, quotaType, amount);
    }

    /**
     * 设置配额限制（管理员）
     */
    @PutMapping("/user/{userId}/limit")
    public Result<Void> setQuotaLimit(@PathVariable Long userId,
                                       @RequestParam Integer quotaType,
                                       @RequestParam(required = false) Long dailyLimit,
                                       @RequestParam(required = false) Long monthlyLimit,
                                       @RequestParam(required = false) Long totalLimit) {
        return quotaService.setQuotaLimit(userId, quotaType, dailyLimit, monthlyLimit, totalLimit);
    }

    /**
     * 检查配额（内部使用）
     */
    @GetMapping("/check")
    public Result<Boolean> checkQuota(@RequestParam Long userId,
                                      @RequestParam Integer quotaType,
                                      @RequestParam Long requiredAmount) {
        return Result.success(quotaService.checkQuota(userId, quotaType, requiredAmount));
    }

    /**
     * 扣减配额（内部使用）
     */
    @PostMapping("/deduct")
    public Result<Void> deductQuota(@RequestParam Long userId,
                                    @RequestParam Integer quotaType,
                                    @RequestParam Long amount) {
        return quotaService.deductQuota(userId, quotaType, amount);
    }
}
