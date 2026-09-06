package com.aisaas.billing.service.impl;

import com.aisaas.billing.dto.*;
import com.aisaas.billing.entity.PriceConfig;
import com.aisaas.billing.mapper.PriceConfigMapper;
import com.aisaas.billing.service.PriceConfigService;
import com.aisaas.common.result.Result;
import com.aisaas.common.result.ResultCode;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
public class PriceConfigServiceImpl implements PriceConfigService {

    @Autowired
    private PriceConfigMapper priceConfigMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> createPriceConfig(PriceConfigCreateDTO dto) {
        try {
            // 检查是否已存在相同配置
            LambdaQueryWrapper<PriceConfig> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(PriceConfig::getProvider, dto.getProvider())
                    .eq(PriceConfig::getModelId, dto.getModelId())
                    .eq(PriceConfig::getOperationType, dto.getOperationType())
                    .eq(PriceConfig::getStatus, 1)
                    .eq(PriceConfig::getIsDeleted, 0);
            
            long count = priceConfigMapper.selectCount(wrapper);
            if (count > 0) {
                return Result.error(ResultCode.BUSINESS_ERROR, "已存在相同的价格配置");
            }

            PriceConfig config = new PriceConfig();
            BeanUtils.copyProperties(dto, config);
            config.setConfigId(generateConfigId());
            config.setStatus(1);
            config.setCreatedAt(LocalDateTime.now());
            config.setUpdatedAt(LocalDateTime.now());

            priceConfigMapper.insert(config);

            log.info("价格配置创建成功: configId={}", config.getConfigId());
            return Result.success();
        } catch (Exception e) {
            log.error("创建价格配置失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR, "创建价格配置失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> updatePriceConfig(PriceConfigUpdateDTO dto) {
        try {
            PriceConfig config = priceConfigMapper.selectById(dto.getId());
            if (config == null || config.getIsDeleted() == 1) {
                return Result.error(ResultCode.NOT_FOUND, "价格配置不存在");
            }

            BeanUtils.copyProperties(dto, config);
            config.setUpdatedAt(LocalDateTime.now());

            priceConfigMapper.updateById(config);

            log.info("价格配置更新成功: configId={}", config.getConfigId());
            return Result.success();
        } catch (Exception e) {
            log.error("更新价格配置失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR, "更新价格配置失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> deletePriceConfig(String configId) {
        try {
            LambdaQueryWrapper<PriceConfig> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(PriceConfig::getConfigId, configId);
            
            PriceConfig config = priceConfigMapper.selectOne(wrapper);
            if (config == null || config.getIsDeleted() == 1) {
                return Result.error(ResultCode.NOT_FOUND, "价格配置不存在");
            }

            config.setIsDeleted(1);
            config.setUpdatedAt(LocalDateTime.now());
            priceConfigMapper.updateById(config);

            log.info("价格配置删除成功: configId={}", configId);
            return Result.success();
        } catch (Exception e) {
            log.error("删除价格配置失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR, "删除价格配置失败: " + e.getMessage());
        }
    }

    @Override
    public Result<PriceConfigVO> getPriceConfig(String configId) {
        try {
            LambdaQueryWrapper<PriceConfig> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(PriceConfig::getConfigId, configId);
            
            PriceConfig config = priceConfigMapper.selectOne(wrapper);
            if (config == null || config.getIsDeleted() == 1) {
                return Result.error(ResultCode.NOT_FOUND, "价格配置不存在");
            }

            PriceConfigVO vo = convertToVO(config);
            return Result.success(vo);
        } catch (Exception e) {
            log.error("获取价格配置失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR, "获取价格配置失败: " + e.getMessage());
        }
    }

    @Override
    public Result<IPage<PriceConfigVO>> queryPriceConfig(PriceConfigQueryDTO dto) {
        try {
            Page<PriceConfig> page = new Page<>(dto.getPageNum(), dto.getPageSize());
            
            LambdaQueryWrapper<PriceConfig> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(PriceConfig::getIsDeleted, 0);
            
            if (dto.getProvider() != null && !dto.getProvider().isEmpty()) {
                wrapper.eq(PriceConfig::getProvider, dto.getProvider());
            }
            if (dto.getModelId() != null && !dto.getModelId().isEmpty()) {
                wrapper.eq(PriceConfig::getModelId, dto.getModelId());
            }
            if (dto.getOperationType() != null && !dto.getOperationType().isEmpty()) {
                wrapper.eq(PriceConfig::getOperationType, dto.getOperationType());
            }
            if (dto.getStatus() != null) {
                wrapper.eq(PriceConfig::getStatus, dto.getStatus());
            }
            
            wrapper.orderByDesc(PriceConfig::getCreatedAt);
            
            IPage<PriceConfig> configPage = priceConfigMapper.selectPage(page, wrapper);
            
            List<PriceConfigVO> voList = configPage.getRecords().stream()
                    .map(this::convertToVO)
                    .collect(Collectors.toList());
            
            Page<PriceConfigVO> voPage = new Page<>(configPage.getCurrent(), configPage.getSize(), configPage.getTotal());
            voPage.setRecords(voList);
            
            return Result.success(voPage);
        } catch (Exception e) {
            log.error("查询价格配置失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR, "查询价格配置失败: " + e.getMessage());
        }
    }

    @Override
    public Result<List<PriceConfigVO>> getAllValidConfigs() {
        try {
            List<PriceConfig> configs = priceConfigMapper.selectAllValidConfigs(LocalDateTime.now());
            List<PriceConfigVO> voList = configs.stream()
                    .map(this::convertToVO)
                    .collect(Collectors.toList());
            return Result.success(voList);
        } catch (Exception e) {
            log.error("获取有效价格配置失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR, "获取有效价格配置失败: " + e.getMessage());
        }
    }

    @Override
    public Result<PriceConfigVO> getPriceByCondition(String provider, String modelId, String operationType) {
        try {
            PriceConfig config = priceConfigMapper.selectValidConfig(provider, modelId, operationType, LocalDateTime.now());
            if (config == null) {
                return Result.error(ResultCode.NOT_FOUND, "未找到匹配的价格配置");
            }
            return Result.success(convertToVO(config));
        } catch (Exception e) {
            log.error("查询价格配置失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR, "查询价格配置失败: " + e.getMessage());
        }
    }

    // ==================== 私有方法 ====================

    private String generateConfigId() {
        return "PRC" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) +
                UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }

    private PriceConfigVO convertToVO(PriceConfig config) {
        PriceConfigVO vo = new PriceConfigVO();
        BeanUtils.copyProperties(config, vo);
        return vo;
    }
}
