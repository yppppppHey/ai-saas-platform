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
 * 价格配置创建DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PriceConfigCreateDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 服务商 */
    private String provider;

    /** 模型ID */
    private String modelId;

    /** 模型名称 */
    private String modelName;

    /** 操作类型 */
    private String operationType;

    /** 输入价格 */
    private BigDecimal inputPrice;

    /** 输出价格 */
    private BigDecimal outputPrice;

    /** 批量折扣 */
    private BigDecimal batchDiscount;

    /** 精度 */
    private Integer precision;

    /** 舍入模式 */
    private String roundingMode;

    /** 生效时间 */
    private LocalDateTime effectiveAt;

    /** 失效时间 */
    private LocalDateTime expireAt;

    /** 备注 */
    private String remark;
}
