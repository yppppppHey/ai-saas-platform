package com.aisaas.billing.entity;

import com.aisaas.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("billing_invoice")
public class Invoice extends BaseEntity {
    
    private static final long serialVersionUID = 1L;
    
    private String invoiceId;
    private Long userId;
    private String invoiceNo;
    private Integer invoiceType;
    private String title;
    private String taxNo;
    private String bankName;
    private String bankAccount;
    private String address;
    private String phone;
    private String email;
    private BigDecimal amount;
    private Integer status;
    private LocalDateTime appliedAt;
    private LocalDateTime issuedAt;
    private String billIds;
    private String remark;
    private String rejectReason;
}
