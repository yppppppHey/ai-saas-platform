package com.aisaas.billing.service;

import com.aisaas.billing.dto.*;
import com.aisaas.common.result.Result;
import com.baomidou.mybatisplus.core.metadata.IPage;

import java.util.Map;

/**
 * 支付服务接口
 */
public interface PaymentService {

    /**
     * 创建支付订单
     */
    Result<PaymentCreateResultVO> createPayment(PaymentCreateDTO dto);

    /**
     * 获取支付详情
     */
    Result<PaymentVO> getPaymentDetail(String paymentId);

    /**
     * 分页查询用户的支付记录
     */
    Result<IPage<PaymentVO>> queryUserPayments(PaymentQueryDTO dto);

    /**
     * 支付回调处理
     */
    Result<Void> handlePaymentCallback(PaymentCallbackDTO dto);

    /**
     * 查询支付状态
     */
    Result<PaymentStatusVO> queryPaymentStatus(String paymentId);

    /**
     * 关闭支付订单
     */
    Result<Void> closePayment(String paymentId);

    /**
     * 申请退款
     */
    Result<RefundResultVO> applyRefund(RefundApplyDTO dto);

    /**
     * 查询退款状态
     */
    Result<RefundStatusVO> queryRefundStatus(String refundId);

    /**
     * 获取支持的支付方式
     */
    Result<Map<String, Object>> getSupportedPaymentMethods();
}
