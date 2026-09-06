package com.aisaas.user.controller;

import com.aisaas.common.result.Result;
import com.aisaas.user.entity.UserRole;
import com.aisaas.user.service.RoleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 角色控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/role")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    /**
     * 获取所有角色
     */
    @GetMapping("/list")
    public Result<List<UserRole>> getAllRoles() {
        return Result.success(roleService.getAllActiveRoles());
    }

    /**
     * 根据ID获取角色
     */
    @GetMapping("/{roleId}")
    public Result<UserRole> getRoleById(@PathVariable Long roleId) {
        return Result.success(roleService.getById(roleId));
    }

    /**
     * 根据编码获取角色
     */
    @GetMapping("/code/{roleCode}")
    public Result<UserRole> getRoleByCode(@PathVariable String roleCode) {
        return Result.success(roleService.getByCode(roleCode));
    }

    /**
     * 创建角色
     */
    @PostMapping
    public Result<Void> createRole(@RequestBody UserRole role) {
        roleService.save(role);
        return Result.success();
    }

    /**
     * 更新角色
     */
    @PutMapping("/{roleId}")
    public Result<Void> updateRole(@PathVariable Long roleId, @RequestBody UserRole role) {
        role.setId(roleId);
        roleService.updateById(role);
        return Result.success();
    }

    /**
     * 删除角色
     */
    @DeleteMapping("/{roleId}")
    public Result<Void> deleteRole(@PathVariable Long roleId) {
        roleService.removeById(roleId);
        return Result.success();
    }

    /**
     * 获取用户的角色
     */
    @GetMapping("/user/{userId}")
    public Result<List<UserRole>> getUserRoles(@PathVariable Long userId) {
        return Result.success(roleService.getUserRoles(userId));
    }

    /**
     * 为用户分配角色
     */
    @PostMapping("/user/{userId}/assign")
    public Result<Void> assignRole(@PathVariable Long userId, @RequestParam Long roleId) {
        roleService.assignRole(userId, roleId, null);
        return Result.success();
    }

    /**
     * 移除用户角色
     */
    @DeleteMapping("/user/{userId}/remove")
    public Result<Void> removeRole(@PathVariable Long userId, @RequestParam Long roleId) {
        roleService.removeRole(userId, roleId);
        return Result.success();
    }
}
