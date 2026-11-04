package com.aisaas.billing.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 退款状态VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundStatusVO implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 退款ID */
    private String refundId;

    /** 支付ID */
    private String paymentId;

    /** 退款金额 */
    private BigDecimal refundAmount;

    /** 状态 */
    private Integer status;

    /** 退款原因 */
    private String reason;

    /** 退款时间 */
    private LocalDateTime refundedAt;
}
