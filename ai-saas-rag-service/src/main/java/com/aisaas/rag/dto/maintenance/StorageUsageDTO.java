package com.aisaas.rag.dto.maintenance;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 存储用量DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StorageUsageDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 知识库ID */
    private String kbId;

    /** 存储总大小(字节) */
    private Long totalSize;

    /** 文档数量 */
    private Integer documentCount;

    /** 分块数量 */
    private Integer chunkCount;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
