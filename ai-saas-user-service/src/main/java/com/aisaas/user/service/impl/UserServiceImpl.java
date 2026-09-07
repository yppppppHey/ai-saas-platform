package com.aisaas.user.service.impl;

import com.aisaas.common.constant.ResultCode;
import com.aisaas.common.result.Result;
import com.aisaas.user.dto.*;
import com.aisaas.user.entity.UserAccount;
import com.aisaas.user.entity.UserRole;
import com.aisaas.user.mapper.UserAccountMapper;
import com.aisaas.user.entity.UserOauthBinding;
import com.aisaas.user.entity.UserSettings;
import com.aisaas.user.mapper.UserOauthBindingMapper;
import com.aisaas.user.mapper.UserSettingsMapper;

import com.aisaas.user.cache.UserBloomFilter;
import com.aisaas.user.service.*;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserAccountMapper, UserAccount> implements UserService {

    private final UserAccountMapper userAccountMapper;
    private final RoleService roleService;
    private final PermissionService permissionService;
    private final QuotaService quotaService;
    private final VipService vipService;
    private final BCryptPasswordEncoder passwordEncoder;
    private final com.aisaas.user.util.JwtUtil jwtUtil;
    private final UserSettingsMapper userSettingsMapper;
    private final UserOauthBindingMapper userOauthBindingMapper;
    private final com.aisaas.common.util.RedisUtils redisUtils;
    private final UserBloomFilter userBloomFilter;

    /** 头像存储目录（生产可替换为 OSS/MinIO） */
    @org.springframework.beans.factory.annotation.Value("${user.avatar.storage-path:./uploads/avatars}")
    private String avatarStoragePath;

    /** 头像访问前缀 */
    @org.springframework.beans.factory.annotation.Value("${user.avatar.url-prefix:/uploads/avatars}")
    private String avatarUrlPrefix;

    @Override
    public Result<UserInfoDTO> getUserInfo(Long userId) {
        UserAccount user = getById(userId);
        if (user == null || user.getIsDeleted() == 1) {
            return Result.error(ResultCode.NOT_FOUND, "用户不存在");
        }
        return Result.success(convertToUserInfoDTO(user));
    }

    /**
     * 用户信息缓存：auth 路径(每次鉴权)与 getUserInfo 都走这里，命中率极高。
     * sync=true 由 Spring 对同一个 key 加锁，防止缓存击穿(并发回源)；
     * unless=#result==null 防止缓存穿透(不缓存空值)。
     */
    @Override
    @Cacheable(value = "user", key = "#p0", sync = true)
    public UserAccount getById(Serializable id) {
        // 布隆过滤器预筛: 判定"一定不存在"的 userId 直接返回, 拦截穿透请求打爆 DB(且不进缓存)
        // 注: sync=true 不支持 unless 属性(Spring 限制), 空值不缓存改由布隆在回源前拦截
        Long uid = id instanceof Number ? ((Number) id).longValue() : null;
        if (uid != null && !userBloomFilter.mightContain(uid)) {
            return null;
        }
        return super.getById(id);
    }

    @Override
    public Result<UserInfoDTO> getCurrentUser(String token) {
        if (!org.springframework.util.StringUtils.hasText(token)) {
            return Result.error(ResultCode.UNAUTHORIZED, "请先登录");
        }
        Long userId;
        try {
            userId = jwtUtil.getUserIdFromToken(token);
        } catch (Exception e) {
            log.warn("解析token失败: {}", e.getMessage());
            return Result.error(ResultCode.TOKEN_INVALID, "登录已失效，请重新登录");
        }
        if (userId == null) {
            return Result.error(ResultCode.UNAUTHORIZED, "请重新登录");
        }
        return getUserInfo(userId);
    }

    @Override
    @CacheEvict(value = "user", key = "#p0")
    @Transactional(rollbackFor = Exception.class)
    public Result<UserInfoDTO> updateProfile(Long userId, UpdateProfileDTO updateDTO) {
        UserAccount user = getById(userId);
        if (user == null || user.getIsDeleted() == 1) {
            return Result.error(ResultCode.NOT_FOUND, "用户不存在");
        }

        // 检查用户名是否被占用
        if (StringUtils.hasText(updateDTO.getUsername()) && !updateDTO.getUsername().equals(user.getUsername())) {
            if (userAccountMapper.countByUsername(updateDTO.getUsername()) > 0) {
                return Result.error(ResultCode.CONFLICT, "用户名已被占用");
            }
            user.setUsername(updateDTO.getUsername());
        }

        // 检查邮箱是否被占用
        if (StringUtils.hasText(updateDTO.getEmail()) && !updateDTO.getEmail().equals(user.getEmail())) {
            if (userAccountMapper.countByEmail(updateDTO.getEmail()) > 0) {
                return Result.error(ResultCode.CONFLICT, "邮箱已被占用");
            }
            user.setEmail(updateDTO.getEmail());
        }

        // 检查手机号是否被占用
        if (StringUtils.hasText(updateDTO.getPhone()) && !updateDTO.getPhone().equals(user.getPhone())) {
            if (userAccountMapper.countByPhone(updateDTO.getPhone()) > 0) {
                return Result.error(ResultCode.CONFLICT, "手机号已被占用");
            }
            user.setPhone(updateDTO.getPhone());
        }

        user.setUpdatedAt(LocalDateTime.now());
        updateById(user);

        log.info("用户资料更新成功: userId={}", userId);
        return Result.success(convertToUserInfoDTO(user));
    }

    @Override
    @CacheEvict(value = "user", key = "#p0")
    public Result<String> uploadAvatar(Long userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return Result.error(ResultCode.BAD_REQUEST, "请选择要上传的头像文件");
        }
        if (file.getSize() > 5 * 1024 * 1024) {
            return Result.error(ResultCode.FILE_TOO_LARGE, "头像大小不能超过5MB");
        }
        String original = org.springframework.util.StringUtils.getFilenameExtension(file.getOriginalFilename());
        String ext = org.springframework.util.StringUtils.hasText(original) ? "." + original.toLowerCase() : ".png";
        if (!java.util.Set.of(".png", ".jpg", ".jpeg", ".gif", ".webp").contains(ext)) {
            return Result.error(ResultCode.INVALID_FILE_TYPE, "仅支持 png/jpg/jpeg/gif/webp 格式");
        }

        UserAccount user = getById(userId);
        if (user == null || user.getIsDeleted() == 1) {
            return Result.error(ResultCode.NOT_FOUND, "用户不存在");
        }

        try {
            java.nio.file.Path dir = java.nio.file.Paths.get(avatarStoragePath).toAbsolutePath().normalize();
            java.nio.file.Files.createDirectories(dir);
            String filename = userId + "_" + java.util.UUID.randomUUID().toString().replace("-", "") + ext;
            java.nio.file.Path target = dir.resolve(filename);
            try (java.io.InputStream in = file.getInputStream()) {
                java.nio.file.Files.copy(in, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }

            String url = avatarUrlPrefix + "/" + filename;
            user.setAvatarUrl(url);
            user.setUpdatedAt(java.time.LocalDateTime.now());
            updateById(user);
            log.info("头像上传成功: userId={}, url={}", userId, url);
            return Result.success(url);
        } catch (Exception e) {
            log.error("头像上传失败: userId={}", userId, e);
            return Result.error(ResultCode.FILE_UPLOAD_FAILED, "头像上传失败: " + e.getMessage());
        }
    }

    @Override
    @CacheEvict(value = "user", key = "#p0")
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> changePassword(Long userId, ChangePasswordDTO changePasswordDTO) {
        UserAccount user = getById(userId);
        if (user == null || user.getIsDeleted() == 1) {
            return Result.error(ResultCode.NOT_FOUND, "用户不存在");
        }

        // 验证原密码
        if (!passwordEncoder.matches(changePasswordDTO.getOldPassword(), user.getPasswordHash())) {
            return Result.error(ResultCode.INVALID_CREDENTIALS, "原密码错误");
        }

        // 验证新密码
        if (!changePasswordDTO.getNewPassword().equals(changePasswordDTO.getConfirmPassword())) {
            return Result.error(ResultCode.BAD_REQUEST, "两次输入的密码不一致");
        }

        // 更新密码
        user.setPasswordHash(passwordEncoder.encode(changePasswordDTO.getNewPassword()));
        user.setUpdatedAt(LocalDateTime.now());
        updateById(user);

        log.info("用户密码修改成功: userId={}", userId);
        return Result.success();
    }

    @Override
    @CacheEvict(value = "user", key = "#p0")
    public Result<Void> changePhone(Long userId, String newPhone, String verifyCode) {
        if (!org.springframework.util.StringUtils.hasText(newPhone)) {
            return Result.error(ResultCode.BAD_REQUEST, "手机号不能为空");
        }
        if (!verifyCode(newPhone, verifyCode)) {
            return Result.error(ResultCode.BAD_REQUEST, "验证码错误或已过期");
        }
        UserAccount exist = userAccountMapper.selectByPhone(newPhone);
        if (exist != null && !exist.getId().equals(userId)) {
            return Result.error(ResultCode.CONFLICT, "手机号已被占用");
        }
        UserAccount user = getById(userId);
        if (user == null || user.getIsDeleted() == 1) {
            return Result.error(ResultCode.NOT_FOUND, "用户不存在");
        }
        user.setPhone(newPhone);
        user.setUpdatedAt(java.time.LocalDateTime.now());
        updateById(user);
        log.info("手机号修改成功: userId={}", userId);
        return Result.success();
    }

    @Override
    @CacheEvict(value = "user", key = "#p0")
    public Result<Void> changeEmail(Long userId, String newEmail, String verifyCode) {
        if (!org.springframework.util.StringUtils.hasText(newEmail)) {
            return Result.error(ResultCode.BAD_REQUEST, "邮箱不能为空");
        }
        if (!verifyCode(newEmail, verifyCode)) {
            return Result.error(ResultCode.BAD_REQUEST, "验证码错误或已过期");
        }
        UserAccount exist = userAccountMapper.selectByEmail(newEmail);
        if (exist != null && !exist.getId().equals(userId)) {
            return Result.error(ResultCode.CONFLICT, "邮箱已被占用");
        }
        UserAccount user = getById(userId);
        if (user == null || user.getIsDeleted() == 1) {
            return Result.error(ResultCode.NOT_FOUND, "用户不存在");
        }
        user.setEmail(newEmail);
        user.setUpdatedAt(java.time.LocalDateTime.now());
        updateById(user);
        log.info("邮箱修改成功: userId={}", userId);
        return Result.success();
    }

    @Override
    public Result<Void> bindThirdPartyAccount(Long userId, String platform, String accountId) {
        if (!org.springframework.util.StringUtils.hasText(platform) || !org.springframework.util.StringUtils.hasText(accountId)) {
            return Result.error(ResultCode.BAD_REQUEST, "平台与账号ID不能为空");
        }
        if (userOauthBindingMapper.countBinding(userId, platform) > 0) {
            return Result.error(ResultCode.CONFLICT, "该平台账号已绑定");
        }
        UserOauthBinding binding = new UserOauthBinding();
        binding.setUserId(userId);
        binding.setPlatform(platform);
        binding.setAccountId(accountId);
        binding.setBindAt(java.time.LocalDateTime.now());
        binding.setIsDeleted(0);
        userOauthBindingMapper.insert(binding);
        log.info("第三方账号绑定成功: userId={}, platform={}", userId, platform);
        return Result.success();
    }

    @Override
    @CacheEvict(value = "user", key = "#p0")
    public Result<Void> unbindThirdPartyAccount(Long userId, String platform) {
        if (!org.springframework.util.StringUtils.hasText(platform)) {
            return Result.error(ResultCode.BAD_REQUEST, "平台不能为空");
        }
        // 至少保留一种登录方式：若用户无密码且只剩这一个绑定，则不允许解绑
        UserAccount user = getById(userId);
        if (user == null || user.getIsDeleted() == 1) {
            return Result.error(ResultCode.NOT_FOUND, "用户不存在");
        }
        long bindings = userOauthBindingMapper.selectByUserId(userId).size();
        if (bindings <= 1 && !org.springframework.util.StringUtils.hasText(user.getPasswordHash())) {
            return Result.error(ResultCode.BUSINESS_ERROR, "请至少保留一种登录方式");
        }
        int rows = userOauthBindingMapper.softDelete(userId, platform);
        if (rows == 0) {
            return Result.error(ResultCode.NOT_FOUND, "未找到该平台的绑定记录");
        }
        log.info("第三方账号解绑成功: userId={}, platform={}", userId, platform);
        return Result.success();
    }

    @Override
    public Result<UserSettingsDTO> getUserSettings(Long userId) {
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<UserSettings> wrapper =
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        wrapper.eq(UserSettings::getUserId, userId);
        UserSettings settings = userSettingsMapper.selectOne(wrapper);
        if (settings == null) {
            // 首次访问返回默认设置（不落库，等用户真正修改时再写）
            UserSettingsDTO def = new UserSettingsDTO();
            def.setTheme("system");
            def.setLanguage("zh-CN");
            def.setTimezone("Asia/Shanghai");
            return Result.success(def);
        }
        UserSettingsDTO dto = new UserSettingsDTO();
        dto.setTheme(settings.getTheme());
        dto.setLanguage(settings.getLanguage());
        dto.setTimezone(settings.getTimezone());
        dto.setDefaultModel(settings.getDefaultModel());
        dto.setNotification(settings.getNotification());
        dto.setPrivacy(settings.getPrivacy());
        dto.setExtraConfig(settings.getExtraConfig());
        return Result.success(dto);
    }

    @Override
    public Result<Void> updateUserSettings(Long userId, UserSettingsDTO settingsDTO) {
        if (settingsDTO == null) {
            return Result.error(ResultCode.BAD_REQUEST, "设置内容不能为空");
        }
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<UserSettings> wrapper =
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        wrapper.eq(UserSettings::getUserId, userId);
        UserSettings settings = userSettingsMapper.selectOne(wrapper);

        boolean isNew = false;
        if (settings == null) {
            settings = new UserSettings();
            settings.setUserId(userId);
            isNew = true;
        }
        settings.setTheme(settingsDTO.getTheme());
        settings.setLanguage(settingsDTO.getLanguage());
        settings.setTimezone(settingsDTO.getTimezone());
        settings.setDefaultModel(settingsDTO.getDefaultModel());
        settings.setNotification(settingsDTO.getNotification());
        settings.setPrivacy(settingsDTO.getPrivacy());
        settings.setExtraConfig(settingsDTO.getExtraConfig());

        if (isNew) {
            userSettingsMapper.insert(settings);
        } else {
            userSettingsMapper.updateById(settings);
        }
        log.info("用户设置更新成功: userId={}", userId);
        return Result.success();
    }

    @Override
    public Result<List<UserInfoDTO>> getUserList(Integer page, Integer size, String keyword, Integer status) {
        LambdaQueryWrapper<UserAccount> wrapper = Wrappers.lambdaQuery();
        
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(UserAccount::getUsername, keyword)
                    .or().like(UserAccount::getEmail, keyword)
                    .or().like(UserAccount::getPhone, keyword));
        }
        
        if (status != null) {
            wrapper.eq(UserAccount::getStatus, status);
        }
        
        wrapper.eq(UserAccount::getIsDeleted, 0)
               .orderByDesc(UserAccount::getCreatedAt);

        Page<UserAccount> pageResult = page(new Page<>(page, size), wrapper);
        
        List<UserInfoDTO> list = pageResult.getRecords().stream()
                .map(this::convertToUserInfoDTO)
                .collect(Collectors.toList());

        return Result.success(list);
    }

    @Override
    @CacheEvict(value = "user", key = "#p0")
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> updateUserStatus(Long userId, Integer status) {
        UserAccount user = getById(userId);
        if (user == null || user.getIsDeleted() == 1) {
            return Result.error(ResultCode.NOT_FOUND, "用户不存在");
        }

        user.setStatus(status);
        user.setUpdatedAt(LocalDateTime.now());
        updateById(user);

        log.info("用户状态更新成功: userId={}, status={}", userId, status);
        return Result.success();
    }

    @Override
    @CacheEvict(value = "user", key = "#p0")
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> deleteUser(Long userId) {
        UserAccount user = getById(userId);
        if (user == null || user.getIsDeleted() == 1) {
            return Result.error(ResultCode.NOT_FOUND, "用户不存在");
        }

        user.setIsDeleted(1);
        user.setUpdatedAt(LocalDateTime.now());
        updateById(user);

        log.info("用户删除成功: userId={}", userId);
        return Result.success();
    }

    /**
     * 转换为UserInfoDTO
     */
    private UserInfoDTO convertToUserInfoDTO(UserAccount user) {
        UserInfoDTO dto = new UserInfoDTO();
        dto.setUserId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setPhone(user.getPhone());
        dto.setAvatarUrl(user.getAvatarUrl());
        dto.setUserType(user.getUserType());
        dto.setUserTypeName(getUserTypeName(user.getUserType()));
        dto.setStatus(user.getStatus());
        dto.setStatusName(getStatusName(user.getStatus()));
        dto.setLastLoginAt(user.getLastLoginAt());
        dto.setLastLoginIp(user.getLastLoginIp());
        dto.setCreatedAt(user.getCreatedAt());

        // 获取角色列表
        List<String> roleCodes = roleService.getUserRoleCodes(user.getId());
        dto.setRoles(roleCodes);

        // 获取权限列表
        List<String> permissions = permissionService.getUserPermissionCodes(user.getId())
                .stream().collect(java.util.stream.Collectors.toList());
        dto.setPermissions(permissions);

        // 获取VIP信息
        VipInfoDTO vipInfo = vipService.getUserVipInfo(user.getId()).getData();
        if (vipInfo != null && vipInfo.getIsVip()) {
            dto.setVipLevel(vipInfo.getVipLevel());
            dto.setVipName(vipInfo.getVipName());
            dto.setVipExpireAt(vipInfo.getExpireAt());
        }

        return dto;
    }

    /**
     * 获取用户类型名称
     */
    private String getUserTypeName(Integer userType) {
        if (userType == null) return "未知";
        switch (userType) {
            case 1: return "普通用户";
            case 2: return "VIP用户";
            case 9: return "管理员";
            default: return "未知";
        }
    }

    /**
     * 获取状态名称
     */
    private String getStatusName(Integer status) {
        if (status == null) return "未知";
        switch (status) {
            case 0: return "禁用";
            case 1: return "正常";
            case 2: return "待验证";
            default: return "未知";
        }
    }

    /**
     * 校验目标(手机号/邮箱)对应的验证码
     */
    private boolean verifyCode(String target, String code) {
        String cached = (String) redisUtils.get(com.aisaas.common.util.RedisKeys.USER_VERIFY_CODE + target);
        if (!org.springframework.util.StringUtils.hasText(cached) || !org.springframework.util.StringUtils.hasText(code)) {
            return false;
        }
        if (!cached.equalsIgnoreCase(code)) {
            return false;
        }
        redisUtils.delete(com.aisaas.common.util.RedisKeys.USER_VERIFY_CODE + target);
        return true;
    }
}
