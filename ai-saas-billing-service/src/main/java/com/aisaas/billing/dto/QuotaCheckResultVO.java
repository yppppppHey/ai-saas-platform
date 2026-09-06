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
 * 配额检查结果VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuotaCheckResultVO implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 用户ID */
    private Long userId;

    /** 配额类型 */
    private Integer quotaType;

    /** 所需额度 */
    private Long requiredAmount;

    /** 每日限额 */
    private Long dailyLimit;

    /** 每日已用 */
    private Long dailyUsed;

    /** 每月限额 */
    private Long monthlyLimit;

    /** 每月已用 */
    private Long monthlyUsed;

    /** 总量限额 */
    private Long totalLimit;

    /** 总量已用 */
    private Long totalUsed;

    /** 是否可用 */
    private Boolean available;

    /** 原因 */
    private String reason;
}
