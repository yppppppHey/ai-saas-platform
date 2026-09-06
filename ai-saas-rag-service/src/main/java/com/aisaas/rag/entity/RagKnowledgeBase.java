package com.aisaas.rag.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("rag_knowledge_base")
public class RagKnowledgeBase {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String kbId;

    private String kbName;

    private String description;

    private Long userId;

    private String embeddingModel;

    private String vectorStore;

    private Integer dimension;

    private Integer chunkSize;

    private Integer chunkOverlap;

    private Integer totalDocuments;

    private Integer totalChunks;

    private Long totalSize;

    private Integer accessLevel;

    private Long teamId;

    private Integer status;

    private LocalDateTime lastBuildAt;

    private Integer buildProgress;

    private String buildError;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private Integer isDeleted;
}
