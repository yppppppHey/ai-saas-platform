package com.aisaas.billing.dto;

import lombok.Data;

import java.time.LocalDate;

/**
 * Token使用统计查询DTO
 */
@Data
public class TokenUsageStatisticsQueryDTO {

    private Long userId;
    private LocalDate startDate;
    private LocalDate endDate;
    private String provider;
    private String modelId;
    private String operationType;
}
