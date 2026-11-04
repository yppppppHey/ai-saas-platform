package com.aisaas.billing.dto;

import lombok.Data;

import java.util.List;

/**
 * 批量Token使用记录DTO
 */
@Data
public class BatchTokenUsageDTO {

    private List<TokenUsageRecordDTO> records;
}
