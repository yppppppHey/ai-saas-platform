package com.aisaas.billing.mapper;

import com.aisaas.billing.entity.QuotaConfig;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 配额配置Mapper
 */
@Mapper
public interface QuotaConfigMapper extends BaseMapper<QuotaConfig> {

    /**
     * 根据用户类型和配额类型查询有效配置
     */
    @Select("SELECT * FROM billing_quota_config " +
            "WHERE user_type = #{userType} AND quota_type = #{quotaType} " +
            "AND status = 1 AND is_deleted = 0 " +
            "AND effective_at <= #{now} AND (expire_at IS NULL OR expire_at > #{now}) " +
            "ORDER BY priority DESC, created_at DESC LIMIT 1")
    QuotaConfig selectValidConfig(@Param("userType") Integer userType,
                                  @Param("quotaType") Integer quotaType,
                                  @Param("now") LocalDateTime now);

    /**
     * 查询所有有效配置
     */
    @Select("SELECT * FROM billing_quota_config " +
            "WHERE status = 1 AND is_deleted = 0 " +
            "AND effective_at <= #{now} AND (expire_at IS NULL OR expire_at > #{now}) " +
            "ORDER BY user_type, quota_type, priority DESC")
    List<QuotaConfig> selectAllValidConfigs(@Param("now") LocalDateTime now);
}
