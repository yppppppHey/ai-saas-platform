package com.aisaas.billing.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 每日重置结果VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyResetResultVO implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 重置日期 */
    private LocalDate resetDate;

    /** 重置数量 */
    private Integer resetCount;

    /** 是否成功 */
    private Boolean success;

    /** 消息 */
    private String message;
}
