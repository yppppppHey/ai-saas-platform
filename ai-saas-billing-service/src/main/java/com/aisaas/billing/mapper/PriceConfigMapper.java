package com.aisaas.billing.mapper;

import com.aisaas.billing.entity.PriceConfig;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 价格配置Mapper
 */
@Mapper
public interface PriceConfigMapper extends BaseMapper<PriceConfig> {

    /**
     * 根据提供商、模型、操作类型查询有效配置
     */
    @Select("SELECT * FROM billing_price_config " +
            "WHERE provider = #{provider} AND model_id = #{modelId} " +
            "AND operation_type = #{operationType} " +
            "AND status = 1 AND is_deleted = 0 " +
            "AND effective_at <= #{now} AND (expire_at IS NULL OR expire_at > #{now}) " +
            "ORDER BY priority DESC, created_at DESC LIMIT 1")
    PriceConfig selectValidConfig(@Param("provider") String provider,
                                  @Param("modelId") String modelId,
                                  @Param("operationType") String operationType,
                                  @Param("now") LocalDateTime now);

    /**
     * 查询所有有效配置
     */
    @Select("SELECT * FROM billing_price_config " +
            "WHERE status = 1 AND is_deleted = 0 " +
            "AND effective_at <= #{now} AND (expire_at IS NULL OR expire_at > #{now}) " +
            "ORDER BY provider, model_id, operation_type")
    List<PriceConfig> selectAllValidConfigs(@Param("now") LocalDateTime now);
}
