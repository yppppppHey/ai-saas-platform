package com.aisaas.billing.entity;

import com.aisaas.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 价格配置实体类
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("billing_price_config")
public class PriceConfig extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 配置ID */
    private String configId;

    /** 提供商: openai/deepseek/claude */
    private String provider;

    /** 模型ID */
    private String modelId;

    /** 模型名称 */
    private String modelName;

    /** 操作类型: chat/completion/embedding */
    private String operationType;

    /** 输入价格(每1K tokens, USD) */
    private BigDecimal inputPrice;

    /** 输出价格(每1K tokens, USD) */
    private BigDecimal outputPrice;

    /** 批量输入价格折扣 */
    private BigDecimal batchDiscount;

    /** 计费精度: 0-整数 1-1位小数 2-2位小数 3-3位小数 */
    private Integer precision;

    /** 计费舍入方式: up/down/half_up */
    private String roundingMode;

    /** 生效时间 */
    private LocalDateTime effectiveAt;

    /** 过期时间 */
    private LocalDateTime expireAt;

    /** 状态: 0-禁用 1-启用 */
    private Integer status;

    /** 备注 */
    private String remark;
}
