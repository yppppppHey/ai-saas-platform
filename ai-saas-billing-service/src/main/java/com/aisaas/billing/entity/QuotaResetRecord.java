package com.aisaas.billing.entity;

import com.aisaas.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("billing_quota_reset_record")
public class QuotaResetRecord extends BaseEntity {
    private static final long serialVersionUID = 1L;

    private String recordId;
    private Long userId;
    private Integer quotaType;
    private Long dailyLimit;
    private Long monthlyLimit;
    private Long totalLimit;
    private Long dailyUsed;
    private Long monthlyUsed;
    private Long totalUsed;
    private LocalDate lastResetDate;
    private Integer resetDay;
    private LocalDateTime effectiveAt;
    private LocalDateTime expireAt;
    private Integer status;

    /** 重置类型: 1-日 2-月 3-全部 */
    private Integer resetType;
    /** 操作人 */
    private String operator;
    /** 重置时间 */
    private LocalDateTime resetAt;
}
