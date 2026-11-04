package com.aisaas.user.service;

import com.aisaas.user.entity.UserRole;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 角色服务接口
 */
public interface RoleService extends IService<UserRole> {

    /**
     * 根据角色编码获取角色
     */
    UserRole getByCode(String roleCode);

    /**
     * 获取用户的角色列表
     */
    List<UserRole> getUserRoles(Long userId);

    /**
     * 获取用户的角色编码列表
     */
    List<String> getUserRoleCodes(Long userId);

    /**
     * 检查用户是否拥有指定角色
     */
    boolean hasRole(Long userId, String roleCode);

    /**
     * 为用户分配角色
     */
    void assignRole(Long userId, Long roleId, Long operatorId);

    /**
     * 为用户分配多个角色
     */
    void assignRoles(Long userId, List<Long> roleIds, Long operatorId);

    /**
     * 移除用户角色
     */
    void removeRole(Long userId, Long roleId);

    /**
     * 移除用户的所有角色
     */
    void removeAllRoles(Long userId);

    /**
     * 获取所有有效角色
     */
    List<UserRole> getAllActiveRoles();
}
