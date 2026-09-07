package com.aisaas.chat.config;

import com.aisaas.chat.dto.ai.AiChatRequestDTO;
import com.aisaas.chat.dto.ai.AiChatResponseDTO;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import lombok.extern.slf4j.Slf4j;

/**
 * 对话核心接口的 Sentinel 限流兜底（blockHandler 必须是 public static 且签名匹配原方法 + BlockException）。
 *
 * <p>被限流时返回结构化错误（而不是抛异常），前端可直接提示"请求过于频繁"。</p>
 */
@Slf4j
public final class ChatSentinelBlockHandlers {

    private ChatSentinelBlockHandlers() {
    }

    /**
     * aiChat 资源的限流兜底：触发热点参数(userId)限流时调用。
     */
    @SuppressWarnings("unused")
    public static AiChatResponseDTO chatBlocked(Long userId, AiChatRequestDTO request, BlockException ex) {
        log.warn("对话接口触发限流: userId={}, blockType={}, resource=aiChat",
                userId, ex.getClass().getSimpleName());
        return AiChatResponseDTO.builder()
                .type("error")
                .error("请求过于频繁，请稍后再试")
                .errorCode("RATE_LIMITED")
                .done(true)
                .build();
    }
}
