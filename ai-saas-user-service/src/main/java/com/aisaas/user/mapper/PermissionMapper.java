package com.aisaas.user.mapper;

import com.aisaas.user.entity.Permission;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 权限Mapper
 */
@Mapper
public interface PermissionMapper extends BaseMapper<Permission> {

    /**
     * 根据编码查询权限
     */
    @Select("SELECT * FROM user_permission WHERE permission_code = #{code} AND is_deleted = 0 LIMIT 1")
    Permission selectByCode(@Param("code") String code);

    /**
     * 根据角色ID查询权限列表
     */
    List<Permission> selectPermissionsByRoleId(@Param("roleId") Long roleId);

    /**
     * 根据用户ID查询权限列表
     */
    List<Permission> selectPermissionsByUserId(@Param("userId") Long userId);

    /**
     * 查询所有有效权限
     */
    @Select("SELECT * FROM user_permission WHERE status = 1 AND is_deleted = 0 ORDER BY sort")
    List<Permission> selectAllActivePermissions();

    /**
     * 根据父ID查询子权限
     */
    @Select("SELECT * FROM user_permission WHERE parent_id = #{parentId} AND status = 1 AND is_deleted = 0 ORDER BY sort")
    List<Permission> selectByParentId(@Param("parentId") Long parentId);

    /**
     * 查询角色权限关联是否已存在
     */
    @Select("SELECT COUNT(*) FROM user_role_permission WHERE role_id = #{roleId} AND permission_id = #{permissionId} AND is_deleted = 0")
    long countRolePermission(@Param("roleId") Long roleId, @Param("permissionId") Long permissionId);

    /**
     * 为角色分配权限（已存在则忽略）
     */
    @Insert("INSERT INTO user_role_permission (role_id, permission_id, status, created_at, updated_at, is_deleted) " +
            "SELECT #{roleId}, #{permissionId}, 1, NOW(), NOW(), 0 FROM DUAL " +
            "WHERE NOT EXISTS (SELECT 1 FROM (SELECT 1) t WHERE (SELECT COUNT(*) FROM user_role_permission rp " +
            "WHERE rp.role_id = #{roleId} AND rp.permission_id = #{permissionId} AND rp.is_deleted = 0) > 0)")
    int insertRolePermission(@Param("roleId") Long roleId, @Param("permissionId") Long permissionId);

    /**
     * 移除角色指定权限
     */
    @Delete("DELETE FROM user_role_permission WHERE role_id = #{roleId} AND permission_id = #{permissionId}")
    int deleteRolePermission(@Param("roleId") Long roleId, @Param("permissionId") Long permissionId);

    /**
     * 移除角色全部权限
     */
    @Delete("DELETE FROM user_role_permission WHERE role_id = #{roleId}")
    int deleteRolePermissions(@Param("roleId") Long roleId);
}
