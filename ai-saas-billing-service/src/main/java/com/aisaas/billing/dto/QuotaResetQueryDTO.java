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
 * 配额重置查询DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuotaResetQueryDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 页码 */
    private Integer pageNum;

    /** 每页大小 */
    private Integer pageSize;

    /** 用户ID */
    private Long userId;

    /** 配额类型 */
    private Integer quotaType;

    /** 起始日期 */
    private LocalDate startDate;

    /** 截止日期 */
    private LocalDate endDate;
}
