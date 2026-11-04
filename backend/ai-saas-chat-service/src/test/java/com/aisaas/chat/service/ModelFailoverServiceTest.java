package com.aisaas.chat.service;

import com.aisaas.common.ai.dto.ChatRequest;
import com.aisaas.common.ai.dto.ChatResponse;
import com.aisaas.common.ai.provider.AIProvider;
import com.aisaas.common.ai.provider.AIProviderFactory;
import com.aisaas.common.exception.BizException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.env.MockEnvironment;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * 模型路由降级链单元测试
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ModelFailoverServiceTest {

    @Mock
    private AIProviderFactory providerFactory;

    @Mock
    private AIProvider primaryProvider;   // openai

    @Mock
    private AIProvider fallbackProvider;  // deepseek

    @Mock
    private ChatResponse successResponse;

    private ModelFailoverService service;

    @BeforeEach
    void setUp() {
        // 降级链: gpt-4 -> gpt-3.5-turbo,deepseek-chat; 默认降级 deepseek-chat
        // 构造器现通过 Binder 从 Environment 绑定 ai.failover.chains.* 键值
        MockEnvironment env = new MockEnvironment();
        env.setProperty("ai.failover.chains.gpt-4", "gpt-3.5-turbo,deepseek-chat");
        service = new ModelFailoverService(providerFactory, env, "deepseek-chat");

        when(primaryProvider.getProviderName()).thenReturn("openai");
        when(primaryProvider.supportsModel("gpt-4")).thenReturn(true);
        when(primaryProvider.supportsModel(eq("gpt-3.5-turbo"))).thenReturn(false);

        when(fallbackProvider.getProviderName()).thenReturn("deepseek");
        when(fallbackProvider.supportsModel(anyString())).thenReturn(true);

        when(successResponse.isSuccess()).thenReturn(true);

        when(providerFactory.getProvider("openai")).thenReturn(primaryProvider);
        when(providerFactory.getProviderByModel("gpt-4")).thenReturn(primaryProvider);
        when(providerFactory.getProviderByModel("gpt-3.5-turbo")).thenReturn(fallbackProvider);
        when(providerFactory.getProviderByModel("deepseek-chat")).thenReturn(fallbackProvider);
    }

    private ChatRequest request(String model) {
        return ChatRequest.builder().model(model).build();
    }

    @Test
    @DisplayName("降级链构建: 主模型 + 配置的备选(去重)")
    void buildChain_configured() {
        List<String> chain = service.buildChain("gpt-4");
        assertEquals(List.of("gpt-4", "gpt-3.5-turbo", "deepseek-chat"), chain);
    }

    @Test
    @DisplayName("降级链构建: 未配置的模型走默认降级链")
    void buildChain_defaultForUnknownModel() {
        List<String> chain = service.buildChain("unknown-model");
        assertEquals(List.of("unknown-model", "deepseek-chat"), chain);
    }

    @Test
    @DisplayName("主模型异常 -> 降级到备选模型成功")
    void chatWithFailover_primaryFailsFallbackSucceeds() {
        when(primaryProvider.chat(any())).thenThrow(new RuntimeException("connect timeout"));
        when(fallbackProvider.chat(any())).thenReturn(successResponse);

        ChatResponse resp = service.chatWithFailover("openai", request("gpt-4"));

        assertSame(successResponse, resp);
        verify(primaryProvider).chat(any());
        verify(fallbackProvider).chat(any());
    }

    @Test
    @DisplayName("主模型返回失败(flag) 同样触发降级")
    void chatWithFailover_errorFlagTriggersFailover() {
        ChatResponse failed = mock(ChatResponse.class);
        when(failed.isSuccess()).thenReturn(false);
        when(primaryProvider.chat(any())).thenReturn(failed);
        when(fallbackProvider.chat(any())).thenReturn(successResponse);

        ChatResponse resp = service.chatWithFailover("openai", request("gpt-4"));

        assertSame(successResponse, resp);
    }

    @Test
    @DisplayName("降级链全部失败 -> 抛 BizException")
    void chatWithFailover_allFailThrows() {
        when(primaryProvider.chat(any())).thenThrow(new RuntimeException("down"));
        when(fallbackProvider.chat(any())).thenThrow(new RuntimeException("down too"));

        assertThrows(BizException.class,
                () -> service.chatWithFailover("openai", request("gpt-4")));
        // 链路共 3 跳: gpt-4(openai) -> gpt-3.5-turbo(deepseek) -> deepseek-chat(deepseek)
        verify(primaryProvider).chat(any());
        verify(fallbackProvider, times(2)).chat(any());
    }

    @Test
    @DisplayName("主模型首次成功 -> 不触碰备选模型")
    void chatWithFailover_primarySuccessNoFallback() {
        when(primaryProvider.chat(any())).thenReturn(successResponse);

        ChatResponse resp = service.chatWithFailover("openai", request("gpt-4"));

        assertSame(successResponse, resp);
        verifyNoInteractions(fallbackProvider);
    }

    @Test
    @DisplayName("路由: 显式 Provider 不支持该模型时按模型路由")
    void resolveProvider_routeByModelWhenExplicitUnsupported() {
        // openai 不支持 deepseek-chat -> 应路由到支持它的 provider
        when(providerFactory.getProviderByModel("deepseek-chat")).thenReturn(fallbackProvider);

        AIProvider resolved = service.resolveProvider("openai", "deepseek-chat");

        assertSame(fallbackProvider, resolved);
    }
}
