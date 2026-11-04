package com.aisaas.user.entity;

import com.aisaas.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * VIP会员实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user_vip_membership")
public class VipMembership extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 用户ID
     */
    @TableField("user_id")
    private Long userId;

    /**
     * VIP等级: 1-普通VIP 2-高级VIP 3-至尊VIP
     */
    @TableField("vip_level")
    private Integer vipLevel;

    /**
     * VIP名称
     */
    @TableField("vip_name")
    private String vipName;

    /**
     * 开通方式: 1-月付 2-季付 3-年付 4-永久
     */
    @TableField("subscribe_type")
    private Integer subscribeType;

    /**
     * 支付金额
     */
    @TableField("pay_amount")
    private BigDecimal payAmount;

    /**
     * 开通时间
     */
    @TableField("start_at")
    private LocalDateTime startAt;

    /**
     * 到期时间
     */
    @TableField("expire_at")
    private LocalDateTime expireAt;

    /**
     * 是否自动续费: 0-否 1-是
     */
    @TableField("auto_renew")
    private Integer autoRenew;

    /**
     * 状态: 0-已过期 1-生效中 2-已取消
     */
    private Integer status;

    /**
     * 来源: 1-直接购买 2-兑换码 3-赠送 4-活动
     */
    private Integer source;

    /**
     * 备注
     */
    private String remark;

    /**
     * 检查会员是否有效
     */
    public boolean isValid() {
        if (status == null || status != 1) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now();
        if (startAt != null && now.isBefore(startAt)) {
            return false;
        }
        if (expireAt != null && now.isAfter(expireAt)) {
            return false;
        }
        return true;
    }
}
