package com.aisaas.billing.service.impl;

import com.aisaas.billing.dto.*;
import com.aisaas.billing.entity.Bill;
import com.aisaas.billing.mapper.BillMapper;
import com.aisaas.billing.service.BillService;
import com.aisaas.common.result.Result;
import com.aisaas.common.result.ResultCode;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
public class BillServiceImpl implements BillService {

    @Autowired
    private BillMapper billMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<BillVO> generateBill(BillGenerateDTO dto) {
        try {
            // 查询用户未计费的金额
            BigDecimal unbilledAmount = billMapper.selectUnbilledAmount(dto.getUserId());
            if (unbilledAmount == null || unbilledAmount.compareTo(BigDecimal.ZERO) <= 0) {
                return Result.error(ResultCode.BUSINESS_ERROR, "没有需要计费的金额");
            }

            // 创建账单
            Bill bill = new Bill();
            bill.setBillId(generateBillId());
            bill.setBillNo(generateBillNo());
            bill.setUserId(dto.getUserId());
            bill.setBillType(dto.getBillType() != null ? dto.getBillType() : 1);
            bill.setBillPeriod(dto.getBillPeriod());
            bill.setBillStartDate(dto.getStartDate());
            bill.setBillEndDate(dto.getEndDate());
            bill.setBillDueDate(dto.getDueDate() != null ? dto.getDueDate() : LocalDate.now().plusDays(30));
            bill.setTotalAmount(unbilledAmount);
            bill.setDiscountAmount(BigDecimal.ZERO);
            bill.setPayableAmount(unbilledAmount);
            bill.setPaidAmount(BigDecimal.ZERO);
            bill.setTaxAmount(BigDecimal.ZERO);
            bill.setStatus(0); // 待支付
            bill.setCreatedAt(LocalDateTime.now());
            bill.setUpdatedAt(LocalDateTime.now());
            bill.setIsDeleted(0);

            billMapper.insert(bill);

            // 标记token使用记录为已计费
            billMapper.markAsBilled(dto.getUserId(), dto.getStartDate(), dto.getEndDate());

            log.info("账单生成成功: billId={}, userId={}, amount={}", 
                    bill.getBillId(), bill.getUserId(), bill.getTotalAmount());

            return Result.success(convertToVO(bill));
        } catch (Exception e) {
            log.error("生成账单失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR, "生成账单失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<BatchBillGenerateResultVO> batchGenerateBills(BatchBillGenerateDTO dto) {
        try {
            List<Long> userIds = billMapper.selectUserIdsWithUnbilledUsage();
            
            BatchBillGenerateResultVO vo = new BatchBillGenerateResultVO();
            int successCount = 0;
            int failCount = 0;

            for (Long userId : userIds) {
                BillGenerateDTO singleDto = new BillGenerateDTO();
                singleDto.setUserId(userId);
                singleDto.setBillType(dto.getBillType());
                singleDto.setBillPeriod(dto.getBillPeriod());
                singleDto.setStartDate(dto.getStartDate());
                singleDto.setEndDate(dto.getEndDate());
                singleDto.setDueDate(dto.getDueDate());

                Result<BillVO> result = generateBill(singleDto);
                if (result.isSuccess()) {
                    successCount++;
                } else {
                    failCount++;
                }
            }

            vo.setTotalCount(userIds.size());
            vo.setSuccessCount(successCount);
            vo.setFailCount(failCount);

            return Result.success(vo);
        } catch (Exception e) {
            log.error("批量生成账单失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR, "批量生成账单失败: " + e.getMessage());
        }
    }

    @Override
    public Result<BillVO> getBillDetail(String billId) {
        try {
            Bill bill = billMapper.selectByBillId(billId);
            if (bill == null || bill.getIsDeleted() == 1) {
                return Result.error(ResultCode.NOT_FOUND, "账单不存在");
            }
            return Result.success(convertToVO(bill));
        } catch (Exception e) {
            log.error("获取账单详情失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR, "获取账单详情失败: " + e.getMessage());
        }
    }

    @Override
    public Result<IPage<BillVO>> queryUserBills(BillQueryDTO dto) {
        try {
            Page<Bill> page = new Page<>(dto.getPageNum(), dto.getPageSize());
            IPage<Bill> billPage = billMapper.selectPageByUserId(page, dto.getUserId());

            List<BillVO> voList = billPage.getRecords().stream()
                    .map(this::convertToVO)
                    .collect(Collectors.toList());

            Page<BillVO> voPage = new Page<>(billPage.getCurrent(), billPage.getSize(), billPage.getTotal());
            voPage.setRecords(voList);

            return Result.success(voPage);
        } catch (Exception e) {
            log.error("查询用户账单失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR, "查询用户账单失败: " + e.getMessage());
        }
    }

    @Override
    public Result<BillStatisticsVO> getBillStatistics(Long userId) {
        try {
            List<Bill> bills = billMapper.selectPageByUserId(new Page<>(1, 1000), userId).getRecords();
            
            BillStatisticsVO vo = new BillStatisticsVO();
            BigDecimal totalAmount = BigDecimal.ZERO;
            BigDecimal paidAmount = BigDecimal.ZERO;
            BigDecimal unpaidAmount = BigDecimal.ZERO;
            int totalCount = 0;
            int paidCount = 0;
            int unpaidCount = 0;

            for (Bill bill : bills) {
                if (bill.getIsDeleted() == 1) continue;
                
                totalAmount = totalAmount.add(bill.getTotalAmount());
                totalCount++;

                if (bill.getStatus() == 1) { // 已支付
                    paidAmount = paidAmount.add(bill.getPayableAmount());
                    paidCount++;
                } else if (bill.getStatus() == 0) { // 待支付
                    unpaidAmount = unpaidAmount.add(bill.getPayableAmount());
                    unpaidCount++;
                }
            }

            vo.setTotalAmount(totalAmount);
            vo.setPaidAmount(paidAmount);
            vo.setUnpaidAmount(unpaidAmount);
            vo.setTotalCount(totalCount);
            vo.setPaidCount(paidCount);
            vo.setUnpaidCount(unpaidCount);

            return Result.success(vo);
        } catch (Exception e) {
            log.error("获取账单统计失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR, "获取账单统计失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> cancelBill(String billId, String reason) {
        try {
            Bill bill = billMapper.selectByBillId(billId);
            if (bill == null || bill.getIsDeleted() == 1) {
                return Result.error(ResultCode.NOT_FOUND, "账单不存在");
            }

            if (bill.getStatus() != 0) {
                return Result.error(ResultCode.BUSINESS_ERROR, "只有待支付的账单可以取消");
            }

            bill.setStatus(4); // 已取消
            bill.setRemark(reason);
            bill.setUpdatedAt(LocalDateTime.now());
            billMapper.updateById(bill);

            log.info("账单取消成功: billId={}", billId);
            return Result.success();
        } catch (Exception e) {
            log.error("取消账单失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR, "取消账单失败: " + e.getMessage());
        }
    }

    @Override
    public Result<String> exportBills(BillExportDTO dto) {
        // TODO: 实现导出逻辑
        return Result.success("");
    }

    // ==================== 私有方法 ====================

    private String generateBillId() {
        return "BIL" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) +
                UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private String generateBillNo() {
        return "B" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) +
                UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }

    private BillVO convertToVO(Bill bill) {
        BillVO vo = new BillVO();
        BeanUtils.copyProperties(bill, vo);
        return vo;
    }
}
