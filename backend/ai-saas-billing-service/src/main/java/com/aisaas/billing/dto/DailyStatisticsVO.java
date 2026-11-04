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
 * 每日统计VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyStatisticsVO implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 明细列表 */
    private List<DailyStatisticsItemVO> items;

    /** 总数 */
    private Integer totalCount;
}
