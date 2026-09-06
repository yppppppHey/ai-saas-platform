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
 * 配额扣除项DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuotaDeductItemDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 配额类型 */
    private Integer quotaType;

    /** 扣除额度 */
    private Long amount;

    /** 业务类型 */
    private String bizType;

    /** 业务ID */
    private String bizId;
}
