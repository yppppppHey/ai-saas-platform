package com.aisaas.billing.service;

import com.aisaas.billing.dto.*;
import com.aisaas.common.result.Result;
import com.baomidou.mybatisplus.core.metadata.IPage;

import java.math.BigDecimal;
import java.util.List;

/**
 * 发票服务接口
 */
public interface InvoiceService {

    /**
     * 申请发票
     */
    Result<InvoiceVO> applyInvoice(InvoiceApplyDTO dto);

    /**
     * 获取发票详情
     */
    Result<InvoiceVO> getInvoiceDetail(String invoiceId);

    /**
     * 分页查询用户的发票
     */
    Result<IPage<InvoiceVO>> queryUserInvoices(InvoiceQueryDTO dto);

    /**
     * 获取可开票金额
     */
    Result<BigDecimal> getInvoicableAmount(Long userId);

    /**
     * 审核发票申请
     */
    Result<Void> auditInvoice(InvoiceAuditDTO dto);

    /**
     * 开具发票
     */
    Result<Void> issueInvoice(String invoiceId, String invoiceNo);

    /**
     * 拒绝发票申请
     */
    Result<Void> rejectInvoice(String invoiceId, String rejectReason);

    /**
     * 作废发票
     */
    Result<Void> voidInvoice(String invoiceId, String voidReason);

    /**
     * 获取发票抬头选项
     */
    Result<List<InvoiceTitleVO>> getInvoiceTitles(Long userId);

    /**
     * 保存发票抬头
     */
    Result<Void> saveInvoiceTitle(InvoiceTitleSaveDTO dto);

    /**
     * 删除发票抬头
     */
    Result<Void> deleteInvoiceTitle(Long titleId);
}
