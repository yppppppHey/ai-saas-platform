package com.aisaas.billing.mapper;

import com.aisaas.billing.entity.Bill;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 账单Mapper
 */
@Mapper
public interface BillMapper extends BaseMapper<Bill> {

    /**
     * 根据账单ID查询
     */
    @Select("SELECT * FROM billing_bill WHERE bill_id = #{billId} AND is_deleted = 0")
    Bill selectByBillId(@Param("billId") String billId);

    /**
     * 根据用户ID分页查询
     */
    @Select("SELECT * FROM billing_bill WHERE user_id = #{userId} AND is_deleted = 0 ORDER BY created_at DESC")
    IPage<Bill> selectPageByUserId(Page<Bill> page, @Param("userId") Long userId);

    /**
     * 查询用户待支付账单
     */
    @Select("SELECT * FROM billing_bill WHERE user_id = #{userId} AND status = 0 AND is_deleted = 0 ORDER BY bill_due_date ASC")
    List<Bill> selectPendingBills(@Param("userId") Long userId);

    /**
     * 查询用户指定账期的账单
     */
    @Select("SELECT * FROM billing_bill WHERE user_id = #{userId} AND bill_start_date = #{startDate} AND bill_end_date = #{endDate} AND is_deleted = 0")
    Bill selectByPeriod(@Param("userId") Long userId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /**
     * 查询所有待生成账单的日期
     */
    @Select("SELECT DISTINCT user_id FROM billing_token_usage WHERE is_billed = 0 AND is_deleted = 0 GROUP BY user_id HAVING SUM(total_cost) > 0")
    List<Long> selectUserIdsWithUnbilledUsage();

    /**
     * 统计用户未计费金额
     */
    @Select("SELECT COALESCE(SUM(total_cost), 0) FROM billing_token_usage WHERE user_id = #{userId} AND is_billed = 0 AND is_deleted = 0")
    BigDecimal selectUnbilledAmount(@Param("userId") Long userId);

    /**
     * 标记已计费
     */
    @Update("UPDATE billing_token_usage SET is_billed = 1, billed_at = NOW() " +
            "WHERE user_id = #{userId} AND is_billed = 0 AND is_deleted = 0 " +
            "AND usage_date BETWEEN #{startDate} AND #{endDate}")
    int markAsBilled(@Param("userId") Long userId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
}
