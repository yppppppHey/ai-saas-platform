package com.aisaas.billing.service.impl;

import com.aisaas.billing.dto.QuotaCheckResultVO;
import com.aisaas.billing.dto.QuotaDeductResultVO;
import com.aisaas.billing.entity.QuotaRecord;
import com.aisaas.billing.mapper.QuotaConfigMapper;
import com.aisaas.billing.mapper.QuotaRecordMapper;
import com.aisaas.common.result.Result;
import com.aisaas.common.constant.ResultCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * 配额检查与扣减单元测试
 */
@ExtendWith(MockitoExtension.class)
class QuotaServiceImplTest {

    private static final Long USER_ID = 1L;
    private static final Integer QUOTA_TYPE = 1;

    @Mock
    private QuotaConfigMapper quotaConfigMapper;

    @Mock
    private QuotaRecordMapper quotaRecordMapper;

    @InjectMocks
    private QuotaServiceImpl service;

    private QuotaRecord record(Long dailyLimit, Long dailyUsed,
                               Long monthlyLimit, Long monthlyUsed) {
        QuotaRecord r = new QuotaRecord();
        r.setUserId(USER_ID);
        r.setQuotaType(QUOTA_TYPE);
        r.setDailyLimit(dailyLimit);
        r.setDailyUsed(dailyUsed);
        r.setMonthlyLimit(monthlyLimit);
        r.setMonthlyUsed(monthlyUsed);
        r.setTotalLimit(0L);
        r.setTotalUsed(0L);
        return r;
    }

    @BeforeEach
    void setUp() {
        // 默认无总量限额
    }

    @Test
    @DisplayName("checkQuota: 配额记录不存在 -> NOT_FOUND")
    void checkQuota_recordNotFound() {
        when(quotaRecordMapper.selectByUserIdAndType(USER_ID, QUOTA_TYPE)).thenReturn(null);

        Result<QuotaCheckResultVO> result = service.checkQuota(USER_ID, QUOTA_TYPE, 10L);

        assertEquals(ResultCode.NOT_FOUND.getCode(), result.getCode());
    }

    @Test
    @DisplayName("checkQuota: 每日配额不足 -> available=false 且给出原因")
    void checkQuota_dailyExceeded() {
        // 日限 100，已用 90，再要 20 -> 超限
        when(quotaRecordMapper.selectByUserIdAndType(USER_ID, QUOTA_TYPE))
                .thenReturn(record(100L, 90L, 0L, 0L));

        Result<QuotaCheckResultVO> result = service.checkQuota(USER_ID, QUOTA_TYPE, 20L);

        assertTrue(result.isSuccess());
        assertFalse(result.getData().getAvailable());
        assertEquals("每日配额不足", result.getData().getReason());
    }

    @Test
    @DisplayName("checkQuota: 月度配额兜底生效")
    void checkQuota_monthlyExceeded() {
        when(quotaRecordMapper.selectByUserIdAndType(USER_ID, QUOTA_TYPE))
                .thenReturn(record(0L, 0L, 50L, 45L));

        Result<QuotaCheckResultVO> result = service.checkQuota(USER_ID, QUOTA_TYPE, 10L);

        assertTrue(result.isSuccess());
        assertFalse(result.getData().getAvailable());
        assertEquals("每月配额不足", result.getData().getReason());
    }

    @Test
    @DisplayName("checkQuota: 额度充足 -> available=true")
    void checkQuota_available() {
        when(quotaRecordMapper.selectByUserIdAndType(USER_ID, QUOTA_TYPE))
                .thenReturn(record(100L, 90L, 0L, 0L));

        Result<QuotaCheckResultVO> result = service.checkQuota(USER_ID, QUOTA_TYPE, 10L);

        assertTrue(result.isSuccess());
        assertTrue(result.getData().getAvailable());
    }

    @Test
    @DisplayName("deductQuota: 配额记录不存在(increaseUsage=0) -> 业务失败")
    void deductQuota_recordMissing() {
        when(quotaRecordMapper.selectByUserIdAndType(USER_ID, QUOTA_TYPE))
                .thenReturn(record(100L, 0L, 0L, 0L));
        when(quotaRecordMapper.increaseUsage(eq(USER_ID), eq(QUOTA_TYPE), eq(10L))).thenReturn(0);

        Result<QuotaDeductResultVO> result = service.deductQuota(USER_ID, QUOTA_TYPE, 10L, "chat", "biz-1");

        assertEquals(ResultCode.BUSINESS_ERROR.getCode(), result.getCode());
    }

    @Test
    @DisplayName("deductQuota: 正常扣减 -> success=true 且金额回传")
    void deductQuota_success() {
        when(quotaRecordMapper.selectByUserIdAndType(USER_ID, QUOTA_TYPE))
                .thenReturn(record(100L, 0L, 0L, 0L));
        when(quotaRecordMapper.increaseUsage(eq(USER_ID), eq(QUOTA_TYPE), eq(10L))).thenReturn(1);

        Result<QuotaDeductResultVO> result = service.deductQuota(USER_ID, QUOTA_TYPE, 10L, "chat", "biz-1");

        assertTrue(result.isSuccess());
        assertTrue(result.getData().getSuccess());
        assertEquals(10L, result.getData().getDeductedAmount());
    }
}
