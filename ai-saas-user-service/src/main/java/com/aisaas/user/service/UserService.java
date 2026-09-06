package com.aisaas.user.service;

import com.aisaas.user.dto.*;
import com.aisaas.user.entity.UserAccount;
import com.aisaas.common.result.Result;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 用户服务接口
 */
public interface UserService extends IService<UserAccount> {

    /**
     * 根据ID获取用户信息
     */
    Result<UserInfoDTO> getUserInfo(Long userId);

    /**
     * 根据Token获取当前用户信息
     */
    Result<UserInfoDTO> getCurrentUser(String token);

    /**
     * 更新用户资料
     */
    Result<UserInfoDTO> updateProfile(Long userId, UpdateProfileDTO updateDTO);

    /**
     * 上传头像
     */
    Result<String> uploadAvatar(Long userId, MultipartFile file);

    /**
     * 修改密码
     */
    Result<Void> changePassword(Long userId, ChangePasswordDTO changePasswordDTO);

    /**
     * 修改手机号
     */
    Result<Void> changePhone(Long userId, String newPhone, String verifyCode);

    /**
     * 修改邮箱
     */
    Result<Void> changeEmail(Long userId, String newEmail, String verifyCode);

    /**
     * 绑定/解绑第三方账号
     */
    Result<Void> bindThirdPartyAccount(Long userId, String platform, String accountId);
    Result<Void> unbindThirdPartyAccount(Long userId, String platform);

    /**
     * 获取用户设置
     */
    Result<UserSettingsDTO> getUserSettings(Long userId);

    /**
     * 更新用户设置
     */
    Result<Void> updateUserSettings(Long userId, UserSettingsDTO settingsDTO);

    /**
     * 获取用户列表(管理员)
     */
    Result<List<UserInfoDTO>> getUserList(Integer page, Integer size, String keyword, Integer status);

    /**
     * 禁用/启用用户(管理员)
     */
    Result<Void> updateUserStatus(Long userId, Integer status);

    /**
     * 删除用户(逻辑删除)
     */
    Result<Void> deleteUser(Long userId);
}
