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
 * 账单统计VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BillStatisticsVO implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 总金额 */
    private BigDecimal totalAmount;

    /** 已付金额 */
    private BigDecimal paidAmount;

    /** 未付金额 */
    private BigDecimal unpaidAmount;

    /** 总数 */
    private Integer totalCount;

    /** 已付数 */
    private Integer paidCount;

    /** 未付数 */
    private Integer unpaidCount;
}
