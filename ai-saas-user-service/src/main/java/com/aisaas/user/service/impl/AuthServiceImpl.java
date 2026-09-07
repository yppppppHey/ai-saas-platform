package com.aisaas.user.service.impl;

import com.aisaas.common.constant.ResultCode;
import com.aisaas.common.exception.BizException;
import com.aisaas.common.result.Result;
import com.aisaas.common.util.RedisKeys;
import com.aisaas.common.util.RedisUtils;
import com.aisaas.user.dto.*;
import com.aisaas.user.entity.UserAccount;
import com.aisaas.user.entity.UserLoginLog;
import com.aisaas.user.mapper.UserAccountMapper;
import com.aisaas.user.mapper.UserLoginLogMapper;
import com.aisaas.user.service.AuthService;
import com.aisaas.user.service.QuotaService;
import com.aisaas.user.service.UserService;
import com.aisaas.user.service.RoleService;
import com.aisaas.user.util.JwtUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 认证服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl extends ServiceImpl<UserAccountMapper, UserAccount> implements AuthService {

    private final UserAccountMapper userAccountMapper;
    private final UserLoginLogMapper userLoginLogMapper;
    private final RedisUtils redisUtils;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder passwordEncoder;
    private final RoleService roleService;
    private final QuotaService quotaService;
    private final UserService userService;
    private final com.aisaas.user.messaging.VerifyCodeNotifier verifyCodeNotifier;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<TokenDTO> register(RegisterDTO registerDTO) {
        // 验证密码是否一致
        if (!registerDTO.getPassword().equals(registerDTO.getConfirmPassword())) {
            return Result.error(ResultCode.BAD_REQUEST, "两次输入的密码不一致");
        }

        // 验证验证码
        String verifyKey = RedisKeys.USER_VERIFY_CODE + registerDTO.getVerifyKey();
        String cachedCode = (String) redisUtils.get(verifyKey);
        if (!registerDTO.getVerifyCode().equalsIgnoreCase(cachedCode)) {
            return Result.error(ResultCode.BAD_REQUEST, "验证码错误或已过期");
        }

        // 检查用户名是否存在
        if (userAccountMapper.countByUsername(registerDTO.getUsername()) > 0) {
            return Result.error(ResultCode.CONFLICT, "用户名已存在");
        }

        // 检查邮箱是否存在
        if (userAccountMapper.countByEmail(registerDTO.getEmail()) > 0) {
            return Result.error(ResultCode.CONFLICT, "邮箱已被注册");
        }

        // 检查手机号是否存在
        if (StringUtils.hasText(registerDTO.getPhone()) && 
            userAccountMapper.countByPhone(registerDTO.getPhone()) > 0) {
            return Result.error(ResultCode.CONFLICT, "手机号已被注册");
        }

        // 创建用户
        UserAccount user = new UserAccount();
        user.setUsername(registerDTO.getUsername());
        user.setEmail(registerDTO.getEmail());
        user.setPhone(registerDTO.getPhone());
        user.setPasswordHash(passwordEncoder.encode(registerDTO.getPassword()));
        user.setUserType(1); // 普通用户
        user.setStatus(1); // 正常状态
        user.setIsDeleted(0);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        userAccountMapper.insert(user);

        // 分配默认角色
        roleService.assignRole(user.getId(), 1L, null); // 1L 是普通用户角色

        // 初始化用户配额
        quotaService.initUserQuota(user.getId());

        // 删除验证码
        redisUtils.delete(verifyKey);

        // 生成Token
        TokenDTO tokenDTO = generateToken(user);

        log.info("用户注册成功: {}", user.getUsername());
        return Result.success(tokenDTO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<TokenDTO> login(LoginDTO loginDTO) {
        // 查询用户
        UserAccount user = userAccountMapper.selectByAccount(loginDTO.getAccount());
        if (user == null) {
            // 记录登录失败日志
            saveLoginLog(null, loginDTO, 0, "用户不存在", null);
            return Result.error(ResultCode.INVALID_CREDENTIALS, "账号或密码错误");
        }

        // 检查用户状态
        if (user.getStatus() == 0) {
            saveLoginLog(user.getId(), loginDTO, 0, "账号已被禁用", null);
            return Result.error(ResultCode.FORBIDDEN, "账号已被禁用");
        }

        // 验证密码
        if (!passwordEncoder.matches(loginDTO.getPassword(), user.getPasswordHash())) {
            saveLoginLog(user.getId(), loginDTO, 0, "密码错误", null);
            return Result.error(ResultCode.INVALID_CREDENTIALS, "账号或密码错误");
        }

        // 生成Token
        TokenDTO tokenDTO = generateToken(user);

        // 更新用户登录信息
        user.setLastLoginAt(LocalDateTime.now());
        user.setLastLoginIp(getClientIp());
        user.setUpdatedAt(LocalDateTime.now());
        userAccountMapper.updateById(user);

        // 记录登录成功日志
        saveLoginLog(user.getId(), loginDTO, 1, null, tokenDTO.getAccessToken());

        log.info("用户登录成功: {}", user.getUsername());
        return Result.success(tokenDTO);
    }

    @Override
    public Result<Void> logout(String token) {
        if (!StringUtils.hasText(token)) {
            return Result.success();
        }

        // 将Token加入黑名单（键规则与网关 TokenBlacklistFilter 共用，必须一致）
        String tokenKey = com.aisaas.common.util.TokenBlacklistSupport.blacklistKey(token);
        long ttlSeconds = Math.max(jwtUtil.getExpiration(), 1L);
        redisUtils.set(tokenKey, "1", ttlSeconds, TimeUnit.SECONDS);

        // 删除Token缓存
        String userTokenKey = RedisKeys.USER_TOKEN + jwtUtil.getUserIdFromToken(token);
        redisUtils.delete(userTokenKey);

        log.info("用户登出成功, Token: {}", token);
        return Result.success();
    }

    @Override
    public Result<TokenDTO> refreshToken(String refreshToken) {
        if (!StringUtils.hasText(refreshToken)) {
            return Result.error(ResultCode.BAD_REQUEST, "刷新令牌不能为空");
        }

        // 验证刷新令牌
        if (!jwtUtil.validateToken(refreshToken)) {
            return Result.error(ResultCode.TOKEN_EXPIRED, "刷新令牌已过期或无效");
        }

        // 检查刷新令牌是否在黑名单中
        String blacklistKey = RedisKeys.TOKEN_BLACKLIST + refreshToken;
        if (redisUtils.hasKey(blacklistKey)) {
            return Result.error(ResultCode.TOKEN_INVALID, "刷新令牌已被撤销");
        }

        Long userId = jwtUtil.getUserIdFromToken(refreshToken);
        UserAccount user = userService.getById(userId);
        if (user == null || user.getIsDeleted() == 1) {
            return Result.error(ResultCode.NOT_FOUND, "用户不存在");
        }

        if (user.getStatus() == 0) {
            return Result.error(ResultCode.FORBIDDEN, "账号已被禁用");
        }

        // 生成新的Token对
        TokenDTO tokenDTO = generateToken(user);

        // 将旧的刷新令牌加入黑名单
        redisUtils.set(blacklistKey, "1", jwtUtil.getExpiration(), TimeUnit.SECONDS);

        log.info("Token刷新成功, userId: {}", userId);
        return Result.success(tokenDTO);
    }

    @Override
    public Result<String> sendVerifyCode(String target, String type) {
        // 生成6位验证码
        String code = String.format("%06d", (int) (Math.random() * 1000000));
        String key = UUID.randomUUID().toString().replace("-", "");

        // 验证码缓存5分钟：一份按 key（前端回传），一份按目标（服务端直接校验，如找回密码）
        redisUtils.set(RedisKeys.USER_VERIFY_CODE + key, code, 5, TimeUnit.MINUTES);
        redisUtils.set(RedisKeys.USER_VERIFY_CODE + target, code, 5, TimeUnit.MINUTES);

        // 发送（默认日志实现，接入短信/邮件时替换 VerifyCodeNotifier 实现即可）
        try {
            verifyCodeNotifier.send(target, type, code);
        } catch (Exception e) {
            log.error("验证码发送失败: target={}, type={}", target, type, e);
            return Result.error(ResultCode.SERVICE_UNAVAILABLE, "验证码发送失败，请稍后重试");
        }
        log.info("验证码已发送到 {}: key: {}", target, key);

        return Result.success(key);
    }

    @Override
    public Result<Boolean> verifyCode(String target, String code, String key) {
        String redisKey = RedisKeys.USER_VERIFY_CODE + key;
        String cachedCode = (String) redisUtils.get(redisKey);
        
        if (cachedCode == null) {
            return Result.success(false);
        }
        
        boolean valid = cachedCode.equalsIgnoreCase(code);
        if (valid) {
            // 验证成功后删除验证码
            redisUtils.delete(redisKey);
        }
        
        return Result.success(valid);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> resetPassword(String email, String newPassword, String verifyCode) {
        // 查询用户
        UserAccount user = userAccountMapper.selectByEmail(email);
        if (user == null) {
            return Result.error(ResultCode.NOT_FOUND, "用户不存在");
        }

        // 验证验证码（按目标存储的那一份，找回密码场景前端不传 key）
        String cachedCode = (String) redisUtils.get(RedisKeys.USER_VERIFY_CODE + email);
        if (!org.springframework.util.StringUtils.hasText(cachedCode)) {
            return Result.error(ResultCode.BAD_REQUEST, "验证码已过期，请重新获取");
        }
        if (!cachedCode.equalsIgnoreCase(verifyCode)) {
            return Result.error(ResultCode.BAD_REQUEST, "验证码错误");
        }
        redisUtils.delete(RedisKeys.USER_VERIFY_CODE + email);

        // 更新密码
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setUpdatedAt(LocalDateTime.now());
        userAccountMapper.updateById(user);

        // 修改密码后强制下线：把该用户当前有效 token 拉黑（键规则与网关一致）
        try {
            String currentToken = (String) redisUtils.get(RedisKeys.USER_TOKEN + user.getId());
            if (org.springframework.util.StringUtils.hasText(currentToken)) {
                String blacklistKey = com.aisaas.common.util.TokenBlacklistSupport.blacklistKey(currentToken);
                long ttlSeconds = Math.max(jwtUtil.getExpiration(), 1L);
                redisUtils.set(blacklistKey, "1", ttlSeconds, TimeUnit.SECONDS);
                redisUtils.delete(RedisKeys.USER_TOKEN + user.getId());
                log.info("密码重置后已强制下线: userId={}", user.getId());
            }
        } catch (Exception e) {
            log.warn("密码重置后强制下线失败(不影响重置结果): userId={}", user.getId(), e);
        }

        log.info("密码重置成功: {}", email);
        return Result.success();
    }

    /**
     * 生成Token
     */
    private TokenDTO generateToken(UserAccount user) {
        // 生成访问令牌
        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getUsername());
        
        // 生成刷新令牌
        String refreshToken = jwtUtil.generateRefreshToken(user.getId());

        // 获取过期时间
        Long expiresIn = jwtUtil.getExpiration();

        TokenDTO tokenDTO = new TokenDTO();
        tokenDTO.setAccessToken(accessToken);
        tokenDTO.setRefreshToken(refreshToken);
        tokenDTO.setTokenType("Bearer");
        tokenDTO.setExpiresIn(expiresIn);
        tokenDTO.setExpireTime(LocalDateTime.now().plusSeconds(expiresIn));
        tokenDTO.setRefreshExpireTime(LocalDateTime.now().plusDays(7));

        // 缓存Token
        String userTokenKey = RedisKeys.USER_TOKEN + user.getId();
        redisUtils.set(userTokenKey, accessToken, expiresIn, TimeUnit.SECONDS);

        return tokenDTO;
    }

    /**
     * 保存登录日志
     */
    private void saveLoginLog(Long userId, LoginDTO loginDTO, Integer loginStatus, 
                               String failReason, String token) {
        try {
            UserLoginLog log = new UserLoginLog();
            log.setUserId(userId);
            log.setLoginType(1); // 密码登录
            log.setLoginIp(getClientIp());
            log.setDeviceType(loginDTO.getDeviceType());
            log.setDeviceId(loginDTO.getDeviceId());
            log.setLoginStatus(loginStatus);
            log.setFailReason(failReason);
            log.setCreatedAt(LocalDateTime.now());
            userLoginLogMapper.insert(log);
        } catch (Exception e) {
            log.error("保存登录日志失败", e);
        }
    }

    /**
     * 获取客户端IP
     */
    private String getClientIp() {
        try {
            org.springframework.web.context.request.ServletRequestAttributes attrs =
                    (org.springframework.web.context.request.ServletRequestAttributes)
                            org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
            if (attrs == null) {
                return "127.0.0.1";
            }
            jakarta.servlet.http.HttpServletRequest request = attrs.getRequest();
            // 反向代理场景取真实来源: X-Forwarded-For 首段 -> X-Real-IP -> remoteAddr
            String ip = request.getHeader("X-Forwarded-For");
            if (org.springframework.util.StringUtils.hasText(ip) && !"unknown".equalsIgnoreCase(ip)) {
                return ip.split(",")[0].trim();
            }
            ip = request.getHeader("X-Real-IP");
            if (org.springframework.util.StringUtils.hasText(ip) && !"unknown".equalsIgnoreCase(ip)) {
                return ip;
            }
            return request.getRemoteAddr();
        } catch (Exception e) {
            log.warn("获取客户端IP失败", e);
            return "127.0.0.1";
        }
    }
}
