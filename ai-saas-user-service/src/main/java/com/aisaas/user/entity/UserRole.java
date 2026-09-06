package com.aisaas.user.entity;

import com.aisaas.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 用户角色实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user_role")
public class UserRole extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 角色编码
     */
    @TableField("role_code")
    private String roleCode;

    /**
     * 角色名称
     */
    @TableField("role_name")
    private String roleName;

    /**
     * 角色描述
     */
    private String description;

    /**
     * 角色类型: 1-系统角色 2-自定义角色
     */
    @TableField("role_type")
    private Integer roleType;

    /**
     * 数据范围: 1-全部 2-本部门 3-本部门及以下 4-仅本人
     */
    @TableField("data_scope")
    private Integer dataScope;

    /**
     * 排序号
     */
    private Integer sort;

    /**
     * 状态: 0-禁用 1-启用
     */
    private Integer status;

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
}
