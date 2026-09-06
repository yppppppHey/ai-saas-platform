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
 * 账单生成DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BillGenerateDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 用户ID */
    private Long userId;

    /** 账单类型 */
    private Integer billType;

    /** 账单周期 */
    private Integer billPeriod;

    /** 起始日期 */
    private LocalDate startDate;

    /** 截止日期 */
    private LocalDate endDate;

    /** 支付截止日期 */
    private LocalDate dueDate;
}
