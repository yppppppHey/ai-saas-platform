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
 * 支付创建结果VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentCreateResultVO implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 支付ID */
    private String paymentId;

    /** 支付单号 */
    private String paymentNo;

    /** 金额 */
    private BigDecimal amount;

    /** 状态 */
    private Integer status;

    /** 支付链接 */
    private String payUrl;

    /** 过期时间 */
    private LocalDateTime expiredAt;
}
