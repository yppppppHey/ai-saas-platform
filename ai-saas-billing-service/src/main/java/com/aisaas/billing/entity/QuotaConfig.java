package com.aisaas.billing.entity;

import com.aisaas.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 配额配置实体类
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("billing_quota_config")
public class QuotaConfig extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 配置ID */
    private String configId;

    /** 配置名称 */
    private String configName;

    /** 配额类型: 1-每日token数 2-每月token数 3-总计token数 4-每日请求数 5-存储空间 */
    private Integer quotaType;

    /** 适用用户类型: 1-普通用户 2-VIP用户 9-管理员 */
    private Integer userType;

    /** 每日限制(0为无限制) */
    private Long dailyLimit;

    /** 每月限制(0为无限制) */
    private Long monthlyLimit;

    /** 总计限制(0为无限制) */
    private Long totalLimit;

    /** 超限处理方式: 1-拒绝 2-警告 3-允许但记录 */
    private Integer exceedAction;

    /** 超限阈值(百分比,如120表示超过120%才处理) */
    private Integer exceedThreshold;

    /** 重置周期: daily/monthly/none */
    private String resetCycle;

    /** 每月重置日 */
    private Integer resetDay;

    /** 生效时间 */
    private LocalDateTime effectiveAt;

    /** 过期时间 */
    private LocalDateTime expireAt;

    /** 状态: 0-禁用 1-启用 */
    private Integer status;

    /** 优先级(数字越小优先级越高) */
    private Integer priority;

    /** 备注 */
    private String remark;
}
