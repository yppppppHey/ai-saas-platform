package com.aisaas.billing.controller;

import com.aisaas.billing.dto.*;
import com.aisaas.billing.service.TokenUsageService;
import com.aisaas.common.result.Result;
import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Token使用记录控制器
 */
@Slf4j
@RestController
@RequestMapping("/billing/token")
public class TokenUsageController {

    @Autowired
    private TokenUsageService tokenUsageService;

    /**
     * 记录Token使用
     */
    @PostMapping("/record")
    public Result<Void> recordTokenUsage(@RequestBody @Validated TokenUsageRecordDTO dto) {
        return tokenUsageService.recordTokenUsage(dto);
    }

    /**
     * 批量记录Token使用
     */
    @PostMapping("/batch-record")
    public Result<Void> batchRecordTokenUsage(@RequestBody @Validated BatchTokenUsageDTO dto) {
        return tokenUsageService.batchRecordTokenUsage(dto);
    }

    /**
     * 分页查询Token使用记录
     */
    @GetMapping("/list")
    public Result<IPage<TokenUsageRecordVO>> queryTokenUsage(@Validated TokenUsageQueryDTO dto) {
        return tokenUsageService.queryUserTokenUsage(dto);
    }

    /**
     * 获取Token使用统计
     */
    @GetMapping("/statistics")
    public Result<TokenUsageStatisticsVO> getStatistics(@Validated TokenUsageStatisticsQueryDTO dto) {
        return tokenUsageService.getTokenUsageStatistics(dto);
    }

    /**
     * 获取每日统计
     */
    @GetMapping("/statistics/daily")
    public Result<DailyStatisticsVO> getDailyStatistics(
            @RequestParam Long userId,
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate) {
        return tokenUsageService.getDailyStatistics(userId, startDate, endDate);
    }

    /**
     * 获取模型统计
     */
    @GetMapping("/statistics/model")
    public Result<ModelStatisticsVO> getModelStatistics(
            @RequestParam Long userId,
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate) {
        return tokenUsageService.getModelStatistics(userId, startDate, endDate);
    }

    /**
     * 获取操作类型统计
     */
    @GetMapping("/statistics/operation")
    public Result<OperationTypeStatisticsVO> getOperationTypeStatistics(
            @RequestParam Long userId,
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate) {
        return tokenUsageService.getOperationTypeStatistics(userId, startDate, endDate);
    }

    /**
     * 计算费用
     */
    @PostMapping("/calculate-cost")
    public Result<CostCalculationVO> calculateCost(@RequestBody @Validated CostCalculationDTO dto) {
        return tokenUsageService.calculateCost(dto);
    }

    /**
     * 获取未计费金额
     */
    @GetMapping("/unbilled-amount/{userId}")
    public Result<BigDecimal> getUnbilledAmount(@PathVariable Long userId) {
        return tokenUsageService.getUnbilledAmount(userId);
    }
}
