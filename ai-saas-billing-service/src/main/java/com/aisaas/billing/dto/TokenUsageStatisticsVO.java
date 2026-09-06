package com.aisaas.billing.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * Token使用统计VO
 */
@Data
public class TokenUsageStatisticsVO {

    private Long count;
    private Long totalTokens;
    private BigDecimal totalCost;
    private BigDecimal totalCostCny;
    private Long avgTokens;
    private BigDecimal avgCost;
}
