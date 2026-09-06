package com.aisaas.billing.service.impl;

import com.aisaas.billing.dto.TokenUsageRecordDTO;
import com.aisaas.billing.entity.TokenUsageRecord;
import com.aisaas.billing.mapper.PriceConfigMapper;
import com.aisaas.billing.mapper.TokenUsageRecordMapper;
import com.aisaas.common.result.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * 计费幂等逻辑单元测试
 * 覆盖消费端三重防重的服务层部分：DB 查重 / 正常入账 / 唯一键冲突兜底
 */
@ExtendWith(MockitoExtension.class)
class TokenUsageServiceImplTest {

    private static final String USAGE_ID = "usage-0001";

    @Mock
    private TokenUsageRecordMapper tokenUsageRecordMapper;

    @Mock
    private PriceConfigMapper priceConfigMapper;

    @InjectMocks
    private TokenUsageServiceImpl service;

    private TokenUsageRecordDTO dto;

    @BeforeEach
    void setUp() {
        dto = TokenUsageRecordDTO.builder()
                .userId(1L)
                .conversationId(100L)
                .messageId(1000L)
                .provider("openai")
                .modelId("gpt-3.5-turbo")
                .operationType("chat")
                .promptTokens(100)
                .completionTokens(200)
                .totalTokens(300)
                .build();
        // 价格配置缺失 -> 成本按 0 计算，聚焦幂等逻辑本身
        lenient().when(priceConfigMapper.selectValidConfig(anyString(), anyString(),
                anyString(), any())).thenReturn(null);
    }

    @Test
    @DisplayName("首次入账: 插入记录并沿用消息携带的 usageId, is_billed=1")
    void recordIdempotent_firstInsert() {
        when(tokenUsageRecordMapper.selectCount(any())).thenReturn(0L);
        when(tokenUsageRecordMapper.insert(any(TokenUsageRecord.class))).thenReturn(1);

        Result<Void> result = service.recordTokenUsageIdempotent(dto, USAGE_ID);

        assertTrue(result.isSuccess());
        verify(tokenUsageRecordMapper).insert(argThat(record ->
                USAGE_ID.equals(record.getUsageId())
                        && record.getIsBilled() == 1
                        && record.getBilledAt() != null
                        && Integer.valueOf(300).equals(record.getTotalTokens())));
    }

    @Test
    @DisplayName("幂等命中: usageId 已存在则不再插入")
    void recordIdempotent_duplicateSkipped() {
        when(tokenUsageRecordMapper.selectCount(any())).thenReturn(1L);

        Result<Void> result = service.recordTokenUsageIdempotent(dto, USAGE_ID);

        assertTrue(result.isSuccess());
        verify(tokenUsageRecordMapper, never()).insert(any(TokenUsageRecord.class));
    }

    @Test
    @DisplayName("并发兜底: 唯一键冲突视为成功(幂等)")
    void recordIdempotent_duplicateKeyTreatedAsSuccess() {
        when(tokenUsageRecordMapper.selectCount(any())).thenReturn(0L);
        when(tokenUsageRecordMapper.insert(any(TokenUsageRecord.class)))
                .thenThrow(new DuplicateKeyException("uk_usage_id"));

        Result<Void> result = service.recordTokenUsageIdempotent(dto, USAGE_ID);

        assertTrue(result.isSuccess());
    }

    @Test
    @DisplayName("其他异常: 返回失败, 由调用方触发 MQ 重试/对账补偿")
    void recordIdempotent_otherExceptionFails() {
        when(tokenUsageRecordMapper.selectCount(any())).thenReturn(0L);
        when(tokenUsageRecordMapper.insert(any(TokenUsageRecord.class)))
                .thenThrow(new RuntimeException("db down"));

        Result<Void> result = service.recordTokenUsageIdempotent(dto, USAGE_ID);

        assertFalse(result.isSuccess());
    }
}
