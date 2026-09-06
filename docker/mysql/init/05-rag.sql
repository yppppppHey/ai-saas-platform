-- rag 服务表：独立库 ai_saas_rag
USE `ai_saas_rag`;

-- =============================================
-- RAG Service 数据库初始化脚本
-- 包含: 知识库表、文档表
-- =============================================

-- 创建知识库表
CREATE TABLE IF NOT EXISTS `rag_knowledge_base` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `kb_id` VARCHAR(64) NOT NULL COMMENT '知识库业务ID',
    `kb_name` VARCHAR(128) NOT NULL COMMENT '知识库名称',
    `description` VARCHAR(512) DEFAULT NULL COMMENT '描述',
    `user_id` BIGINT NOT NULL COMMENT '所属用户ID',
    `embedding_model` VARCHAR(64) DEFAULT 'text-embedding-3-small' COMMENT '向量化模型',
    `vector_store` VARCHAR(32) DEFAULT 'qdrant' COMMENT '向量存储类型',
    `dimension` INT DEFAULT 1536 COMMENT '向量维度',
    `chunk_size` INT DEFAULT 1000 COMMENT '分块大小',
    `chunk_overlap` INT DEFAULT 200 COMMENT '分块重叠',
    `total_documents` INT DEFAULT 0 COMMENT '文档总数',
    `total_chunks` INT DEFAULT 0 COMMENT '分块总数',
    `total_size` BIGINT DEFAULT 0 COMMENT '总大小(字节)',
    `access_level` TINYINT DEFAULT 1 COMMENT '访问级别: 1-私有 2-团队 3-公开',
    `team_id` BIGINT DEFAULT NULL COMMENT '团队ID',
    `status` TINYINT DEFAULT 1 COMMENT '状态: 0-禁用 1-正常 2-构建中 3-构建失败',
    `last_build_at` DATETIME DEFAULT NULL COMMENT '最后构建时间',
    `build_progress` TINYINT DEFAULT 0 COMMENT '构建进度',
    `build_error` TEXT COMMENT '构建错误信息',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_kb_id` (`kb_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_user_status` (`user_id`,`status`),
    KEY `idx_team_id` (`team_id`),
    KEY `idx_status` (`status`),
    KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识库表';

-- 创建知识库文档表
CREATE TABLE IF NOT EXISTS `rag_document` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `doc_id` VARCHAR(64) NOT NULL COMMENT '文档业务ID',
    `kb_id` BIGINT NOT NULL COMMENT '所属知识库ID',
    `user_id` BIGINT NOT NULL COMMENT '上传用户ID',
    `doc_name` VARCHAR(255) NOT NULL COMMENT '文档名称',
    `doc_type` VARCHAR(32) NOT NULL COMMENT '文档类型: pdf/doc/docx/txt/md/html',
    `doc_size` BIGINT NOT NULL COMMENT '文档大小(字节)',
    `storage_key` VARCHAR(512) NOT NULL COMMENT '存储路径/对象键',
    `storage_type` VARCHAR(32) DEFAULT 'minio' COMMENT '存储类型: minio/oss/s3',
    `encoding` VARCHAR(32) DEFAULT 'UTF-8' COMMENT '文件编码',
    `page_count` INT DEFAULT NULL COMMENT '页数(PDF/Word)',
    `char_count` BIGINT DEFAULT NULL COMMENT '字符数',
    `chunk_count` INT DEFAULT 0 COMMENT '分块数量',
    `extraction_method` VARCHAR(64) DEFAULT NULL COMMENT '提取方式',
    `extraction_result` JSON DEFAULT NULL COMMENT '提取结果元数据',
    `process_status` TINYINT NOT NULL DEFAULT 0 COMMENT '处理状态: 0-待处理 1-处理中 2-成功 3-失败 4-部分成功',
    `process_progress` TINYINT DEFAULT 0 COMMENT '处理进度',
    `process_started_at` DATETIME DEFAULT NULL COMMENT '开始处理时间',
    `process_completed_at` DATETIME DEFAULT NULL COMMENT '处理完成时间',
    `process_error` TEXT COMMENT '处理错误信息',
    `version` INT DEFAULT 1 COMMENT '版本号(更新时递增)',
    `is_latest` TINYINT DEFAULT 1 COMMENT '是否最新版本',
    `previous_version_id` BIGINT DEFAULT NULL COMMENT '上一版本ID',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_doc_id` (`doc_id`),
    KEY `idx_kb_id` (`kb_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_kb_status` (`kb_id`,`process_status`),
    KEY `idx_process_status` (`process_status`),
    KEY `idx_doc_type` (`doc_type`),
    KEY `idx_created_at` (`created_at`),
    KEY `idx_version` (`previous_version_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识库文档表';
