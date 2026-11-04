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
 * 账单VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BillVO implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 账单ID */
    private String billId;

    /** 用户ID */
    private Long userId;

    /** 账单编号 */
    private String billNo;

    /** 账单类型 */
    private Integer billType;

    /** 账单周期 */
    private Integer billPeriod;

    /** 账单起始日 */
    private LocalDate billStartDate;

    /** 账单截止日 */
    private LocalDate billEndDate;

    /** 支付截止日 */
    private LocalDate billDueDate;

    /** 总金额 */
    private BigDecimal totalAmount;

    /** 折扣金额 */
    private BigDecimal discountAmount;

    /** 应付金额 */
    private BigDecimal payableAmount;

    /** 已付金额 */
    private BigDecimal paidAmount;

    /** 税额 */
    private BigDecimal taxAmount;

    /** 状态 */
    private Integer status;

    /** 支付时间 */
    private LocalDateTime paidAt;

    /** 支付方式 */
    private String payMethod;

    /** 支付流水号 */
    private String payTradeNo;

    /** 备注 */
    private String remark;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
