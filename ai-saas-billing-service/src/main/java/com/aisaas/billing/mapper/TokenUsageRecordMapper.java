package com.aisaas.billing.mapper;

import com.aisaas.billing.entity.TokenUsageRecord;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Token使用记录Mapper
 */
@Mapper
public interface TokenUsageRecordMapper extends BaseMapper<TokenUsageRecord> {

    /**
     * 分页查询用户的Token使用记录
     */
    @Select("SELECT * FROM billing_token_usage WHERE user_id = #{userId} AND is_deleted = 0 ORDER BY created_at DESC")
    IPage<TokenUsageRecord> selectPageByUserId(Page<TokenUsageRecord> page, @Param("userId") Long userId);

    /**
     * 根据条件查询统计
     */
    @Select("<script>" +
            "SELECT COUNT(*) as count, SUM(total_tokens) as totalTokens, " +
            "SUM(total_cost) as totalCost, SUM(cost_cny) as totalCostCny " +
            "FROM billing_token_usage WHERE is_deleted = 0 " +
            "<if test='userId != null'> AND user_id = #{userId} </if>" +
            "<if test='startDate != null'> AND usage_date &gt;= #{startDate} </if>" +
            "<if test='endDate != null'> AND usage_date &lt;= #{endDate} </if>" +
            "<if test='provider != null'> AND provider = #{provider} </if>" +
            "<if test='modelId != null'> AND model_id = #{modelId} </if>" +
            "</script>")
    Map<String, Object> selectStatistics(@Param("userId") Long userId,
                                         @Param("startDate") LocalDate startDate,
                                         @Param("endDate") LocalDate endDate,
                                         @Param("provider") String provider,
                                         @Param("modelId") String modelId);

    /**
     * 按日期统计
     */
    @Select("SELECT usage_date as date, COUNT(*) as count, SUM(total_tokens) as tokens, " +
            "SUM(total_cost) as cost, SUM(cost_cny) as costCny " +
            "FROM billing_token_usage " +
            "WHERE user_id = #{userId} AND usage_date BETWEEN #{startDate} AND #{endDate} AND is_deleted = 0 " +
            "GROUP BY usage_date ORDER BY usage_date")
    List<Map<String, Object>> selectDailyStatistics(@Param("userId") Long userId,
                                                    @Param("startDate") LocalDate startDate,
                                                    @Param("endDate") LocalDate endDate);

    /**
     * 按模型统计
     */
    @Select("SELECT provider, model_id as modelId, COUNT(*) as count, SUM(total_tokens) as tokens, " +
            "SUM(total_cost) as cost " +
            "FROM billing_token_usage " +
            "WHERE user_id = #{userId} AND usage_date BETWEEN #{startDate} AND #{endDate} AND is_deleted = 0 " +
            "GROUP BY provider, model_id ORDER BY tokens DESC")
    List<Map<String, Object>> selectModelStatistics(@Param("userId") Long userId,
                                                    @Param("startDate") LocalDate startDate,
                                                    @Param("endDate") LocalDate endDate);

    /**
     * 按操作类型统计
     */
    @Select("SELECT operation_type as operationType, COUNT(*) as count, SUM(total_tokens) as tokens, " +
            "SUM(total_cost) as cost " +
            "FROM billing_token_usage " +
            "WHERE user_id = #{userId} AND usage_date BETWEEN #{startDate} AND #{endDate} AND is_deleted = 0 " +
            "GROUP BY operation_type")
    List<Map<String, Object>> selectOperationTypeStatistics(@Param("userId") Long userId,
                                                            @Param("startDate") LocalDate startDate,
                                                            @Param("endDate") LocalDate endDate);

    /**
     * 查询总金额
     */
    @Select("SELECT SUM(total_cost) FROM billing_token_usage WHERE user_id = #{userId} AND is_billed = 0 AND is_deleted = 0")
    BigDecimal selectUnbilledAmount(@Param("userId") Long userId);
}
