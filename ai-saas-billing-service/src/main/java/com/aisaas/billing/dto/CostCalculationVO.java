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
 * 费用计算VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CostCalculationVO implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 输入费用 */
    private BigDecimal promptCost;

    /** 输出费用 */
    private BigDecimal completionCost;

    /** 总费用 */
    private BigDecimal totalCost;

    /** 人民币费用 */
    private BigDecimal costCny;

    /** 汇率 */
    private BigDecimal exchangeRate;
}
