package com.aisaas.user.service.impl;

import com.aisaas.common.constant.ResultCode;
import com.aisaas.common.result.Result;
import com.aisaas.user.dto.UserSettingsDTO;
import com.aisaas.user.entity.UserSettings;
import com.aisaas.user.mapper.UserSettingsMapper;
import com.aisaas.user.service.UserSettingsService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 用户设置服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserSettingsServiceImpl extends ServiceImpl<UserSettingsMapper, UserSettings> implements UserSettingsService {

    private final UserSettingsMapper userSettingsMapper;
    private final ObjectMapper objectMapper;

    @Override
    public Result<UserSettingsDTO> getUserSettings(Long userId) {
        UserSettings settings = userSettingsMapper.selectByUserId(userId);
        if (settings == null) {
            // 创建默认设置
            settings = createDefaultSettings(userId);
        }
        return Result.success(convertToDTO(settings));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> updateUserSettings(Long userId, UserSettingsDTO settingsDTO) {
        UserSettings settings = userSettingsMapper.selectByUserId(userId);
        if (settings == null) {
            settings = createDefaultSettings(userId);
        }

        // 更新设置
        if (settingsDTO.getTheme() != null) {
            settings.setTheme(settingsDTO.getTheme());
        }
        if (settingsDTO.getLanguage() != null) {
            settings.setLanguage(settingsDTO.getLanguage());
        }
        if (settingsDTO.getTimezone() != null) {
            settings.setTimezone(settingsDTO.getTimezone());
        }
        if (settingsDTO.getDefaultModel() != null) {
            settings.setDefaultModel(settingsDTO.getDefaultModel());
        }
        if (settingsDTO.getNotification() != null) {
            settings.setNotification(settingsDTO.getNotification());
        }
        if (settingsDTO.getPrivacy() != null) {
            settings.setPrivacy(settingsDTO.getPrivacy());
        }
        if (settingsDTO.getExtraConfig() != null) {
            settings.setExtraConfig(settingsDTO.getExtraConfig());
        }

        settings.setUpdatedAt(LocalDateTime.now());
        saveOrUpdate(settings);

        log.info("更新用户设置成功: userId={}", userId);
        return Result.success();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> resetUserSettings(Long userId) {
        UserSettings settings = userSettingsMapper.selectByUserId(userId);
        if (settings != null) {
            settings.setTheme("light");
            settings.setLanguage("zh-CN");
            settings.setTimezone("Asia/Shanghai");
            settings.setDefaultModel(null);
            settings.setNotification(null);
            settings.setPrivacy(null);
            settings.setExtraConfig(null);
            settings.setUpdatedAt(LocalDateTime.now());
            updateById(settings);
        }
        log.info("重置用户设置成功: userId={}", userId);
        return Result.success();
    }

    /**
     * 创建默认设置
     */
    private UserSettings createDefaultSettings(Long userId) {
        UserSettings settings = new UserSettings();
        settings.setUserId(userId);
        settings.setTheme("light");
        settings.setLanguage("zh-CN");
        settings.setTimezone("Asia/Shanghai");
        settings.setIsDeleted(0);
        settings.setCreatedAt(LocalDateTime.now());
        settings.setUpdatedAt(LocalDateTime.now());
        save(settings);
        return settings;
    }

    /**
     * 转换为DTO
     */
    private UserSettingsDTO convertToDTO(UserSettings settings) {
        UserSettingsDTO dto = new UserSettingsDTO();
        dto.setTheme(settings.getTheme());
        dto.setLanguage(settings.getLanguage());
        dto.setTimezone(settings.getTimezone());
        dto.setDefaultModel(settings.getDefaultModel());
        dto.setNotification(settings.getNotification());
        dto.setPrivacy(settings.getPrivacy());
        dto.setExtraConfig(settings.getExtraConfig());
        return dto;
    }
}
