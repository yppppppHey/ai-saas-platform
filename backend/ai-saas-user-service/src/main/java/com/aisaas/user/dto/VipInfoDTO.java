package com.aisaas.user.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * VIP信息DTO
 */
@Data
public class VipInfoDTO {

    /**
     * 是否VIP
     */
    private Boolean isVip;

    /**
     * VIP等级: 1-普通VIP 2-高级VIP 3-至尊VIP
     */
    private Integer vipLevel;

    /**
     * VIP名称
     */
    private String vipName;

    /**
     * 开通方式: 1-月付 2-季付 3-年付 4-永久
     */
    private Integer subscribeType;

    /**
     * 开通时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startAt;

    /**
     * 到期时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime expireAt;

    /**
     * 是否自动续费
     */
    private Boolean autoRenew;

    /**
     * 剩余天数
     */
    private Long remainingDays;
}
