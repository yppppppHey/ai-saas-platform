package com.aisaas.user.mapper;

import com.aisaas.user.entity.VipMembership;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * VIP会员Mapper
 */
@Mapper
public interface VipMembershipMapper extends BaseMapper<VipMembership> {

    /**
     * 根据用户ID查询当前有效的VIP会员信息
     */
    @Select("SELECT * FROM user_vip_membership " +
            "WHERE user_id = #{userId} AND status = 1 AND is_deleted = 0 " +
            "AND (expire_at IS NULL OR expire_at > NOW()) " +
            "ORDER BY vip_level DESC, id DESC LIMIT 1")
    VipMembership selectValidByUserId(@Param("userId") Long userId);

    /**
     * 根据用户ID查询所有VIP会员记录
     */
    @Select("SELECT * FROM user_vip_membership WHERE user_id = #{userId} AND is_deleted = 0 ORDER BY id DESC")
    List<VipMembership> selectByUserId(@Param("userId") Long userId);

    /**
     * 检查用户是否有有效VIP
     */
    @Select("SELECT COUNT(*) FROM user_vip_membership " +
            "WHERE user_id = #{userId} AND status = 1 AND is_deleted = 0 " +
            "AND (expire_at IS NULL OR expire_at > NOW())")
    Long countValidByUserId(@Param("userId") Long userId);
}
