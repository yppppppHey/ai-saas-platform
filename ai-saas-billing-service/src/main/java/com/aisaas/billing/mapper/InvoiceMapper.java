package com.aisaas.billing.mapper;

import com.aisaas.billing.entity.Invoice;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 发票Mapper
 */
@Mapper
public interface InvoiceMapper extends BaseMapper<Invoice> {

    /**
     * 根据发票ID查询
     */
    @Select("SELECT * FROM billing_invoice WHERE invoice_id = #{invoiceId} AND is_deleted = 0")
    Invoice selectByInvoiceId(@Param("invoiceId") String invoiceId);

    /**
     * 分页查询用户的发票
     */
    @Select("SELECT * FROM billing_invoice WHERE user_id = #{userId} AND is_deleted = 0 ORDER BY created_at DESC")
    IPage<Invoice> selectPageByUserId(Page<Invoice> page, @Param("userId") Long userId);

    /**
     * 查询用户待处理的发票申请
     */
    @Select("SELECT * FROM billing_invoice WHERE user_id = #{userId} AND status = 0 AND is_deleted = 0 ORDER BY applied_at DESC")
    List<Invoice> selectPendingInvoices(@Param("userId") Long userId);

    /**
     * 查询用户可开票金额(已支付但未开票的账单金额)
     */
    @Select("SELECT COALESCE(SUM(payable_amount), 0) FROM billing_bill " +
            "WHERE user_id = #{userId} AND status = 1 AND bill_id NOT IN " +
            "(SELECT bill_ids FROM billing_invoice WHERE user_id = #{userId} AND status IN (0,1,2)) " +
            "AND is_deleted = 0")
    java.math.BigDecimal selectInvoicableAmount(@Param("userId") Long userId);

    /**
     * 更新发票状态
     */
    @Update("UPDATE billing_invoice SET status = #{status}, issued_at = NOW(), remark = #{remark}, " +
            "updated_at = NOW() WHERE invoice_id = #{invoiceId}")
    int updateStatus(@Param("invoiceId") String invoiceId, @Param("status") Integer status, @Param("remark") String remark);

    /**
     * 拒绝发票申请
     */
    @Update("UPDATE billing_invoice SET status = 3, reject_reason = #{rejectReason}, " +
            "updated_at = NOW() WHERE invoice_id = #{invoiceId}")
    int rejectInvoice(@Param("invoiceId") String invoiceId, @Param("rejectReason") String rejectReason);
}
