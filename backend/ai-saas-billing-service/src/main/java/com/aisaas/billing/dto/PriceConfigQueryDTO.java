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
 * 价格配置查询DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PriceConfigQueryDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 页码 */
    private Integer pageNum;

    /** 每页大小 */
    private Integer pageSize;

    /** 服务商 */
    private String provider;

    /** 模型ID */
    private String modelId;

    /** 操作类型 */
    private String operationType;

    /** 状态 */
    private Integer status;
}
