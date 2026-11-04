package com.aisaas.user.service.impl;

import com.aisaas.common.util.RedisUtils;
import com.aisaas.user.cache.UserBloomFilter;
import com.aisaas.user.dto.UpdateProfileDTO;
import com.aisaas.user.entity.UserAccount;
import com.aisaas.user.mapper.UserAccountMapper;
import com.aisaas.user.mapper.UserOauthBindingMapper;
import com.aisaas.user.mapper.UserSettingsMapper;
import com.aisaas.user.service.PermissionService;
import com.aisaas.user.service.QuotaService;
import com.aisaas.user.service.RoleService;
import com.aisaas.user.service.UserService;
import com.aisaas.user.service.VipService;
import com.aisaas.user.util.JwtUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 缓存一致性测试（自包含 Spring 上下文，不依赖 MySQL/Redis）：
 * 验证 updateProfile 写操作后，@CacheEvict 真的把 user 缓存按 userId 清掉，
 * 后续 getById 会重新回源 DB 拿到最新值（而不是命中旧的缓存）。
 */
class UserCacheEvictTest {

    @Test
    @DisplayName("updateProfile 后缓存失效: 再读 getById 回源 DB 取到新昵称")
    void updateProfile_evictsUserCache_thenReadReturnsFresh() {
        UserAccountMapper mapper = mock(UserAccountMapper.class);
        UserBloomFilter bloom = mock(UserBloomFilter.class);
        RedisUtils redis = mock(RedisUtils.class);
        RedisTemplate<String, Object> redisTemplate = mock(RedisTemplate.class);
        RedissonClient redissonClient = mock(RedissonClient.class);
        RoleService roleService = mock(RoleService.class);
        PermissionService permissionService = mock(PermissionService.class);
        QuotaService quotaService = mock(QuotaService.class);
        VipService vipService = mock(VipService.class);
        BCryptPasswordEncoder encoder = mock(BCryptPasswordEncoder.class);
        JwtUtil jwtUtil = mock(JwtUtil.class);
        UserSettingsMapper userSettingsMapper = mock(UserSettingsMapper.class);
        UserOauthBindingMapper userOauthBindingMapper = mock(UserOauthBindingMapper.class);

        // 布隆预筛放行(认为存在), 不走 Redis
        when(bloom.mightContain(anyLong())).thenReturn(true);

        // updateProfile 成功后组装 UserInfoDTO 会调用这些协作者, 打桩返回空集合/空 VIP
        when(roleService.getUserRoleCodes(anyLong())).thenReturn(java.util.List.of());
        when(permissionService.getUserPermissionCodes(anyLong())).thenReturn(java.util.Set.of());
        when(vipService.getUserVipInfo(anyLong())).thenReturn(com.aisaas.common.result.Result.success(null));

        // 同一 UserAccount 实例: 第一次读 old, 写操作把它改成 new, 再读应拿到 new(证明回源且缓存被清)
        UserAccount user = new UserAccount();
        user.setId(1L);
        user.setUsername("old");
        user.setIsDeleted(0);
        user.setStatus(1);
        when(mapper.selectById(1L)).thenReturn(user);
        when(mapper.countByUsername("new")).thenReturn(0L);
        when(mapper.updateById(any())).thenReturn(1);

        try (AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext()) {
            ctx.registerBean(UserAccountMapper.class, () -> mapper);
            ctx.registerBean(UserBloomFilter.class, () -> bloom);
            ctx.registerBean(RedisUtils.class, () -> redis);
            ctx.registerBean(RedisTemplate.class, () -> redisTemplate);
            ctx.registerBean(RedissonClient.class, () -> redissonClient);
            ctx.registerBean(RoleService.class, () -> roleService);
            ctx.registerBean(PermissionService.class, () -> permissionService);
            ctx.registerBean(QuotaService.class, () -> quotaService);
            ctx.registerBean(VipService.class, () -> vipService);
            ctx.registerBean(BCryptPasswordEncoder.class, () -> encoder);
            ctx.registerBean(JwtUtil.class, () -> jwtUtil);
            ctx.registerBean(UserSettingsMapper.class, () -> userSettingsMapper);
            ctx.registerBean(UserOauthBindingMapper.class, () -> userOauthBindingMapper);
            ctx.registerBean(UserServiceImpl.class,
                    () -> new UserServiceImpl(mapper, roleService, permissionService, quotaService,
                            vipService, encoder, jwtUtil, userSettingsMapper,
                            userOauthBindingMapper, redis, bloom));
            ctx.register(CacheConfig.class);
            ctx.refresh();

            UserService svc = ctx.getBean(UserService.class);

            // 1) 首次读取 -> 缓存未命中 -> 回源, 得到 old
            UserAccount first = svc.getById(1L);
            assertEquals("old", first.getUsername());
            verify(mapper, times(1)).selectById(1L);

            // 2) 写操作: 改昵称为 new
            UpdateProfileDTO dto = new UpdateProfileDTO();
            dto.setUsername("new");
            svc.updateProfile(1L, dto);

            // 3) 再次读取 -> 缓存应已被 @CacheEvict 清掉 -> 重新回源 -> 拿到 new
            UserAccount second = svc.getById(1L);
            assertEquals("new", second.getUsername(),
                    "updateProfile 后未失效缓存, 读到了旧值, 缓存一致性 bug 仍存在");
            // 首次(1) + updateProfile 内部 self-call(1) + 失效后再次回源(1) = 3 次; 若未失效只会是 2 次
            verify(mapper, times(3)).selectById(1L);
        }
    }

    @Configuration
    @EnableCaching
    static class CacheConfig {
        @Bean
        public CacheManager cacheManager() {
            return new ConcurrentMapCacheManager();
        }
    }
}
