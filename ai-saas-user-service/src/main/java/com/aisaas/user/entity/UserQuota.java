package com.aisaas.user.entity;

import com.aisaas.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 用户配额实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user_quota")
public class UserQuota extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 用户ID
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 配额类型: 1-对话token 2-任务次数 3-存储空间
     */
    @TableField("quota_type")
    private Integer quotaType;

    /**
     * 每日限制(0为无限制)
     */
    @TableField("daily_limit")
    private Long dailyLimit;

    /**
     * 每月限制(0为无限制)
     */
    @TableField("monthly_limit")
    private Long monthlyLimit;

    /**
     * 总计限制(0为无限制)
     */
    @TableField("total_limit")
    private Long totalLimit;

    /**
     * 今日已使用
     */
    @TableField("daily_used")
    private Long dailyUsed;

    /**
     * 本月已使用
     */
    @TableField("monthly_used")
    private Long monthlyUsed;

    /**
     * 总计已使用
     */
    @TableField("total_used")
    private Long totalUsed;

    /**
     * 每月重置日
     */
    @TableField("reset_day")
    private Integer resetDay;

    /**
     * 生效时间
     */
    @TableField("effective_at")
    private LocalDateTime effectiveAt;

    /**
     * 过期时间
     */
    @TableField("expire_at")
    private LocalDateTime expireAt;

    /**
     * 是否超出配额
     */
    public boolean isExceeded() {
        if (dailyLimit > 0 && dailyUsed >= dailyLimit) {
            return true;
        }
        if (monthlyLimit > 0 && monthlyUsed >= monthlyLimit) {
            return true;
        }
        if (totalLimit > 0 && totalUsed >= totalLimit) {
            return true;
        }
        return false;
    }

    /**
     * 检查并扣减配额
     */
    public boolean deduct(long amount) {
        if (isExceeded()) {
            return false;
        }
        this.dailyUsed += amount;
        this.monthlyUsed += amount;
        this.totalUsed += amount;
        return true;
    }
}
