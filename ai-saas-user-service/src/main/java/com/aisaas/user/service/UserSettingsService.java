package com.aisaas.user.service;

import com.aisaas.common.result.Result;
import com.aisaas.user.dto.UserSettingsDTO;
import com.aisaas.user.entity.UserSettings;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 用户设置服务接口
 */
public interface UserSettingsService extends IService<UserSettings> {

    /**
     * 获取用户设置
     */
    Result<UserSettingsDTO> getUserSettings(Long userId);

    /**
     * 更新用户设置
     */
    Result<Void> updateUserSettings(Long userId, UserSettingsDTO settingsDTO);

    /**
     * 重置用户设置
     */
    Result<Void> resetUserSettings(Long userId);
}
