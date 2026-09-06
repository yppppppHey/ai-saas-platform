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
 * 发票抬头保存DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceTitleSaveDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 用户ID */
    private Long userId;

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

    /** 是否默认 */
    private Boolean isDefault;
}
