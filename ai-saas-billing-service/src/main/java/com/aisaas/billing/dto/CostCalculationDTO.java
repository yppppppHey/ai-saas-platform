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
 * 费用计算DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CostCalculationDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 服务商 */
    private String provider;

    /** 模型ID */
    private String modelId;

    /** 操作类型 */
    private String operationType;

    /** 输入Token数 */
    private Integer promptTokens;

    /** 输出Token数 */
    private Integer completionTokens;
}
