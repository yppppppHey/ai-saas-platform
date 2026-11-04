package com.aisaas.billing.mapper;

import com.aisaas.billing.entity.Payment;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 支付Mapper
 */
@Mapper
public interface PaymentMapper extends BaseMapper<Payment> {

    /**
     * 根据支付ID查询
     */
    @Select("SELECT * FROM billing_payment WHERE payment_id = #{paymentId} AND is_deleted = 0")
    Payment selectByPaymentId(@Param("paymentId") String paymentId);

    /**
     * 根据支付单号查询
     */
    @Select("SELECT * FROM billing_payment WHERE payment_no = #{paymentNo} AND is_deleted = 0")
    Payment selectByPaymentNo(@Param("paymentNo") String paymentNo);

    /**
     * 根据第三方交易号查询
     */
    @Select("SELECT * FROM billing_payment WHERE third_trade_no = #{thirdTradeNo} AND is_deleted = 0")
    Payment selectByThirdTradeNo(@Param("thirdTradeNo") String thirdTradeNo);

    /**
     * 分页查询用户的支付记录
     */
    @Select("SELECT * FROM billing_payment WHERE user_id = #{userId} AND is_deleted = 0 ORDER BY created_at DESC")
    IPage<Payment> selectPageByUserId(Page<Payment> page, @Param("userId") Long userId);

    /**
     * 查询用户的待支付记录
     */
    @Select("SELECT * FROM billing_payment WHERE user_id = #{userId} AND status = 0 AND is_deleted = 0 ORDER BY created_at DESC")
    List<Payment> selectPendingPayments(@Param("userId") Long userId);

    /**
     * 更新支付状态为成功
     */
    @Update("UPDATE billing_payment SET status = 1, paid_at = NOW(), third_trade_no = #{thirdTradeNo}, " +
            "payer_info = #{payerInfo}, callback_info = #{callbackInfo}, updated_at = NOW() " +
            "WHERE payment_id = #{paymentId}")
    int updatePaymentSuccess(@Param("paymentId") String paymentId,
                             @Param("thirdTradeNo") String thirdTradeNo,
                             @Param("payerInfo") String payerInfo,
                             @Param("callbackInfo") String callbackInfo);

    /**
     * 更新支付状态为失败
     */
    @Update("UPDATE billing_payment SET status = 2, updated_at = NOW(), remark = #{remark} " +
            "WHERE payment_id = #{paymentId}")
    int updatePaymentFailed(@Param("paymentId") String paymentId, @Param("remark") String remark);

    /**
     * 关闭过期支付
     */
    @Update("UPDATE billing_payment SET status = 3, updated_at = NOW() " +
            "WHERE status = 0 AND expired_at &lt; NOW()")
    int closeExpiredPayments();
}
