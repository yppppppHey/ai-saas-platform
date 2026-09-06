package com.aisaas.user.mapper;

import com.aisaas.user.entity.UserRole;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 角色Mapper
 */
@Mapper
public interface UserRoleMapper extends BaseMapper<UserRole> {

    /**
     * 根据角色编码查询
     */
    @Select("SELECT * FROM user_role WHERE role_code = #{roleCode} AND is_deleted = 0 LIMIT 1")
    UserRole selectByCode(@Param("roleCode") String roleCode);

    /**
     * 根据用户ID查询角色列表
     */
    @Select("SELECT r.* FROM user_role r " +
            "INNER JOIN user_role_relation ur ON r.id = ur.role_id " +
            "WHERE ur.user_id = #{userId} AND ur.status = 1 AND r.status = 1 " +
            "AND r.is_deleted = 0 AND ur.is_deleted = 0")
    List<UserRole> selectRolesByUserId(@Param("userId") Long userId);

    /**
     * 查询所有有效角色
     */
    @Select("SELECT * FROM user_role WHERE status = 1 AND is_deleted = 0 ORDER BY sort")
    List<UserRole> selectAllActiveRoles();
}
