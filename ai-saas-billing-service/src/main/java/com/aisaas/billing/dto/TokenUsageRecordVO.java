package com.aisaas.billing.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Token使用记录VO
 */
@Data
public class TokenUsageRecordVO {

    private String usageId;
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
    private Integer isBilled;
    private LocalDateTime billedAt;
    private LocalDate usageDate;
    private Integer usageHour;
    private String traceId;
    private LocalDateTime createdAt;
}
