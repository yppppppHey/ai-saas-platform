package com.aisaas.user.mapper;

import com.aisaas.user.entity.Permission;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
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
}
