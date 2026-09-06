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
 * 发票审核DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceAuditDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 发票ID */
    private String invoiceId;

    /** 是否通过 */
    private Boolean approved;

    /** 拒绝原因 */
    private String rejectReason;

    /** 备注 */
    private String remark;
}
