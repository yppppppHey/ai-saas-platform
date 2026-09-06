-- =============================================
-- Chat Service 数据库初始化脚本
-- 包含: 会话表、消息表、提示词模板表
-- =============================================

-- 创建会话表
CREATE TABLE IF NOT EXISTS `chat_conversation` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `title` VARCHAR(255) NOT NULL COMMENT '会话标题',
    `model` VARCHAR(64) NOT NULL COMMENT '使用的模型',
    `provider` VARCHAR(32) DEFAULT 'openai' COMMENT 'AI Provider',
    `system_prompt` TEXT COMMENT '系统提示词',
    `is_pinned` TINYINT NOT NULL DEFAULT 0 COMMENT '置顶状态: 0-未置顶 1-已置顶',
    `is_archived` TINYINT NOT NULL DEFAULT 0 COMMENT '归档状态: 0-未归档 1-已归档',
    `message_count` INT NOT NULL DEFAULT 0 COMMENT '消息数量',
    `token_usage` BIGINT NOT NULL DEFAULT 0 COMMENT 'Token使用量',
    `last_message_at` DATETIME COMMENT '最后消息时间',
    `last_message_preview` VARCHAR(255) COMMENT '最后消息内容预览',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0-禁用 1-正常',
    `extras` JSON COMMENT '扩展字段',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除 1-已删除',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_is_pinned` (`is_pinned`),
    KEY `idx_is_archived` (`is_archived`),
    KEY `idx_last_message_at` (`last_message_at`),
    KEY `idx_status` (`status`),
    KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会话表';

-- 创建消息表
CREATE TABLE IF NOT EXISTS `chat_message` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `conversation_id` BIGINT NOT NULL COMMENT '会话ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `message_type` TINYINT NOT NULL COMMENT '消息类型: 1-用户消息 2-AI回复 3-系统消息',
    `content_type` VARCHAR(16) NOT NULL DEFAULT 'text' COMMENT '内容类型: text/image/file/audio',
    `content` TEXT NOT NULL COMMENT '消息内容',
    `original_content` TEXT COMMENT '原始内容(用于编辑历史)',
    `parent_id` BIGINT COMMENT '父消息ID',
    `reply_to_id` BIGINT COMMENT '回复的消息ID',
    `model` VARCHAR(64) COMMENT '使用的模型',
    `model_version` VARCHAR(32) COMMENT '模型版本',
    `input_tokens` INT DEFAULT 0 COMMENT '输入Token数',
    `output_tokens` INT DEFAULT 0 COMMENT '输出Token数',
    `total_tokens` INT DEFAULT 0 COMMENT '总Token数',
    `cost` DECIMAL(10,6) DEFAULT 0 COMMENT '费用消耗',
    `generate_time` BIGINT COMMENT '生成耗时(毫秒)',
    `first_token_latency` BIGINT COMMENT '首字延迟(毫秒)',
    `edit_status` TINYINT DEFAULT 0 COMMENT '编辑状态: 0-未编辑 1-已编辑 2-已删除',
    `edited_at` DATETIME COMMENT '编辑时间',
    `edit_count` INT DEFAULT 0 COMMENT '编辑次数',
    `regenerate_count` INT DEFAULT 0 COMMENT '重新生成次数',
    `regenerate_from_id` BIGINT COMMENT '重新生成的源消息ID',
    `status` TINYINT DEFAULT 1 COMMENT '状态: 0-生成中 1-完成 2-失败 3-取消',
    `error_msg` TEXT COMMENT '错误信息',
    `attachments` JSON COMMENT '文件附件',
    `citation_ids` JSON COMMENT '引用文档ID列表',
    `extras` JSON COMMENT '扩展字段',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除 1-已删除',
    PRIMARY KEY (`id`),
    KEY `idx_conversation_id` (`conversation_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_message_type` (`message_type`),
    KEY `idx_parent_id` (`parent_id`),
    KEY `idx_status` (`status`),
    KEY `idx_created_at` (`created_at`),
    KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息表';

-- 创建提示词模板表
CREATE TABLE IF NOT EXISTS `chat_prompt_template` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `name` VARCHAR(128) NOT NULL COMMENT '模板名称',
    `description` VARCHAR(512) COMMENT '模板描述',
    `content` TEXT NOT NULL COMMENT '模板内容',
    `variables` JSON COMMENT '变量定义',
    `template_type` VARCHAR(32) NOT NULL DEFAULT 'system' COMMENT '模板类型: system/user/assistant',
    `category` VARCHAR(64) COMMENT '场景分类',
    `is_builtin` TINYINT NOT NULL DEFAULT 0 COMMENT '是否为内置模板: 0-否 1-是',
    `creator_id` BIGINT NOT NULL DEFAULT 0 COMMENT '创建者ID(0表示系统)',
    `usage_count` INT NOT NULL DEFAULT 0 COMMENT '使用次数',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0-禁用 1-正常',
    `extras` JSON COMMENT '扩展字段',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除 1-已删除',
    PRIMARY KEY (`id`),
    KEY `idx_name` (`name`),
    KEY `idx_template_type` (`template_type`),
    KEY `idx_category` (`category`),
    KEY `idx_is_builtin` (`is_builtin`),
    KEY `idx_creator_id` (`creator_id`),
    KEY `idx_status` (`status`),
    KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='提示词模板表';

-- 插入默认的系统提示词模板
INSERT INTO `chat_prompt_template` (`name`, `description`, `content`, `template_type`, `category`, `is_builtin`, `creator_id`, `status`) VALUES
('通用助手', '一个 helpful、harmless、honest 的AI助手', 'You are a helpful, harmless, and honest AI assistant. Please provide accurate and useful information to help users solve their problems.', 'system', 'general', 1, 0, 1),
('代码助手', '专业的编程助手，擅长各种编程语言', 'You are a professional programming assistant. You are proficient in various programming languages including but not limited to Java, Python, JavaScript, Go, Rust, C++, etc. You can help users write code, debug, explain code, and provide best practices.', 'system', 'programming', 1, 0, 1),
('翻译助手', '专业的翻译专家，支持多语言互译', 'You are a professional translation expert. You can accurately translate between multiple languages including Chinese, English, Japanese, Korean, French, German, Spanish, etc. You not only translate literally but also consider cultural context to provide natural and accurate translations.', 'system', 'translation', 1, 0, 1),
('写作助手', '专业的写作助手，帮助提升文章质量', 'You are a professional writing assistant. You can help users improve their writing, whether it\'s academic papers, business documents, creative writing, or casual content. You provide suggestions for grammar, style, structure, and clarity.', 'system', 'writing', 1, 0, 1);
