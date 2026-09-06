package com.aisaas.user.messaging;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

/**
 * 默认验证码发送器：输出到日志（便于本地联调时直接从日志取码）
 */
@Slf4j
@Component
@ConditionalOnMissingBean(VerifyCodeNotifier.class)
public class LoggingVerifyCodeNotifier implements VerifyCodeNotifier {

    @Override
    public void send(String target, String type, String code) {
        log.info("[verify-code] 发送验证码 -> target={}, type={}, code={}", target, type, code);
    }
}
