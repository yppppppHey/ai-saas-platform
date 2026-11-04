package com.aisaas.billing.schedule;

import com.aisaas.billing.dto.TokenUsageRecordDTO;
import com.aisaas.billing.service.TokenUsageService;
import com.aisaas.common.mq.message.TokenUsageMessage;
import com.aisaas.common.result.Result;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * 对账补偿任务单元测试
 */
@ExtendWith(MockitoExtension.class)
class UsageReconcileTaskTest {

    private static final String PENDING_KEY = TokenUsageMessage.PENDING_KEY;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private HashOperations<String, Object, Object> hashOperations;

    @Mock
    private TokenUsageService tokenUsageService;

    private UsageReconcileTask task;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<Object, Object> pendingStore = new HashMap<>();

    @BeforeEach
    void setUp() {
        task = new UsageReconcileTask(stringRedisTemplate, tokenUsageService, objectMapper);
        lenient().when(stringRedisTemplate.opsForHash()).thenReturn(hashOperations);
        lenient().when(hashOperations.entries(PENDING_KEY)).thenReturn(pendingStore);
        // 删除操作同步维护内存 map, 便于 verify
        lenient().doAnswer(inv -> {
            pendingStore.remove(inv.getArgument(1));
            return null;
        }).when(hashOperations).delete(eq(PENDING_KEY), any());
    }

    private void putPending(String usageId, long sendTimeMs) throws Exception {
        TokenUsageMessage msg = TokenUsageMessage.builder()
                .usageId(usageId).userId(1L).provider("openai")
                .modelId("gpt-3.5-turbo").operationType("chat")
                .promptTokens(10).completionTokens(20).totalTokens(30)
                .build();
        pendingStore.put(usageId, sendTimeMs + "|" + objectMapper.writeValueAsString(msg));
    }

    @Test
    @DisplayName("pending 为空: 不触发任何补偿")
    void reconcile_emptyPending() {
        task.reconcile();
        verifyNoInteractions(tokenUsageService);
    }

    @Test
    @DisplayName("滞留超时且重放入账成功: 清理 pending")
    void reconcile_staleEntryReplayed() throws Exception {
        putPending("u-1", System.currentTimeMillis() - 11 * 60 * 1000L);
        when(tokenUsageService.recordTokenUsageIdempotent(any(TokenUsageRecordDTO.class),
                eq("u-1"))).thenReturn(Result.success());

        task.reconcile();

        verify(tokenUsageService).recordTokenUsageIdempotent(any(TokenUsageRecordDTO.class), eq("u-1"));
        assertFalse(pendingStore.containsKey("u-1"), "入账成功后 pending 应被清理");
    }

    @Test
    @DisplayName("重放失败: pending 保留, 等待下一轮")
    void reconcile_replayFailed_keepsPending() throws Exception {
        putPending("u-2", System.currentTimeMillis() - 11 * 60 * 1000L);
        when(tokenUsageService.recordTokenUsageIdempotent(any(TokenUsageRecordDTO.class),
                eq("u-2"))).thenReturn(Result.error("db down"));

        task.reconcile();

        assertTrue(pendingStore.containsKey("u-2"), "补偿失败 pending 应保留");
    }

    @Test
    @DisplayName("未超时: 正常消费窗口内不补偿")
    void reconcile_freshEntrySkipped() throws Exception {
        putPending("u-3", System.currentTimeMillis());
        // 未超时会跳过补偿，此 stub 不应被调用 -> lenient 避免 UnnecessaryStubbing
        lenient().when(tokenUsageService.recordTokenUsageIdempotent(any(TokenUsageRecordDTO.class),
                anyString())).thenReturn(Result.success());

        task.reconcile();

        verify(tokenUsageService, never()).recordTokenUsageIdempotent(any(), anyString());
    }
}
