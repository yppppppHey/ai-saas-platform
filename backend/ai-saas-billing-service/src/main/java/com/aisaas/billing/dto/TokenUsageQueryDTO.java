package com.aisaas.billing.dto;

import com.aisaas.common.entity.BasePageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * Token使用记录查询DTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class TokenUsageQueryDTO extends BasePageQuery {

    private Long userId;
    private LocalDate startDate;
    private LocalDate endDate;
    private String provider;
    private String modelId;
    private String operationType;
}
