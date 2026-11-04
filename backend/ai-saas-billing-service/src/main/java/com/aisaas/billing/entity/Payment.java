package com.aisaas.billing.entity;

import com.aisaas.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("billing_payment")
public class Payment extends BaseEntity {
    
    private static final long serialVersionUID = 1L;
    
    private String paymentId;
    private Long userId;
    private String billId;
    private String paymentNo;
    private Integer paymentType;
    private Integer channel;
    private String channelName;
    private BigDecimal amount;
    private BigDecimal fee;
    private String currency;
    private Integer status;
    private LocalDateTime paidAt;
    private LocalDateTime expiredAt;
    private String thirdTradeNo;
    private String payerInfo;
    private String callbackInfo;
    private String remark;
}
