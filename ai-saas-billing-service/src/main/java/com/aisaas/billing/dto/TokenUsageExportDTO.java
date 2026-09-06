package com.aisaas.billing.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 使用记录导出DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenUsageExportDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 用户ID */
    private Long userId;

    /** 起始日期 */
    private LocalDate startDate;

    /** 截止日期 */
    private LocalDate endDate;

    /** 服务商 */
    private String provider;

    /** 模型ID */
    private String modelId;

    /** 操作类型 */
    private String operationType;
}
