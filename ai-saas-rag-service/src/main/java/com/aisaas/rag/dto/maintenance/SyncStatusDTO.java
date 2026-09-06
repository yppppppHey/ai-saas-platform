package com.aisaas.rag.dto.maintenance;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 文档同步状态DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SyncStatusDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 知识库ID */
    private String kbId;

    /** 同步状态 */
    private String syncStatus;

    /** 上次同步时间 */
    private LocalDateTime lastSyncAt;

    /** 文档数量 */
    private Integer documentCount;
}
