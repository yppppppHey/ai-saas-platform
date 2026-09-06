package com.aisaas.user.entity;

import com.aisaas.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 用户角色关联实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user_role_relation")
public class UserRoleRelation extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 用户ID
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 角色ID
     */
    @TableField("role_id")
    private Long roleId;

    /**
     * 授予方式: 1-系统分配 2-手动分配 3-条件触发
     */
    @TableField("grant_type")
    private Integer grantType;

    /**
     * 授予者ID
     */
    @TableField("granted_by")
    private Long grantedBy;

    /**
     * 生效时间
     */
    @TableField("effective_at")
    private LocalDateTime effectiveAt;

    /**
     * 过期时间
     */
    @TableField("expire_at")
    private LocalDateTime expireAt;

    /**
     * 状态: 0-禁用 1-启用
     */
    private Integer status;

    /**
     * 检查角色是否有效
     */
    public boolean isEffective() {
        if (status == null || status != 1) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now();
        if (effectiveAt != null && now.isBefore(effectiveAt)) {
            return false;
        }
        if (expireAt != null && now.isAfter(expireAt)) {
            return false;
        }
        return true;
    }
}
