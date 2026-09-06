package com.aisaas.billing.service;

import com.aisaas.billing.dto.*;
import com.aisaas.common.result.Result;
import com.baomidou.mybatisplus.core.metadata.IPage;

import java.util.List;

/**
 * 账单服务接口
 */
public interface BillService {

    /**
     * 生成账单
     */
    Result<BillVO> generateBill(BillGenerateDTO dto);

    /**
     * 批量生成账单
     */
    Result<BatchBillGenerateResultVO> batchGenerateBills(BatchBillGenerateDTO dto);

    /**
     * 获取账单详情
     */
    Result<BillVO> getBillDetail(String billId);

    /**
     * 分页查询用户的账单
     */
    Result<IPage<BillVO>> queryUserBills(BillQueryDTO dto);

    /**
     * 查询账单统计
     */
    Result<BillStatisticsVO> getBillStatistics(Long userId);

    /**
     * 取消账单
     */
    Result<Void> cancelBill(String billId, String reason);

    /**
     * 导出账单
     */
    Result<String> exportBills(BillExportDTO dto);
}
