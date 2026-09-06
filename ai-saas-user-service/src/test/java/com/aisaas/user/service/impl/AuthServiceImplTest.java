package com.aisaas.user.service.impl;

import com.aisaas.common.util.RedisKeys;
import com.aisaas.common.util.RedisUtils;
import com.aisaas.common.util.TokenBlacklistSupport;
import com.aisaas.user.entity.UserAccount;
import com.aisaas.user.mapper.UserAccountMapper;
import com.aisaas.user.mapper.UserLoginLogMapper;
import com.aisaas.user.service.QuotaService;
import com.aisaas.user.service.RoleService;
import com.aisaas.user.util.JwtUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * 认证服务测试：Token 黑名单（登出 / 改密强制下线）
 *
 * 覆盖的坑：黑名单键必须同时满足两点才有效——
 * 1) 键规则与网关 TokenBlacklistFilter 一致（PREFIX + SHA256(token)）
 * 2) TTL 至少要覆盖 token 剩余有效期，否则登出"提前失效"等于没登出
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthServiceImplTest {

    private static final String TOKEN = "eyJhbGciOi.test.token";

    @Mock
    private UserAccountMapper userAccountMapper;
    @Mock
    private UserLoginLogMapper userLoginLogMapper;
    @Mock
    private RedisUtils redisUtils;
    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private RoleService roleService;
    @Mock
    private QuotaService quotaService;

    @InjectMocks
    private AuthServiceImpl authService;

    @Test
    @DisplayName("登出: 写入与网关一致的黑名单键(SHA256哈希), TTL 覆盖 token 剩余有效期")
    void logout_writesBlacklistKeyMatchingGateway() {
        when(jwtUtil.getExpiration()).thenReturn(3600L);
        when(jwtUtil.getUserIdFromToken(TOKEN)).thenReturn(1L);

        var result = authService.logout(TOKEN);

        assertTrue(result.isSuccess());
        String expectedKey = TokenBlacklistSupport.blacklistKey(TOKEN);
        assertTrue(expectedKey.startsWith(RedisKeys.TOKEN_BLACKLIST));
        assertNotEquals(RedisKeys.TOKEN_BLACKLIST + TOKEN, expectedKey, "不应明文存 token");
        verify(redisUtils).set(eq(expectedKey), eq("1"), eq(3600L), eq(TimeUnit.SECONDS));
        verify(redisUtils).delete(RedisKeys.USER_TOKEN + 1L);
    }

    @Test
    @DisplayName("登出: token 为空时直接成功, 不写 Redis")
    void logout_blankToken_noop() {
        var result = authService.logout(null);
        assertTrue(result.isSuccess());
        verify(redisUtils, never()).set(anyString(), any(), anyLong(), any());
    }

    @Test
    @DisplayName("黑名单键规则: 同 token 幂等, 不同 token 不碰撞")
    void blacklistKey_stableAndUnique() {
        assertEquals(TokenBlacklistSupport.blacklistKey(TOKEN),
                TokenBlacklistSupport.blacklistKey(TOKEN));
        assertNotEquals(TokenBlacklistSupport.blacklistKey(TOKEN),
                TokenBlacklistSupport.blacklistKey(TOKEN + "-other"));
    }
}
