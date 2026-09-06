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
 * 配额配置VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuotaConfigVO implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 配置ID */
    private String configId;

    /** 状态 */
    private Integer status;

    /** 配置名称 */
    private String configName;

    /** 配额类型 */
    private Integer quotaType;

    /** 用户类型 */
    private Integer userType;

    /** 每日限额 */
    private Long dailyLimit;

    /** 每月限额 */
    private Long monthlyLimit;

    /** 总量限额 */
    private Long totalLimit;

    /** 超额动作 */
    private Integer exceedAction;

    /** 超额阈值 */
    private Integer exceedThreshold;

    /** 重置周期 */
    private String resetCycle;

    /** 重置日 */
    private Integer resetDay;

    /** 生效时间 */
    private LocalDateTime effectiveAt;

    /** 失效时间 */
    private LocalDateTime expireAt;

    /** 备注 */
    private String remark;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
