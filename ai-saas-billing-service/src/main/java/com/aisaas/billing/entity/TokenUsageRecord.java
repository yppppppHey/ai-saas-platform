package com.aisaas.billing.entity;

import com.aisaas.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Token消费记录实体类
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("billing_token_usage")
public class TokenUsageRecord extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 消费记录ID */
    private String usageId;

    /** 用户ID */
    private Long userId;

    /** 关联会话ID */
    private Long conversationId;

    /** 关联消息ID */
    private Long messageId;

    /** 关联任务ID */
    private Long taskId;

    /** 提供商: openai/deepseek/claude */
    private String provider;

    /** 模型ID */
    private String modelId;

    /** 操作类型: chat/completion/embedding */
    private String operationType;

    /** 输入token数 */
    private Integer promptTokens;

    /** 输出token数 */
    private Integer completionTokens;

    /** 总token数 */
    private Integer totalTokens;

    /** 输入成本(USD) */
    private BigDecimal promptCost;

    /** 输出成本(USD) */
    private BigDecimal completionCost;

    /** 总成本(USD) */
    private BigDecimal totalCost;

    /** 汇率(USD to CNY) */
    private BigDecimal exchangeRate;

    /** 成本(CNY) */
    private BigDecimal costCny;

    /** 延迟(毫秒) */
    private Integer latencyMs;

    /** 错误码 */
    private String errorCode;

    /** 错误信息 */
    private String errorMsg;

    /** 是否计费: 0-否 1-是 */
    private Integer isBilled;

    /** 计费时间 */
    private LocalDateTime billedAt;

    /** 使用日期(用于分表) */
    private LocalDate usageDate;

    /** 使用小时 */
    private Integer usageHour;

    /** 链路追踪ID */
    private String traceId;
}
