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
 * 配额重置记录VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuotaResetRecordVO implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 记录ID */
    private String recordId;

    /** 用户ID */
    private Long userId;

    /** 配额类型 */
    private Integer quotaType;

    /** 每日限额 */
    private Long dailyLimit;

    /** 每月限额 */
    private Long monthlyLimit;

    /** 总量限额 */
    private Long totalLimit;

    /** 每日已用 */
    private Long dailyUsed;

    /** 每月已用 */
    private Long monthlyUsed;

    /** 总量已用 */
    private Long totalUsed;

    /** 上次重置日期 */
    private LocalDate lastResetDate;

    /** 重置日 */
    private Integer resetDay;

    /** 生效时间 */
    private LocalDateTime effectiveAt;

    /** 失效时间 */
    private LocalDateTime expireAt;

    /** 状态 */
    private Integer status;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
