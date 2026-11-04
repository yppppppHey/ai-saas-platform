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
 * 发票VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceVO implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 发票ID */
    private String invoiceId;

    /** 用户ID */
    private Long userId;

    /** 发票号 */
    private String invoiceNo;

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

    /** 状态 */
    private Integer status;

    /** 申请时间 */
    private LocalDateTime appliedAt;

    /** 开具时间 */
    private LocalDateTime issuedAt;

    /** 关联账单 */
    private String billIds;

    /** 备注 */
    private String remark;

    /** 拒绝原因 */
    private String rejectReason;
}
