package com.aisaas.user.dto;

import lombok.Data;

/**
 * 配额信息DTO
 */
@Data
public class QuotaDTO {

    /**
     * 配额类型: 1-对话token 2-任务次数 3-存储空间
     */
    private Integer quotaType;

    /**
     * 配额类型名称
     */
    private String quotaTypeName;

    /**
     * 每日限制
     */
    private Long dailyLimit;

    /**
     * 每月限制
     */
    private Long monthlyLimit;

    /**
     * 总计限制
     */
    private Long totalLimit;

    /**
     * 今日已使用
     */
    private Long dailyUsed;

    /**
     * 本月已使用
     */
    private Long monthlyUsed;

    /**
     * 总计已使用
     */
    private Long totalUsed;

    /**
     * 每日剩余
     */
    private Long dailyRemaining;

    /**
     * 每月剩余
     */
    private Long monthlyRemaining;

    /**
     * 是否已超出配额
     */
    private Boolean exceeded;
}
