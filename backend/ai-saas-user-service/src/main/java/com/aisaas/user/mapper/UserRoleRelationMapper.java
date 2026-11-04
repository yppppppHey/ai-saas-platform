package com.aisaas.user.mapper;

import com.aisaas.user.entity.UserRoleRelation;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 用户角色关联Mapper
 */
@Mapper
public interface UserRoleRelationMapper extends BaseMapper<UserRoleRelation> {

    /**
     * 根据用户ID查询角色关联
     */
    @Select("SELECT * FROM user_role_relation WHERE user_id = #{userId} AND is_deleted = 0")
    List<UserRoleRelation> selectByUserId(@Param("userId") Long userId);

    /**
     * 根据用户ID和角色ID查询
     */
    @Select("SELECT * FROM user_role_relation WHERE user_id = #{userId} AND role_id = #{roleId} AND is_deleted = 0 LIMIT 1")
    UserRoleRelation selectByUserIdAndRoleId(@Param("userId") Long userId, @Param("roleId") Long roleId);

    /**
     * 检查用户是否有指定角色
     */
    @Select("SELECT COUNT(*) FROM user_role_relation ur " +
            "INNER JOIN user_role r ON ur.role_id = r.id " +
            "WHERE ur.user_id = #{userId} AND r.role_code = #{roleCode} " +
            "AND ur.status = 1 AND r.status = 1 AND ur.is_deleted = 0 AND r.is_deleted = 0")
    Long countByUserIdAndRoleCode(@Param("userId") Long userId, @Param("roleCode") String roleCode);

    /**
     * 删除用户的所有角色
     */
    void deleteByUserId(@Param("userId") Long userId);
}
