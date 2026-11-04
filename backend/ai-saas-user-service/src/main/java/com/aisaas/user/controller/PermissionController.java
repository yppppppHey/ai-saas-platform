package com.aisaas.user.controller;

import com.aisaas.common.result.Result;
import com.aisaas.user.entity.Permission;
import com.aisaas.user.service.PermissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

/**
 * 权限控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/permission")
@RequiredArgsConstructor
public class PermissionController {

    private final PermissionService permissionService;

    /**
     * 获取所有权限
     */
    @GetMapping("/list")
    public Result<List<Permission>> getAllPermissions() {
        return Result.success(permissionService.getAllActivePermissions());
    }

    /**
     * 获取权限树
     */
    @GetMapping("/tree")
    public Result<List<Permission>> getPermissionTree() {
        return Result.success(permissionService.buildPermissionTree());
    }

    /**
     * 根据ID获取权限
     */
    @GetMapping("/{permissionId}")
    public Result<Permission> getPermissionById(@PathVariable Long permissionId) {
        return Result.success(permissionService.getById(permissionId));
    }

    /**
     * 根据编码获取权限
     */
    @GetMapping("/code/{permissionCode}")
    public Result<Permission> getPermissionByCode(@PathVariable String permissionCode) {
        return Result.success(permissionService.getByCode(permissionCode));
    }

    /**
     * 创建权限
     */
    @PostMapping
    public Result<Void> createPermission(@RequestBody Permission permission) {
        permissionService.save(permission);
        return Result.success();
    }

    /**
     * 更新权限
     */
    @PutMapping("/{permissionId}")
    public Result<Void> updatePermission(@PathVariable Long permissionId, @RequestBody Permission permission) {
        permission.setId(permissionId);
        permissionService.updateById(permission);
        return Result.success();
    }

    /**
     * 删除权限
     */
    @DeleteMapping("/{permissionId}")
    public Result<Void> deletePermission(@PathVariable Long permissionId) {
        permissionService.removeById(permissionId);
        return Result.success();
    }

    /**
     * 获取用户的权限列表
     */
    @GetMapping("/user/{userId}")
    public Result<List<Permission>> getUserPermissions(@PathVariable Long userId) {
        return Result.success(permissionService.getUserPermissions(userId));
    }

    /**
     * 获取用户的权限编码集合
     */
    @GetMapping("/user/{userId}/codes")
    public Result<Set<String>> getUserPermissionCodes(@PathVariable Long userId) {
        return Result.success(permissionService.getUserPermissionCodes(userId));
    }

    /**
     * 检查用户是否有指定权限
     */
    @GetMapping("/check")
    public Result<Boolean> hasPermission(@RequestParam Long userId, @RequestParam String permissionCode) {
        return Result.success(permissionService.hasPermission(userId, permissionCode));
    }

    /**
     * 获取角色的权限列表
     */
    @GetMapping("/role/{roleId}")
    public Result<List<Permission>> getRolePermissions(@PathVariable Long roleId) {
        return Result.success(permissionService.getRolePermissions(roleId));
    }

    /**
     * 为角色分配权限
     */
    @PostMapping("/role/{roleId}/assign")
    public Result<Void> assignPermission(@PathVariable Long roleId, @RequestParam Long permissionId) {
        permissionService.assignPermission(roleId, permissionId);
        return Result.success();
    }

    /**
     * 移除角色权限
     */
    @DeleteMapping("/role/{roleId}/remove")
    public Result<Void> removePermission(@PathVariable Long roleId, @RequestParam Long permissionId) {
        permissionService.removePermission(roleId, permissionId);
        return Result.success();
    }
}
