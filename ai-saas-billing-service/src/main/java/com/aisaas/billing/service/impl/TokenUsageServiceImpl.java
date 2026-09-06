package com.aisaas.billing.service.impl;

import com.aisaas.billing.dto.*;
import com.aisaas.billing.entity.PriceConfig;
import com.aisaas.billing.entity.TokenUsageRecord;
import com.aisaas.billing.mapper.PriceConfigMapper;
import com.aisaas.billing.mapper.TokenUsageRecordMapper;
import com.aisaas.billing.service.TokenUsageService;
import com.aisaas.common.result.Result;
import com.aisaas.common.constant.ResultCode;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
public class TokenUsageServiceImpl implements TokenUsageService {

    @Autowired
    private TokenUsageRecordMapper tokenUsageRecordMapper;

    @Autowired
    private PriceConfigMapper priceConfigMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> recordTokenUsage(TokenUsageRecordDTO dto) {
        try {
            // 计算成本
            CostCalculationResult costResult = calculateCostInternal(dto.getProvider(), dto.getModelId(),
                    dto.getOperationType(), dto.getPromptTokens(), dto.getCompletionTokens());

            // 创建记录
            TokenUsageRecord record = new TokenUsageRecord();
            BeanUtils.copyProperties(dto, record);
            record.setUsageId(generateUsageId());
            record.setPromptCost(costResult.getPromptCost());
            record.setCompletionCost(costResult.getCompletionCost());
            record.setTotalCost(costResult.getTotalCost());
            record.setCostCny(costResult.getTotalCost().multiply(new BigDecimal("7.2")));
            record.setExchangeRate(new BigDecimal("7.2"));
            record.setIsBilled(0);
            record.setUsageDate(LocalDate.now());
            record.setUsageHour(LocalDateTime.now().getHour());
            record.setCreatedAt(LocalDateTime.now());

            tokenUsageRecordMapper.insert(record);

            log.info("Token使用记录已保存: usageId={}, userId={}, totalTokens={}",
                    record.getUsageId(), record.getUserId(), record.getTotalTokens());

            return Result.success();
        } catch (Exception e) {
            log.error("记录Token使用失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR, "记录Token使用失败: " + e.getMessage());
        }
    }

    /**
     * 幂等记录 Token 使用（供 MQ 消费端调用）
     *
     * 三重防重：
     * 1. 调用方 IdempotentMessageHandler 的 Redis 标记（拦截重复投递）
     * 2. 按 usageId 查库（拦截 Redis 标记过期的极端情况）
     * 3. billing_token_usage.uk_usage_id 唯一键（并发兜底，冲突视为成功）
     */
    public Result<Void> recordTokenUsageIdempotent(TokenUsageRecordDTO dto, String usageId) {
        // 2. DB 查重（usage_id 唯一键的前置检查，减少冲突异常）
        LambdaQueryWrapper<TokenUsageRecord> dupWrapper = new LambdaQueryWrapper<>();
        dupWrapper.eq(TokenUsageRecord::getUsageId, usageId);
        if (tokenUsageRecordMapper.selectCount(dupWrapper) > 0) {
            log.info("Token使用记录已存在(幂等命中), usageId={}", usageId);
            return Result.success();
        }

        try {
            CostCalculationResult costResult = calculateCostInternal(dto.getProvider(), dto.getModelId(),
                    dto.getOperationType(), dto.getPromptTokens(), dto.getCompletionTokens());

            TokenUsageRecord record = new TokenUsageRecord();
            BeanUtils.copyProperties(dto, record);
            record.setUsageId(usageId);  // 关键：沿用消息携带的幂等键
            record.setPromptCost(costResult.getPromptCost());
            record.setCompletionCost(costResult.getCompletionCost());
            record.setTotalCost(costResult.getTotalCost());
            record.setCostCny(costResult.getTotalCost().multiply(new BigDecimal("7.2")));
            record.setExchangeRate(new BigDecimal("7.2"));
            record.setIsBilled(1);
            record.setBilledAt(LocalDateTime.now());
            record.setUsageDate(LocalDate.now());
            record.setUsageHour(LocalDateTime.now().getHour());
            record.setCreatedAt(LocalDateTime.now());

            tokenUsageRecordMapper.insert(record);
            log.info("Token使用记录已入账: usageId={}, userId={}, totalTokens={}",
                    usageId, record.getUserId(), record.getTotalTokens());
            return Result.success();
        } catch (org.springframework.dao.DuplicateKeyException e) {
            // 3. 唯一键冲突 = 并发重复消费，视为成功
            log.info("Token使用记录并发重复(唯一键兜底), usageId={}", usageId);
            return Result.success();
        } catch (Exception e) {
            log.error("记录Token使用失败, usageId={}", usageId, e);
            return Result.error(ResultCode.SYSTEM_ERROR, "记录Token使用失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> batchRecordTokenUsage(BatchTokenUsageDTO dto) {
        try {
            for (TokenUsageRecordDTO record : dto.getRecords()) {
                Result<Void> result = recordTokenUsage(record);
                if (!result.isSuccess()) {
                    return result;
                }
            }
            return Result.success();
        } catch (Exception e) {
            log.error("批量记录Token使用失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR, "批量记录Token使用失败: " + e.getMessage());
        }
    }

    @Override
    public Result<IPage<TokenUsageRecordVO>> queryUserTokenUsage(TokenUsageQueryDTO dto) {
        try {
            Page<TokenUsageRecord> page = new Page<>(dto.getPageNum(), dto.getPageSize());
            IPage<TokenUsageRecord> recordPage = tokenUsageRecordMapper.selectPageByUserId(page, dto.getUserId());

            // 转换为VO
            List<TokenUsageRecordVO> voList = recordPage.getRecords().stream()
                    .map(this::convertToVO)
                    .collect(Collectors.toList());

            Page<TokenUsageRecordVO> voPage = new Page<>(recordPage.getCurrent(), recordPage.getSize(), recordPage.getTotal());
            voPage.setRecords(voList);

            return Result.success(voPage);
        } catch (Exception e) {
            log.error("查询Token使用记录失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR, "查询Token使用记录失败: " + e.getMessage());
        }
    }

    @Override
    public Result<TokenUsageStatisticsVO> getTokenUsageStatistics(TokenUsageStatisticsQueryDTO dto) {
        try {
            Map<String, Object> stats = tokenUsageRecordMapper.selectStatistics(
                    dto.getUserId(), dto.getStartDate(), dto.getEndDate(),
                    dto.getProvider(), dto.getModelId());

            TokenUsageStatisticsVO vo = new TokenUsageStatisticsVO();
            vo.setCount(stats.get("count") != null ? ((Number) stats.get("count")).longValue() : 0L);
            vo.setTotalTokens(stats.get("totalTokens") != null ? ((Number) stats.get("totalTokens")).longValue() : 0L);
            vo.setTotalCost(stats.get("totalCost") != null ? (BigDecimal) stats.get("totalCost") : BigDecimal.ZERO);
            vo.setTotalCostCny(stats.get("totalCostCny") != null ? (BigDecimal) stats.get("totalCostCny") : BigDecimal.ZERO);

            if (vo.getCount() > 0) {
                vo.setAvgTokens(vo.getTotalTokens() / vo.getCount());
                vo.setAvgCost(vo.getTotalCost().divide(new BigDecimal(vo.getCount()), 8, RoundingMode.HALF_UP));
            } else {
                vo.setAvgTokens(0L);
                vo.setAvgCost(BigDecimal.ZERO);
            }

            return Result.success(vo);
        } catch (Exception e) {
            log.error("获取Token使用统计失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR, "获取Token使用统计失败: " + e.getMessage());
        }
    }

    @Override
    public Result<DailyStatisticsVO> getDailyStatistics(Long userId, LocalDate startDate, LocalDate endDate) {
        try {
            List<Map<String, Object>> list = tokenUsageRecordMapper.selectDailyStatistics(userId, startDate, endDate);
            
            DailyStatisticsVO vo = new DailyStatisticsVO();
            List<DailyStatisticsItemVO> items = new ArrayList<>();
            
            for (Map<String, Object> map : list) {
                DailyStatisticsItemVO item = new DailyStatisticsItemVO();
                item.setDate((LocalDate) map.get("date"));
                item.setCount(((Number) map.get("count")).longValue());
                item.setTokens(((Number) map.get("tokens")).longValue());
                item.setCost((BigDecimal) map.get("cost"));
                items.add(item);
            }
            
            vo.setItems(items);
            vo.setTotalCount(items.size());
            
            return Result.success(vo);
        } catch (Exception e) {
            log.error("获取每日统计失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR, "获取每日统计失败: " + e.getMessage());
        }
    }

    @Override
    public Result<ModelStatisticsVO> getModelStatistics(Long userId, LocalDate startDate, LocalDate endDate) {
        try {
            List<Map<String, Object>> list = tokenUsageRecordMapper.selectModelStatistics(userId, startDate, endDate);
            
            ModelStatisticsVO vo = new ModelStatisticsVO();
            List<ModelStatisticsItemVO> items = new ArrayList<>();
            
            for (Map<String, Object> map : list) {
                ModelStatisticsItemVO item = new ModelStatisticsItemVO();
                item.setProvider((String) map.get("provider"));
                item.setModelId((String) map.get("modelId"));
                item.setCount(((Number) map.get("count")).longValue());
                item.setTokens(((Number) map.get("tokens")).longValue());
                item.setCost((BigDecimal) map.get("cost"));
                items.add(item);
            }
            
            vo.setItems(items);
            vo.setTotalCount(items.size());
            
            return Result.success(vo);
        } catch (Exception e) {
            log.error("获取模型统计失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR, "获取模型统计失败: " + e.getMessage());
        }
    }

    @Override
    public Result<OperationTypeStatisticsVO> getOperationTypeStatistics(Long userId, LocalDate startDate, LocalDate endDate) {
        try {
            List<Map<String, Object>> list = tokenUsageRecordMapper.selectOperationTypeStatistics(userId, startDate, endDate);
            
            OperationTypeStatisticsVO vo = new OperationTypeStatisticsVO();
            List<OperationTypeStatisticsItemVO> items = new ArrayList<>();
            
            for (Map<String, Object> map : list) {
                OperationTypeStatisticsItemVO item = new OperationTypeStatisticsItemVO();
                item.setOperationType((String) map.get("operationType"));
                item.setCount(((Number) map.get("count")).longValue());
                item.setTokens(((Number) map.get("tokens")).longValue());
                item.setCost((BigDecimal) map.get("cost"));
                items.add(item);
            }
            
            vo.setItems(items);
            vo.setTotalCount(items.size());
            
            return Result.success(vo);
        } catch (Exception e) {
            log.error("获取操作类型统计失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR, "获取操作类型统计失败: " + e.getMessage());
        }
    }

    @Override
    public Result<CostCalculationVO> calculateCost(CostCalculationDTO dto) {
        try {
            CostCalculationResult result = calculateCostInternal(
                    dto.getProvider(), dto.getModelId(), dto.getOperationType(),
                    dto.getPromptTokens(), dto.getCompletionTokens());

            CostCalculationVO vo = new CostCalculationVO();
            vo.setPromptCost(result.getPromptCost());
            vo.setCompletionCost(result.getCompletionCost());
            vo.setTotalCost(result.getTotalCost());
            vo.setCostCny(result.getTotalCost().multiply(new BigDecimal("7.2")));
            vo.setExchangeRate(new BigDecimal("7.2"));

            return Result.success(vo);
        } catch (Exception e) {
            log.error("计算费用失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR, "计算费用失败: " + e.getMessage());
        }
    }

    @Override
    public Result<BigDecimal> getUnbilledAmount(Long userId) {
        try {
            BigDecimal amount = tokenUsageRecordMapper.selectUnbilledAmount(userId);
            return Result.success(amount != null ? amount : BigDecimal.ZERO);
        } catch (Exception e) {
            log.error("获取未计费金额失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR, "获取未计费金额失败: " + e.getMessage());
        }
    }

    @Override
    public Result<String> exportUsageRecords(TokenUsageExportDTO dto) {
        // TODO: 实现导出逻辑
        return Result.success("");
    }

    // ==================== 私有方法 ====================

    private String generateUsageId() {
        return "USG" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) +
                UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private TokenUsageRecordVO convertToVO(TokenUsageRecord record) {
        TokenUsageRecordVO vo = new TokenUsageRecordVO();
        BeanUtils.copyProperties(record, vo);
        return vo;
    }

    private CostCalculationResult calculateCostInternal(String provider, String modelId,
                                                        String operationType, Integer promptTokens,
                                                        Integer completionTokens) {
        CostCalculationResult result = new CostCalculationResult();

        // 查询价格配置
        PriceConfig config = priceConfigMapper.selectValidConfig(provider, modelId, operationType, LocalDateTime.now());

        if (config != null) {
            // 计算输入成本
            BigDecimal promptCost = config.getInputPrice()
                    .multiply(new BigDecimal(promptTokens))
                    .divide(new BigDecimal(1000), 8, RoundingMode.HALF_UP);

            // 计算输出成本
            BigDecimal completionCost = config.getOutputPrice()
                    .multiply(new BigDecimal(completionTokens))
                    .divide(new BigDecimal(1000), 8, RoundingMode.HALF_UP);

            result.setPromptCost(promptCost);
            result.setCompletionCost(completionCost);
            result.setTotalCost(promptCost.add(completionCost));
        } else {
            // 没有价格配置，成本为0
            result.setPromptCost(BigDecimal.ZERO);
            result.setCompletionCost(BigDecimal.ZERO);
            result.setTotalCost(BigDecimal.ZERO);
        }

        return result;
    }

    // 内部类
    private static class CostCalculationResult {
        private BigDecimal promptCost;
        private BigDecimal completionCost;
        private BigDecimal totalCost;

        public BigDecimal getPromptCost() {
            return promptCost;
        }

        public void setPromptCost(BigDecimal promptCost) {
            this.promptCost = promptCost;
        }

        public BigDecimal getCompletionCost() {
            return completionCost;
        }

        public void setCompletionCost(BigDecimal completionCost) {
            this.completionCost = completionCost;
        }

        public BigDecimal getTotalCost() {
            return totalCost;
        }

        public void setTotalCost(BigDecimal totalCost) {
            this.totalCost = totalCost;
        }
    }
}
