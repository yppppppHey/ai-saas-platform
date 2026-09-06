package com.aisaas.billing.mapper;

import com.aisaas.billing.entity.QuotaRecord;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDate;
import java.util.List;

/**
 * 配额记录Mapper
 */
@Mapper
public interface QuotaRecordMapper extends BaseMapper<QuotaRecord> {

    /**
     * 根据用户ID和配额类型查询记录
     */
    @Select("SELECT * FROM billing_quota_record " +
            "WHERE user_id = #{userId} AND quota_type = #{quotaType} " +
            "AND status = 1 AND is_deleted = 0 " +
            "ORDER BY created_at DESC LIMIT 1")
    QuotaRecord selectByUserIdAndType(@Param("userId") Long userId, @Param("quotaType") Integer quotaType);

    /**
     * 查询用户所有配额记录
     */
    @Select("SELECT * FROM billing_quota_record " +
            "WHERE user_id = #{userId} AND status = 1 AND is_deleted = 0")
    List<QuotaRecord> selectByUserId(@Param("userId") Long userId);

    /**
     * 增加使用量
     */
    @Update("UPDATE billing_quota_record SET " +
            "daily_used = daily_used + #{amount}, " +
            "monthly_used = monthly_used + #{amount}, " +
            "total_used = total_used + #{amount}, " +
            "updated_at = NOW() " +
            "WHERE user_id = #{userId} AND quota_type = #{quotaType} AND is_deleted = 0")
    int increaseUsage(@Param("userId") Long userId, @Param("quotaType") Integer quotaType, @Param("amount") Long amount);

    /**
     * 重置每日使用量
     */
    @Update("UPDATE billing_quota_record SET daily_used = 0, last_reset_date = #{resetDate}, updated_at = NOW() " +
            "WHERE last_reset_date &lt; #{resetDate} AND is_deleted = 0")
    int resetDailyUsage(@Param("resetDate") LocalDate resetDate);

    /**
     * 重置每月使用量
     */
    @Update("UPDATE billing_quota_record SET monthly_used = 0, updated_at = NOW() " +
            "WHERE is_deleted = 0")
    int resetMonthlyUsage();
}
