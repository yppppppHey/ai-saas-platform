package com.aisaas.user.controller;

import com.aisaas.common.result.Result;
import com.aisaas.user.dto.*;
import com.aisaas.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 用户控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 获取当前用户信息
     */
    @GetMapping("/me")
    public Result<UserInfoDTO> getCurrentUser(@RequestAttribute("userId") Long userId) {
        return userService.getUserInfo(userId);
    }

    /**
     * 获取用户信息
     */
    @GetMapping("/{userId}")
    public Result<UserInfoDTO> getUserInfo(@PathVariable Long userId) {
        return userService.getUserInfo(userId);
    }

    /**
     * 更新个人资料
     */
    @PutMapping("/profile")
    public Result<UserInfoDTO> updateProfile(@RequestAttribute("userId") Long userId,
                                              @Valid @RequestBody UpdateProfileDTO updateDTO) {
        return userService.updateProfile(userId, updateDTO);
    }

    /**
     * 上传头像
     */
    @PostMapping("/avatar")
    public Result<String> uploadAvatar(@RequestAttribute("userId") Long userId,
                                       @RequestParam("file") MultipartFile file) {
        return userService.uploadAvatar(userId, file);
    }

    /**
     * 修改密码
     */
    @PutMapping("/password")
    public Result<Void> changePassword(@RequestAttribute("userId") Long userId,
                                       @Valid @RequestBody ChangePasswordDTO changePasswordDTO) {
        return userService.changePassword(userId, changePasswordDTO);
    }

    /**
     * 修改手机号
     */
    @PutMapping("/phone")
    public Result<Void> changePhone(@RequestAttribute("userId") Long userId,
                                    @RequestParam String newPhone,
                                    @RequestParam String verifyCode) {
        return userService.changePhone(userId, newPhone, verifyCode);
    }

    /**
     * 修改邮箱
     */
    @PutMapping("/email")
    public Result<Void> changeEmail(@RequestAttribute("userId") Long userId,
                                    @RequestParam String newEmail,
                                    @RequestParam String verifyCode) {
        return userService.changeEmail(userId, newEmail, verifyCode);
    }

    /**
     * 获取用户设置
     */
    @GetMapping("/settings")
    public Result<UserSettingsDTO> getUserSettings(@RequestAttribute("userId") Long userId) {
        return userService.getUserSettings(userId);
    }

    /**
     * 更新用户设置
     */
    @PutMapping("/settings")
    public Result<Void> updateUserSettings(@RequestAttribute("userId") Long userId,
                                           @RequestBody UserSettingsDTO settingsDTO) {
        return userService.updateUserSettings(userId, settingsDTO);
    }

    // ==================== 管理员接口 ====================

    /**
     * 获取用户列表(管理员)
     */
    @GetMapping("/admin/list")
    public Result<List<UserInfoDTO>> getUserList(@RequestParam(defaultValue = "1") Integer page,
                                                  @RequestParam(defaultValue = "10") Integer size,
                                                  @RequestParam(required = false) String keyword,
                                                  @RequestParam(required = false) Integer status) {
        return userService.getUserList(page, size, keyword, status);
    }

    /**
     * 更新用户状态(管理员)
     */
    @PutMapping("/admin/{userId}/status")
    public Result<Void> updateUserStatus(@PathVariable Long userId,
                                         @RequestParam Integer status) {
        return userService.updateUserStatus(userId, status);
    }

    /**
     * 删除用户(管理员)
     */
    @DeleteMapping("/admin/{userId}")
    public Result<Void> deleteUser(@PathVariable Long userId) {
        return userService.deleteUser(userId);
    }
}
