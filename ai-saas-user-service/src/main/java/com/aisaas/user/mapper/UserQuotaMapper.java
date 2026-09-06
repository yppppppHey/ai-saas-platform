package com.aisaas.user.mapper;

import com.aisaas.user.entity.UserQuota;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 用户配额Mapper
 */
@Mapper
public interface UserQuotaMapper extends BaseMapper<UserQuota> {

    /**
     * 根据用户ID和配额类型查询
     */
    @Select("SELECT * FROM user_quota WHERE user_id = #{userId} AND quota_type = #{quotaType} AND is_deleted = 0 LIMIT 1")
    UserQuota selectByUserIdAndType(@Param("userId") Long userId, @Param("quotaType") Integer quotaType);

    /**
     * 根据用户ID查询所有配额
     */
    @Select("SELECT * FROM user_quota WHERE user_id = #{userId} AND is_deleted = 0")
    List<UserQuota> selectByUserId(@Param("userId") Long userId);

    /**
     * 增加已使用量
     */
    @Update("UPDATE user_quota SET daily_used = daily_used + #{amount}, " +
            "monthly_used = monthly_used + #{amount}, " +
            "total_used = total_used + #{amount}, " +
            "updated_at = NOW() " +
            "WHERE user_id = #{userId} AND quota_type = #{quotaType} AND is_deleted = 0")
    int incrementUsed(@Param("userId") Long userId, @Param("quotaType") Integer quotaType, @Param("amount") Long amount);

    /**
     * 重置每日使用统计
     */
    @Update("UPDATE user_quota SET daily_used = 0, updated_at = NOW() WHERE is_deleted = 0")
    int resetDailyUsed();

    /**
     * 重置每月使用统计
     */
    @Update("UPDATE user_quota SET daily_used = 0, monthly_used = 0, updated_at = NOW() WHERE is_deleted = 0")
    int resetMonthlyUsed();
}
