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
 * 批量配额扣除结果VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchQuotaDeductResultVO implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 结果列表 */
    private List<QuotaDeductResultVO> results;

    /** 是否全部成功 */
    private Boolean allSuccess;

    /** 总扣除额度 */
    private Long totalDeducted;

    /** 总数 */
    private Integer totalCount;

    /** 成功数 */
    private Integer successCount;

    /** 失败数 */
    private Integer failCount;
}
