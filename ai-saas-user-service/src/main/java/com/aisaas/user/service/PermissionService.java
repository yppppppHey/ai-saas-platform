package com.aisaas.user.service;

import com.aisaas.user.entity.Permission;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Set;

/**
 * 权限服务接口
 */
public interface PermissionService extends IService<Permission> {

    /**
     * 根据编码获取权限
     */
    Permission getByCode(String permissionCode);

    /**
     * 获取角色的权限列表
     */
    List<Permission> getRolePermissions(Long roleId);

    /**
     * 获取用户的权限列表
     */
    List<Permission> getUserPermissions(Long userId);

    /**
     * 获取用户的权限编码集合
     */
    Set<String> getUserPermissionCodes(Long userId);

    /**
     * 检查用户是否拥有指定权限
     */
    boolean hasPermission(Long userId, String permissionCode);

    /**
     * 检查用户是否有任意一个权限
     */
    boolean hasAnyPermission(Long userId, String... permissionCodes);

    /**
     * 检查用户是否有所有权限
     */
    boolean hasAllPermissions(Long userId, String... permissionCodes);

    /**
     * 为角色分配权限
     */
    void assignPermission(Long roleId, Long permissionId);

    /**
     * 为角色分配多个权限
     */
    void assignPermissions(Long roleId, List<Long> permissionIds);

    /**
     * 移除角色权限
     */
    void removePermission(Long roleId, Long permissionId);

    /**
     * 移除角色的所有权限
     */
    void removeAllPermissions(Long roleId);

    /**
     * 获取所有有效权限
     */
    List<Permission> getAllActivePermissions();

    /**
     * 根据父ID获取子权限
     */
    List<Permission> getChildrenByParentId(Long parentId);

    /**
     * 构建权限树
     */
    List<Permission> buildPermissionTree();
}
