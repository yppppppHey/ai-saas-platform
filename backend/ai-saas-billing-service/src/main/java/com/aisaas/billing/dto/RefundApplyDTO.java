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
 * 退款申请DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundApplyDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 支付ID */
    private String paymentId;

    /** 退款金额 */
    private BigDecimal refundAmount;

    /** 退款原因 */
    private String reason;

    /** 备注 */
    private String remark;
}
