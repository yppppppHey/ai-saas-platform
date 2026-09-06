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
 * 模型统计明细VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelStatisticsItemVO implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 服务商 */
    private String provider;

    /** 模型ID */
    private String modelId;

    /** 次数 */
    private Long count;

    /** Token数 */
    private Long tokens;

    /** 费用 */
    private BigDecimal cost;
}
