package com.aisaas.rag.dto.maintenance;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 知识库备份DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BackupDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 备份ID */
    private String backupId;

    /** 知识库ID */
    private String kbId;

    /** 备份大小(字节) */
    private Long backupSize;

    /** 文档数量 */
    private Integer documentCount;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 创建人 */
    private Long createdBy;
}
