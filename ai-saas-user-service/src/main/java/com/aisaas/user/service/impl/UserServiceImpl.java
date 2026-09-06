package com.aisaas.user.service.impl;

import com.aisaas.common.constant.ResultCode;
import com.aisaas.common.result.Result;
import com.aisaas.user.dto.*;
import com.aisaas.user.entity.UserAccount;
import com.aisaas.user.entity.UserRole;
import com.aisaas.user.mapper.UserAccountMapper;
import com.aisaas.user.service.*;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

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

    @Override
    public Result<UserInfoDTO> getUserInfo(Long userId) {
        UserAccount user = getById(userId);
        if (user == null || user.getIsDeleted() == 1) {
            return Result.error(ResultCode.NOT_FOUND, "用户不存在");
        }
        return Result.success(convertToUserInfoDTO(user));
    }

    @Override
    public Result<UserInfoDTO> getCurrentUser(String token) {
        // TODO: 从token解析用户ID
        return Result.error(ResultCode.UNAUTHORIZED, "请重新登录");
    }

    @Override
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
    public Result<String> uploadAvatar(Long userId, MultipartFile file) {
        // TODO: 实现头像上传逻辑
        // 1. 上传到MinIO/OSS
        // 2. 更新用户头像URL
        return Result.error(ResultCode.FEATURE_NOT_IMPLEMENTED, "头像上传功能待实现");
    }

    @Override
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
    public Result<Void> changePhone(Long userId, String newPhone, String verifyCode) {
        // TODO: 实现修改手机号逻辑
        return Result.error(ResultCode.FEATURE_NOT_IMPLEMENTED, "修改手机号功能待实现");
    }

    @Override
    public Result<Void> changeEmail(Long userId, String newEmail, String verifyCode) {
        // TODO: 实现修改邮箱逻辑
        return Result.error(ResultCode.FEATURE_NOT_IMPLEMENTED, "修改邮箱功能待实现");
    }

    @Override
    public Result<Void> bindThirdPartyAccount(Long userId, String platform, String accountId) {
        // TODO: 实现绑定第三方账号逻辑
        return Result.error(ResultCode.FEATURE_NOT_IMPLEMENTED, "绑定第三方账号功能待实现");
    }

    @Override
    public Result<Void> unbindThirdPartyAccount(Long userId, String platform) {
        // TODO: 实现解绑第三方账号逻辑
        return Result.error(ResultCode.FEATURE_NOT_IMPLEMENTED, "解绑第三方账号功能待实现");
    }

    @Override
    public Result<UserSettingsDTO> getUserSettings(Long userId) {
        // TODO: 实现获取用户设置逻辑
        return Result.success(new UserSettingsDTO());
    }

    @Override
    public Result<Void> updateUserSettings(Long userId, UserSettingsDTO settingsDTO) {
        // TODO: 实现更新用户设置逻辑
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
}
