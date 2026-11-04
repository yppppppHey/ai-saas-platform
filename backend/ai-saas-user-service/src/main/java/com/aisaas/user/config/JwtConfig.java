package com.aisaas.user.config;

import com.aisaas.user.util.JwtUtil;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * JWT配置类
 */
@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class JwtConfig {

    @Bean
    public JwtUtil jwtUtil(JwtProperties jwtProperties) {
        JwtUtil jwtUtil = new JwtUtil();
        jwtUtil.setSecret(jwtProperties.getSecret());
        jwtUtil.setAccessExpiration(jwtProperties.getAccessExpiration());
        jwtUtil.setRefreshExpiration(jwtProperties.getRefreshExpiration());
        jwtUtil.setIssuer(jwtProperties.getIssuer());
        return jwtUtil;
    }
}
