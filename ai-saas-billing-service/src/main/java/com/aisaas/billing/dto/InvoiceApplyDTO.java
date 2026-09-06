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
 * 发票申请DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceApplyDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 用户ID */
    private Long userId;

    /** 发票类型 */
    private Integer invoiceType;

    /** 抬头 */
    private String title;

    /** 税号 */
    private String taxNo;

    /** 开户银行 */
    private String bankName;

    /** 银行账号 */
    private String bankAccount;

    /** 地址 */
    private String address;

    /** 电话 */
    private String phone;

    /** 邮箱 */
    private String email;

    /** 金额 */
    private BigDecimal amount;

    /** 关联账单ID */
    private String billIds;

    /** 备注 */
    private String remark;
}
