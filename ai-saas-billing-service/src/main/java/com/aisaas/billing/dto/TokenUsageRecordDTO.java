package com.aisaas.billing.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * Token使用记录DTO
 */
@Data
public class TokenUsageRecordDTO {

    private Long userId;
    private Long conversationId;
    private Long messageId;
    private Long taskId;
    private String provider;
    private String modelId;
    private String operationType;
    private Integer promptTokens;
    private Integer completionTokens;
    private Integer totalTokens;
    private BigDecimal promptCost;
    private BigDecimal completionCost;
    private BigDecimal totalCost;
    private BigDecimal costCny;
    private Integer latencyMs;
    private String errorCode;
    private String errorMsg;
    private String traceId;
}
