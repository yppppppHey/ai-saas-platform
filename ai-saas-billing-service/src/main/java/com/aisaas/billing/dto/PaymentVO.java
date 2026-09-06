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
 * 支付VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentVO implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 支付ID */
    private String paymentId;

    /** 用户ID */
    private Long userId;

    /** 账单ID */
    private String billId;

    /** 支付单号 */
    private String paymentNo;

    /** 支付类型 */
    private Integer paymentType;

    /** 支付渠道 */
    private Integer channel;

    /** 渠道名称 */
    private String channelName;

    /** 金额 */
    private BigDecimal amount;

    /** 手续费 */
    private BigDecimal fee;

    /** 币种 */
    private String currency;

    /** 状态 */
    private Integer status;

    /** 支付时间 */
    private LocalDateTime paidAt;

    /** 过期时间 */
    private LocalDateTime expiredAt;

    /** 第三方流水号 */
    private String thirdTradeNo;

    /** 支付人信息 */
    private String payerInfo;

    /** 回调信息 */
    private String callbackInfo;

    /** 备注 */
    private String remark;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
