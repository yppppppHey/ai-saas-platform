package com.aisaas.billing.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 支付查询DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentQueryDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 页码 */
    private Integer pageNum;

    /** 每页大小 */
    private Integer pageSize;

    /** 用户ID */
    private Long userId;

    /** 支付类型 */
    private Integer paymentType;

    /** 状态 */
    private Integer status;

    /** 起始时间 */
    private LocalDateTime startDate;

    /** 截止时间 */
    private LocalDateTime endDate;
}
