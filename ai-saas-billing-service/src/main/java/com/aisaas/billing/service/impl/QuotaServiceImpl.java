package com.aisaas.billing.service.impl;

import com.aisaas.billing.dto.*;
import com.aisaas.billing.entity.QuotaConfig;
import com.aisaas.billing.entity.QuotaRecord;
import com.aisaas.billing.mapper.QuotaConfigMapper;
import com.aisaas.billing.mapper.QuotaRecordMapper;
import com.aisaas.billing.service.QuotaService;
import com.aisaas.common.result.Result;
import com.aisaas.common.constant.ResultCode;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
public class QuotaServiceImpl implements QuotaService {

    @Autowired
    private QuotaConfigMapper quotaConfigMapper;

    @Autowired
    private org.springframework.data.redis.core.StringRedisTemplate stringRedisTemplate;

    @Autowired
    private QuotaRecordMapper quotaRecordMapper;

    // ==================== 配额配置管理 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> createQuotaConfig(QuotaConfigCreateDTO dto) {
        try {
            QuotaConfig config = new QuotaConfig();
            BeanUtils.copyProperties(dto, config);
            config.setConfigId(generateConfigId());
            config.setStatus(1);
            config.setCreatedAt(LocalDateTime.now());
            config.setUpdatedAt(LocalDateTime.now());

            quotaConfigMapper.insert(config);

            log.info("配额配置创建成功: configId={}", config.getConfigId());
            return Result.success();
        } catch (Exception e) {
            log.error("创建配额配置失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR, "创建配额配置失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> updateQuotaConfig(QuotaConfigUpdateDTO dto) {
        try {
            QuotaConfig config = quotaConfigMapper.selectById(dto.getId());
            if (config == null || config.getIsDeleted() == 1) {
                return Result.error(ResultCode.NOT_FOUND, "配额配置不存在");
            }

            BeanUtils.copyProperties(dto, config);
            config.setUpdatedAt(LocalDateTime.now());

            quotaConfigMapper.updateById(config);

            log.info("配额配置更新成功: configId={}", config.getConfigId());
            return Result.success();
        } catch (Exception e) {
            log.error("更新配额配置失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR, "更新配额配置失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> deleteQuotaConfig(String configId) {
        try {
            LambdaQueryWrapper<QuotaConfig> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(QuotaConfig::getConfigId, configId);

            QuotaConfig config = quotaConfigMapper.selectOne(wrapper);
            if (config == null || config.getIsDeleted() == 1) {
                return Result.error(ResultCode.NOT_FOUND, "配额配置不存在");
            }

            config.setIsDeleted(1);
            config.setUpdatedAt(LocalDateTime.now());
            quotaConfigMapper.updateById(config);

            log.info("配额配置删除成功: configId={}", configId);
            return Result.success();
        } catch (Exception e) {
            log.error("删除配额配置失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR, "删除配额配置失败: " + e.getMessage());
        }
    }

    @Override
    public Result<QuotaConfigVO> getQuotaConfig(String configId) {
        try {
            LambdaQueryWrapper<QuotaConfig> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(QuotaConfig::getConfigId, configId);

            QuotaConfig config = quotaConfigMapper.selectOne(wrapper);
            if (config == null || config.getIsDeleted() == 1) {
                return Result.error(ResultCode.NOT_FOUND, "配额配置不存在");
            }

            QuotaConfigVO vo = convertConfigToVO(config);
            return Result.success(vo);
        } catch (Exception e) {
            log.error("获取配额配置失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR, "获取配额配置失败: " + e.getMessage());
        }
    }

    @Override
    public Result<IPage<QuotaConfigVO>> queryQuotaConfig(QuotaConfigQueryDTO dto) {
        try {
            Page<QuotaConfig> page = new Page<>(dto.getPageNum(), dto.getPageSize());

            LambdaQueryWrapper<QuotaConfig> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(QuotaConfig::getIsDeleted, 0);

            if (dto.getQuotaType() != null) {
                wrapper.eq(QuotaConfig::getQuotaType, dto.getQuotaType());
            }
            if (dto.getUserType() != null) {
                wrapper.eq(QuotaConfig::getUserType, dto.getUserType());
            }
            if (dto.getStatus() != null) {
                wrapper.eq(QuotaConfig::getStatus, dto.getStatus());
            }

            wrapper.orderByDesc(QuotaConfig::getCreatedAt);

            IPage<QuotaConfig> configPage = quotaConfigMapper.selectPage(page, wrapper);

            List<QuotaConfigVO> voList = configPage.getRecords().stream()
                    .map(this::convertConfigToVO)
                    .collect(Collectors.toList());

            Page<QuotaConfigVO> voPage = new Page<>(configPage.getCurrent(), configPage.getSize(), configPage.getTotal());
            voPage.setRecords(voList);

            return Result.success(voPage);
        } catch (Exception e) {
            log.error("查询配额配置失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR, "查询配额配置失败: " + e.getMessage());
        }
    }

    @Override
    public Result<List<QuotaConfigVO>> getAllValidConfigs() {
        try {
            List<QuotaConfig> configs = quotaConfigMapper.selectAllValidConfigs(LocalDateTime.now());
            List<QuotaConfigVO> voList = configs.stream()
                    .map(this::convertConfigToVO)
                    .collect(Collectors.toList());
            return Result.success(voList);
        } catch (Exception e) {
            log.error("获取有效配额配置失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR, "获取有效配额配置失败: " + e.getMessage());
        }
    }

    // ==================== 配额记录管理 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> initUserQuota(Long userId, Integer userType) {
        try {
            // 获取用户的配额配置
            List<QuotaConfig> configs = quotaConfigMapper.selectAllValidConfigs(LocalDateTime.now())
                    .stream()
                    .filter(c -> c.getUserType().equals(userType))
                    .collect(Collectors.toList());

            for (QuotaConfig config : configs) {
                // 检查是否已存在配额记录
                QuotaRecord existing = quotaRecordMapper.selectByUserIdAndType(userId, config.getQuotaType());
                if (existing != null) {
                    continue;
                }

                QuotaRecord record = new QuotaRecord();
                record.setRecordId(generateRecordId());
                record.setUserId(userId);
                record.setQuotaType(config.getQuotaType());
                record.setDailyLimit(config.getDailyLimit());
                record.setMonthlyLimit(config.getMonthlyLimit());
                record.setTotalLimit(config.getTotalLimit());
                record.setDailyUsed(0L);
                record.setMonthlyUsed(0L);
                record.setTotalUsed(0L);
                record.setLastResetDate(LocalDate.now());
                record.setResetDay(config.getResetDay());
                record.setEffectiveAt(config.getEffectiveAt());
                record.setExpireAt(config.getExpireAt());
                record.setStatus(1);
                record.setCreatedAt(LocalDateTime.now());
                record.setUpdatedAt(LocalDateTime.now());
                record.setIsDeleted(0);

                quotaRecordMapper.insert(record);
            }

            log.info("用户配额初始化成功: userId={}", userId);
            return Result.success();
        } catch (Exception e) {
            log.error("初始化用户配额失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR, "初始化用户配额失败: " + e.getMessage());
        }
    }

    @Override
    public Result<List<QuotaRecordVO>> getUserQuotaRecords(Long userId) {
        try {
            List<QuotaRecord> records = quotaRecordMapper.selectByUserId(userId);
            List<QuotaRecordVO> voList = records.stream()
                    .map(this::convertRecordToVO)
                    .collect(Collectors.toList());
            return Result.success(voList);
        } catch (Exception e) {
            log.error("获取用户配额记录失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR, "获取用户配额记录失败: " + e.getMessage());
        }
    }

    @Override
    public Result<QuotaRecordVO> getUserQuotaByType(Long userId, Integer quotaType) {
        try {
            QuotaRecord record = quotaRecordMapper.selectByUserIdAndType(userId, quotaType);
            if (record == null) {
                return Result.error(ResultCode.NOT_FOUND, "配额记录不存在");
            }
            return Result.success(convertRecordToVO(record));
        } catch (Exception e) {
            log.error("获取用户配额失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR, "获取用户配额失败: " + e.getMessage());
        }
    }

    // ==================== 配额检查与扣减 ====================

    @Override
    public Result<QuotaCheckResultVO> checkQuota(Long userId, Integer quotaType, Long requiredAmount) {
        try {
            QuotaRecord record = quotaRecordMapper.selectByUserIdAndType(userId, quotaType);
            if (record == null) {
                return Result.error(ResultCode.NOT_FOUND, "配额记录不存在");
            }

            QuotaCheckResultVO vo = new QuotaCheckResultVO();
            vo.setUserId(userId);
            vo.setQuotaType(quotaType);
            vo.setRequiredAmount(requiredAmount);
            vo.setDailyLimit(record.getDailyLimit());
            vo.setDailyUsed(record.getDailyUsed());
            vo.setMonthlyLimit(record.getMonthlyLimit());
            vo.setMonthlyUsed(record.getMonthlyUsed());
            vo.setTotalLimit(record.getTotalLimit());
            vo.setTotalUsed(record.getTotalUsed());

            // 检查配额
            boolean available = true;
            String reason = "";

            if (record.getDailyLimit() > 0 && record.getDailyUsed() + requiredAmount > record.getDailyLimit()) {
                available = false;
                reason = "每日配额不足";
            } else if (record.getMonthlyLimit() > 0 && record.getMonthlyUsed() + requiredAmount > record.getMonthlyLimit()) {
                available = false;
                reason = "每月配额不足";
            } else if (record.getTotalLimit() > 0 && record.getTotalUsed() + requiredAmount > record.getTotalLimit()) {
                available = false;
                reason = "总计配额不足";
            }

            vo.setAvailable(available);
            vo.setReason(reason);

            return Result.success(vo);
        } catch (Exception e) {
            log.error("检查配额失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR, "检查配额失败: " + e.getMessage());
        }
    }

    @Override
    public Result<BatchQuotaCheckResultVO> batchCheckQuota(Long userId, List<QuotaCheckItemDTO> items) {
        try {
            BatchQuotaCheckResultVO vo = new BatchQuotaCheckResultVO();
            List<QuotaCheckResultVO> results = new ArrayList<>();
            boolean allAvailable = true;

            for (QuotaCheckItemDTO item : items) {
                Result<QuotaCheckResultVO> result = checkQuota(userId, item.getQuotaType(), item.getRequiredAmount());
                if (result.isSuccess()) {
                    results.add(result.getData());
                    if (!result.getData().getAvailable()) {
                        allAvailable = false;
                    }
                } else {
                    allAvailable = false;
                }
            }

            vo.setResults(results);
            vo.setAllAvailable(allAvailable);

            return Result.success(vo);
        } catch (Exception e) {
            log.error("批量检查配额失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR, "批量检查配额失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<QuotaDeductResultVO> deductQuota(Long userId, Integer quotaType, Long amount, String bizType, String bizId) {
        try {
            // 1. 业务幂等：同一业务动作(bizType+bizId)只允许扣一次，MQ 重投/用户重试不会重复扣
            if (StringUtils.hasText(bizId)) {
                String dedupKey = "billing:quota:dedup:" + bizType + ":" + bizId;
                Boolean first = stringRedisTemplate.opsForValue()
                        .setIfAbsent(dedupKey, "1", java.time.Duration.ofHours(24));
                if (!Boolean.TRUE.equals(first)) {
                    log.info("配额扣减重复请求(幂等命中): bizType={}, bizId={}", bizType, bizId);
                    return Result.error(ResultCode.BUSINESS_ERROR, "重复的扣减请求");
                }
            }

            // 2. 友好提示用的预检查（真正的并发安全由下面 CAS 保证）
            Result<QuotaCheckResultVO> checkResult = checkQuota(userId, quotaType, amount);
            if (!checkResult.isSuccess()) {
                return Result.error(checkResult.getCode(), checkResult.getMessage());
            }
            if (!checkResult.getData().getAvailable()) {
                return Result.error(ResultCode.BUSINESS_ERROR, checkResult.getData().getReason());
            }

            // 3. CAS 扣减：条件更新保证"检查+扣减"原子性，高并发不会扣超
            int rows = quotaRecordMapper.deductUsageAtomic(userId, quotaType, amount);
            if (rows == 0) {
                // 区分"记录不存在"与"并发下配额已被扣完"
                boolean exists = quotaRecordMapper.countByUserAndType(userId, quotaType) > 0;
                return Result.error(ResultCode.BUSINESS_ERROR,
                        exists ? "配额不足(并发校验)" : "配额记录不存在");
            }

            QuotaDeductResultVO vo = new QuotaDeductResultVO();
            vo.setUserId(userId);
            vo.setQuotaType(quotaType);
            vo.setDeductedAmount(amount);
            vo.setBizType(bizType);
            vo.setBizId(bizId);
            vo.setSuccess(true);

            log.info("配额扣减成功: userId={}, quotaType={}, amount={}", userId, quotaType, amount);
            return Result.success(vo);
        } catch (Exception e) {
            log.error("配额扣减失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR, "配额扣减失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<BatchQuotaDeductResultVO> batchDeductQuota(Long userId, List<QuotaDeductItemDTO> items) {
        try {
            BatchQuotaDeductResultVO vo = new BatchQuotaDeductResultVO();
            List<QuotaDeductResultVO> results = new ArrayList<>();
            boolean allSuccess = true;
            long totalDeducted = 0;

            for (QuotaDeductItemDTO item : items) {
                Result<QuotaDeductResultVO> result = deductQuota(userId, item.getQuotaType(),
                        item.getAmount(), item.getBizType(), item.getBizId());
                if (result.isSuccess()) {
                    results.add(result.getData());
                    totalDeducted += item.getAmount();
                } else {
                    allSuccess = false;
                }
            }

            vo.setResults(results);
            vo.setAllSuccess(allSuccess);
            vo.setTotalDeducted(totalDeducted);

            return Result.success(vo);
        } catch (Exception e) {
            log.error("批量扣减配额失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR, "批量扣减配额失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> rollbackQuota(Long userId, Integer quotaType, Long amount, String bizType, String bizId) {
        try {
            // 回滚配额（GREATEST 防止异常数据扣成负数）
            int rows = quotaRecordMapper.refundUsage(userId, quotaType, amount);
            if (rows == 0) {
                return Result.error(ResultCode.BUSINESS_ERROR, "配额回滚失败，配额记录不存在");
            }

            log.info("配额回滚成功: userId={}, quotaType={}, amount={}", userId, quotaType, amount);
            return Result.success();
        } catch (Exception e) {
            log.error("配额回滚失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR, "配额回滚失败: " + e.getMessage());
        }
    }

    // ==================== 配额超限处理 ====================

    @Override
    public Result<QuotaExceedHandleResultVO> handleQuotaExceed(Long userId, Integer quotaType, Long exceedAmount) {
        // TODO: 实现配额超限处理逻辑
        return Result.success(new QuotaExceedHandleResultVO());
    }

    @Override
    public Result<IPage<QuotaExceedRecordVO>> getQuotaExceedRecords(QuotaExceedQueryDTO dto) {
        // TODO: 实现获取配额超限记录逻辑
        return Result.success(new Page<>());
    }

    // ==================== 配额重置 ====================

    @Override
    public Result<DailyResetResultVO> dailyResetQuota() {
        try {
            LocalDate today = LocalDate.now();
            int rows = quotaRecordMapper.resetDailyUsage(today);

            DailyResetResultVO vo = new DailyResetResultVO();
            vo.setResetDate(today);
            vo.setResetCount(rows);
            vo.setSuccess(true);

            log.info("每日配额重置成功: 重置日期={}, 重置数量={}", today, rows);
            return Result.success(vo);
        } catch (Exception e) {
            log.error("每日配额重置失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR, "每日配额重置失败: " + e.getMessage());
        }
    }

    @Override
    public Result<MonthlyResetResultVO> monthlyResetQuota() {
        try {
            int rows = quotaRecordMapper.resetMonthlyUsage();

            MonthlyResetResultVO vo = new MonthlyResetResultVO();
            vo.setResetMonth(LocalDate.now());
            vo.setResetCount(rows);
            vo.setSuccess(true);

            log.info("每月配额重置成功: 重置月份={}, 重置数量={}", LocalDate.now(), rows);
            return Result.success(vo);
        } catch (Exception e) {
            log.error("每月配额重置失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR, "每月配额重置失败: " + e.getMessage());
        }
    }

    @Override
    public Result<Void> manualResetUserQuota(Long userId, Integer quotaType, Integer resetType) {
        // TODO: 实现手动重置用户配额逻辑
        return Result.success();
    }

    @Override
    public Result<IPage<QuotaResetRecordVO>> getResetRecords(QuotaResetQueryDTO dto) {
        // TODO: 实现获取重置记录逻辑
        return Result.success(new Page<>());
    }

    // ==================== 私有方法 ====================

    private String generateConfigId() {
        return "QTC" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) +
                UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }

    private String generateRecordId() {
        return "QTR" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) +
                UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private QuotaConfigVO convertConfigToVO(QuotaConfig config) {
        QuotaConfigVO vo = new QuotaConfigVO();
        BeanUtils.copyProperties(config, vo);
        return vo;
    }

    private QuotaRecordVO convertRecordToVO(QuotaRecord record) {
        QuotaRecordVO vo = new QuotaRecordVO();
        BeanUtils.copyProperties(record, vo);
        return vo;
    }
}
