package com.aisaas.rag.dto.maintenance;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 索引重建进度DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RebuildProgressDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 知识库ID */
    private String kbId;

    /** 状态 */
    private Integer status;

    /** 重建进度(百分比) */
    private Integer progress;

    /** 上次构建时间 */
    private LocalDateTime lastBuildAt;
}
