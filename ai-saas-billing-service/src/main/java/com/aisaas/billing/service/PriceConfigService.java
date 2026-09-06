package com.aisaas.billing.service;

import com.aisaas.billing.dto.*;
import com.aisaas.common.result.Result;
import com.baomidou.mybatisplus.core.metadata.IPage;

import java.util.List;

/**
 * 价格配置服务接口
 */
public interface PriceConfigService {

    /**
     * 创建价格配置
     */
    Result<Void> createPriceConfig(PriceConfigCreateDTO dto);

    /**
     * 更新价格配置
     */
    Result<Void> updatePriceConfig(PriceConfigUpdateDTO dto);

    /**
     * 删除价格配置
     */
    Result<Void> deletePriceConfig(String configId);

    /**
     * 获取价格配置详情
     */
    Result<PriceConfigVO> getPriceConfig(String configId);

    /**
     * 分页查询价格配置
     */
    Result<IPage<PriceConfigVO>> queryPriceConfig(PriceConfigQueryDTO dto);

    /**
     * 查询所有有效配置
     */
    Result<List<PriceConfigVO>> getAllValidConfigs();

    /**
     * 根据条件查询价格
     */
    Result<PriceConfigVO> getPriceByCondition(String provider, String modelId, String operationType);
}
