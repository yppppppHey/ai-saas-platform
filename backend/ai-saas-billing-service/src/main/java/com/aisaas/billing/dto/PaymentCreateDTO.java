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
 * 支付创建DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentCreateDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 用户ID */
    private Long userId;

    /** 账单ID */
    private String billId;

    /** 支付类型 */
    private Integer paymentType;

    /** 支付渠道 */
    private Integer channel;

    /** 金额 */
    private BigDecimal amount;

    /** 币种 */
    private String currency;

    /** 备注 */
    private String remark;
}
