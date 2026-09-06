package com.aisaas.billing.entity;

import com.aisaas.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("billing_bill")
public class Bill extends BaseEntity {
    
    private static final long serialVersionUID = 1L;
    
    private String billId;
    private Long userId;
    private String billNo;
    private Integer billType;
    private Integer billPeriod;
    private LocalDate billStartDate;
    private LocalDate billEndDate;
    private LocalDate billDueDate;
    private BigDecimal totalAmount;
    private BigDecimal discountAmount;
    private BigDecimal payableAmount;
    private BigDecimal paidAmount;
    private BigDecimal taxAmount;
    private Integer status;
    private LocalDateTime paidAt;
    private String payMethod;
    private String payTradeNo;
    private String remark;
}
