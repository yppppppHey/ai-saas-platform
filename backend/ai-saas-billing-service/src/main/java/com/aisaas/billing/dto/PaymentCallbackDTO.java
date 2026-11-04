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
 * 支付回调DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentCallbackDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 支付单号 */
    private String paymentNo;

    /** 第三方流水号 */
    private String thirdTradeNo;

    /** 状态 */
    private Integer status;

    /** 金额 */
    private BigDecimal amount;

    /** 支付时间 */
    private LocalDateTime paidAt;

    /** 回调信息 */
    private String callbackInfo;

    /** 签名 */
    private String sign;
}
