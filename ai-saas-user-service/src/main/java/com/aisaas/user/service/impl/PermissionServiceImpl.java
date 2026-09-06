package com.aisaas.user.service.impl;

import com.aisaas.user.entity.Permission;
import com.aisaas.user.mapper.PermissionMapper;
import com.aisaas.user.service.PermissionService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 权限服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionServiceImpl extends ServiceImpl<PermissionMapper, Permission> implements PermissionService {

    private final PermissionMapper permissionMapper;

    @Override
    public Permission getByCode(String permissionCode) {
        return permissionMapper.selectByCode(permissionCode);
    }

    @Override
    public List<Permission> getRolePermissions(Long roleId) {
        return permissionMapper.selectPermissionsByRoleId(roleId);
    }

    @Override
    public List<Permission> getUserPermissions(Long userId) {
        return permissionMapper.selectPermissionsByUserId(userId);
    }

    @Override
    public Set<String> getUserPermissionCodes(Long userId) {
        List<Permission> permissions = getUserPermissions(userId);
        return permissions.stream()
                .map(Permission::getPermissionCode)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    @Override
    public boolean hasPermission(Long userId, String permissionCode) {
        Set<String> userPermissions = getUserPermissionCodes(userId);
        return userPermissions.contains(permissionCode) || userPermissions.contains("*");
    }

    @Override
    public boolean hasAnyPermission(Long userId, String... permissionCodes) {
        Set<String> userPermissions = getUserPermissionCodes(userId);
        for (String permission : permissionCodes) {
            if (userPermissions.contains(permission)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean hasAllPermissions(Long userId, String... permissionCodes) {
        Set<String> userPermissions = getUserPermissionCodes(userId);
        for (String permission : permissionCodes) {
            if (!userPermissions.contains(permission)) {
                return false;
            }
        }
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignPermission(Long roleId, Long permissionId) {
        // TODO: 实现角色权限分配
        log.info("为角色分配权限: roleId={}, permissionId={}", roleId, permissionId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignPermissions(Long roleId, List<Long> permissionIds) {
        // TODO: 实现批量分配权限
        log.info("为角色批量分配权限: roleId={}, permissionIds={}", roleId, permissionIds);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removePermission(Long roleId, Long permissionId) {
        // TODO: 实现移除角色权限
        log.info("移除角色权限: roleId={}, permissionId={}", roleId, permissionId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeAllPermissions(Long roleId) {
        // TODO: 实现移除角色所有权限
        log.info("移除角色所有权限: roleId={}", roleId);
    }

    @Override
    public List<Permission> getAllActivePermissions() {
        return permissionMapper.selectAllActivePermissions();
    }

    @Override
    public List<Permission> getChildrenByParentId(Long parentId) {
        return permissionMapper.selectByParentId(parentId);
    }

    @Override
    public List<Permission> buildPermissionTree() {
        List<Permission> allPermissions = getAllActivePermissions();
        
        // 构建树形结构
        Map<Long, Permission> permissionMap = new HashMap<>();
        List<Permission> rootPermissions = new ArrayList<>();
        
        for (Permission permission : allPermissions) {
            permissionMap.put(permission.getId(), permission);
        }
        
        for (Permission permission : allPermissions) {
            if (permission.getParentId() == null || permission.getParentId() == 0) {
                rootPermissions.add(permission);
            } else {
                Permission parent = permissionMap.get(permission.getParentId());
                if (parent != null) {
                    // TODO: 添加子权限到父权限
                }
            }
        }
        
        return rootPermissions;
    }
}
