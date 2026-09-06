
## 3.2 核心数据表设计

### 3.2.1 用户模块 (user_*)

#### 3.2.1.1 用户账户表 (user_account)

```sql
CREATE TABLE `user_account` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `username` varchar(64) NOT NULL COMMENT '用户名',
  `email` varchar(128) DEFAULT NULL COMMENT '邮箱',
  `phone` varchar(32) DEFAULT NULL COMMENT '手机号',
  `password_hash` varchar(255) NOT NULL COMMENT '密码哈希(BCrypt)',
  `avatar_url` varchar(512) DEFAULT NULL COMMENT '头像URL',
  `user_type` tinyint unsigned NOT NULL DEFAULT '1' COMMENT '用户类型: 1-普通用户 2-VIP用户 9-管理员',
  `status` tinyint unsigned NOT NULL DEFAULT '1' COMMENT '状态: 0-禁用 1-正常 2-待验证',
  `last_login_at` datetime DEFAULT NULL COMMENT '最后登录时间',
  `last_login_ip` varchar(64) DEFAULT NULL COMMENT '最后登录IP',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '是否删除: 0-否 1-是',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  UNIQUE KEY `uk_email` (`email`),
  UNIQUE KEY `uk_phone` (`phone`),
  KEY `idx_user_type` (`user_type`),
  KEY `idx_status` (`status`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户账户表';
```

#### 3.2.1.2 用户配额表 (user_quota)

```sql
CREATE TABLE `user_quota` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint unsigned NOT NULL COMMENT '用户ID',
  `quota_type` tinyint unsigned NOT NULL DEFAULT '1' COMMENT '配额类型: 1-对话token 2-任务次数 3-存储空间',
  `daily_limit` bigint unsigned NOT NULL DEFAULT '0' COMMENT '每日限制(0为无限制)',
  `monthly_limit` bigint unsigned NOT NULL DEFAULT '0' COMMENT '每月限制(0为无限制)',
  `total_limit` bigint unsigned NOT NULL DEFAULT '0' COMMENT '总计限制(0为无限制)',
  `daily_used` bigint unsigned NOT NULL DEFAULT '0' COMMENT '今日已使用',
  `monthly_used` bigint unsigned NOT NULL DEFAULT '0' COMMENT '本月已使用',
  `total_used` bigint unsigned NOT NULL DEFAULT '0' COMMENT '总计已使用',
  `reset_day` tinyint unsigned NOT NULL DEFAULT '1' COMMENT '每月重置日',
  `effective_at` datetime NOT NULL COMMENT '生效时间',
  `expire_at` datetime DEFAULT NULL COMMENT '过期时间',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_quota_type` (`user_id`,`quota_type`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_effective_at` (`effective_at`),
  KEY `idx_expire_at` (`expire_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户配额表';
```

#### 3.2.1.3 用户登录记录表 (user_login_log)

```sql
CREATE TABLE `user_login_log` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint unsigned NOT NULL COMMENT '用户ID',
  `login_type` tinyint unsigned NOT NULL DEFAULT '1' COMMENT '登录方式: 1-密码 2-短信 3-邮箱 4-第三方',
  `login_ip` varchar(64) NOT NULL COMMENT '登录IP',
  `login_location` varchar(128) DEFAULT NULL COMMENT '登录地点',
  `user_agent` text COMMENT '浏览器UA',
  `device_type` varchar(64) DEFAULT NULL COMMENT '设备类型',
  `device_id` varchar(128) DEFAULT NULL COMMENT '设备标识',
  `login_status` tinyint unsigned NOT NULL DEFAULT '1' COMMENT '登录状态: 0-失败 1-成功',
  `fail_reason` varchar(255) DEFAULT NULL COMMENT '失败原因',
  `login_at` datetime NOT NULL COMMENT '登录时间',
  `logout_at` datetime DEFAULT NULL COMMENT '登出时间',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_login_ip` (`login_ip`),
  KEY `idx_login_at` (`login_at`),
  KEY `idx_login_status` (`login_status`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户登录记录表';
```

#### 3.2.2 对话模块 (chat_*)

#### 3.2.2.1 会话表 (chat_conversation)

```sql
CREATE TABLE `chat_prompt_template` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `template_code` varchar(64) NOT NULL COMMENT '模板编码',
  `template_name` varchar(128) NOT NULL COMMENT '模板名称',
  `category` varchar(64) DEFAULT 'general' COMMENT '分类: general/coding/writing/translation',
  `description` varchar(512) DEFAULT NULL COMMENT '模板描述',
  `system_prompt` text NOT NULL COMMENT '系统提示词',
  `user_prompt_template` text COMMENT '用户提示词模板(支持变量)',
  `variables` json DEFAULT NULL COMMENT '变量定义[{"name":"var1","desc":"描述","required":true}]',
  `model_id` varchar(64) DEFAULT NULL COMMENT '推荐模型',
  `temperature` decimal(3,2) DEFAULT NULL COMMENT '推荐温度',
  `max_tokens` int unsigned DEFAULT NULL COMMENT '推荐最大token',
  `icon` varchar(128) DEFAULT NULL COMMENT '图标',
  `sort_order` int unsigned DEFAULT '0' COMMENT '排序',
  `is_builtin` tinyint unsigned DEFAULT '0' COMMENT '是否内置',
  `is_public` tinyint unsigned DEFAULT '1' COMMENT '是否公开',
  `creator_id` bigint unsigned DEFAULT NULL COMMENT '创建者ID',
  `usage_count` bigint unsigned DEFAULT '0' COMMENT '使用次数',
  `status` tinyint unsigned DEFAULT '1' COMMENT '状态: 0-禁用 1-启用',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_template_code` (`template_code`),
  KEY `idx_category` (`category`),
  KEY `idx_creator` (`creator_id`),
  KEY `idx_status` (`status`),
  KEY `idx_is_public` (`is_public`,`status`),
  KEY `idx_sort_order` (`sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Prompt模板表';
```

### 3.2.3 任务模块 (task_*)

#### 3.2.3.1 异步任务表 (task_async_job)

```sql
CREATE TABLE `task_async_job` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `task_id` varchar(64) NOT NULL COMMENT '业务任务ID(对外暴露)',
  `task_type` varchar(64) NOT NULL COMMENT '任务类型: summary/extract/report/generate',
  `task_name` varchar(128) DEFAULT NULL COMMENT '任务名称',
  `user_id` bigint unsigned NOT NULL COMMENT '用户ID',
  `status` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '状态: 0-PENDING 1-QUEUED 2-RUNNING 3-SUCCESS 4-FAILED 5-CANCELLED 6-TIMEOUT',
  `priority` tinyint unsigned NOT NULL DEFAULT '5' COMMENT '优先级: 1-10, 数字越小优先级越高',
  `progress` tinyint unsigned DEFAULT '0' COMMENT '进度百分比(0-100)',
  `progress_detail` varchar(512) DEFAULT NULL COMMENT '进度详情描述',
  `input_params` json NOT NULL COMMENT '输入参数',
  `output_result` longtext COMMENT '输出结果',
  `output_url` varchar(512) DEFAULT NULL COMMENT '输出文件URL',
  `model_id` varchar(64) DEFAULT NULL COMMENT '使用的模型',
  `provider` varchar(32) DEFAULT NULL COMMENT 'AI提供商',
  `prompt_tokens` int unsigned DEFAULT '0' COMMENT '输入token数',
  `completion_tokens` int unsigned DEFAULT '0' COMMENT '输出token数',
  `total_tokens` int unsigned DEFAULT '0' COMMENT '总token数',
  `cost_usd` decimal(10,6) DEFAULT '0.000000' COMMENT '成本(美元)',
  `started_at` datetime DEFAULT NULL COMMENT '开始执行时间',
  `completed_at` datetime DEFAULT NULL COMMENT '完成时间',
  `cancelled_at` datetime DEFAULT NULL COMMENT '取消时间',
  `timeout_at` datetime DEFAULT NULL COMMENT '超时时间',
  `retry_count` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '重试次数',
  `max_retry` tinyint unsigned NOT NULL DEFAULT '3' COMMENT '最大重试次数',
  `error_code` varchar(64) DEFAULT NULL COMMENT '错误码',
  `error_msg` text COMMENT '错误信息',
  `stack_trace` text COMMENT '异常堆栈',
  `worker_node` varchar(64) DEFAULT NULL COMMENT '执行节点',
  `mq_message_id` varchar(128) DEFAULT NULL COMMENT 'MQ消息ID',
  `trace_id` varchar(64) DEFAULT NULL COMMENT '链路追踪ID',
  `callback_url` varchar(512) DEFAULT NULL COMMENT '回调URL',
  `callback_status` tinyint unsigned DEFAULT '0' COMMENT '回调状态: 0-未回调 1-成功 2-失败',
  `callback_count` tinyint unsigned DEFAULT '0' COMMENT '回调次数',
  `last_callback_at` datetime DEFAULT NULL COMMENT '最后回调时间',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_task_id` (`task_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_status` (`status`),
  KEY `idx_user_status` (`user_id`,`status`),
  KEY `idx_task_type` (`task_type`),
  KEY `idx_created_at` (`created_at`),
  KEY `idx_completed_at` (`completed_at`),
  KEY `idx_trace_id` (`trace_id`),
  KEY `idx_mq_message_id` (`mq_message_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='异步任务表';
```

### 3.2.4 RAG知识库模块 (rag_*)

#### 3.2.4.1 知识库表 (rag_knowledge_base)

```sql
CREATE TABLE `rag_knowledge_base` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `kb_id` varchar(64) NOT NULL COMMENT '知识库业务ID',
  `kb_name` varchar(128) NOT NULL COMMENT '知识库名称',
  `description` varchar(512) DEFAULT NULL COMMENT '描述',
  `user_id` bigint unsigned NOT NULL COMMENT '所属用户ID',
  `embedding_model` varchar(64) DEFAULT 'text-embedding-3-small' COMMENT '向量化模型',
  `vector_store` varchar(32) DEFAULT 'qdrant' COMMENT '向量存储类型',
  `dimension` int unsigned DEFAULT '1536' COMMENT '向量维度',
  `chunk_size` int unsigned DEFAULT '1000' COMMENT '分块大小',
  `chunk_overlap` int unsigned DEFAULT '200' COMMENT '分块重叠',
  `total_documents` int unsigned DEFAULT '0' COMMENT '文档总数',
  `total_chunks` int unsigned DEFAULT '0' COMMENT '分块总数',
  `total_size` bigint unsigned DEFAULT '0' COMMENT '总大小(字节)',
  `access_level` tinyint unsigned DEFAULT '1' COMMENT '访问级别: 1-私有 2-团队 3-公开',
  `team_id` bigint unsigned DEFAULT NULL COMMENT '团队ID',
  `status` tinyint unsigned DEFAULT '1' COMMENT '状态: 0-禁用 1-正常 2-构建中 3-构建失败',
  `last_build_at` datetime DEFAULT NULL COMMENT '最后构建时间',
  `build_progress` tinyint unsigned DEFAULT '0' COMMENT '构建进度',
  `build_error` text COMMENT '构建错误信息',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_kb_id` (`kb_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_user_status` (`user_id`,`status`),
  KEY `idx_team_id` (`team_id`),
  KEY `idx_status` (`status`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识库表';
```

#### 3.2.4.2 知识库文档表 (rag_document)

```sql
CREATE TABLE `rag_document` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `doc_id` varchar(64) NOT NULL COMMENT '文档业务ID',
  `kb_id` bigint unsigned NOT NULL COMMENT '所属知识库ID',
  `user_id` bigint unsigned NOT NULL COMMENT '上传用户ID',
  `doc_name` varchar(255) NOT NULL COMMENT '文档名称',
  `doc_type` varchar(32) NOT NULL COMMENT '文档类型: pdf/doc/docx/txt/md/html',
  `doc_size` bigint unsigned NOT NULL COMMENT '文档大小(字节)',
  `storage_key` varchar(512) NOT NULL COMMENT '存储路径/对象键',
  `storage_type` varchar(32) DEFAULT 'minio' COMMENT '存储类型: minio/oss/s3',
  `encoding` varchar(32) DEFAULT 'UTF-8' COMMENT '文件编码',
  `page_count` int unsigned DEFAULT NULL COMMENT '页数(PDF/Word)',
  `char_count` bigint unsigned DEFAULT NULL COMMENT '字符数',
  `chunk_count` int unsigned DEFAULT '0' COMMENT '分块数量',
  `extraction_method` varchar(64) DEFAULT NULL COMMENT '提取方式',
  `extraction_result` json DEFAULT NULL COMMENT '提取结果元数据',
  `process_status` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '处理状态: 0-待处理 1-处理中 2-成功 3-失败 4-部分成功',
  `process_progress` tinyint unsigned DEFAULT '0' COMMENT '处理进度',
  `process_started_at` datetime DEFAULT NULL COMMENT '开始处理时间',
  `process_completed_at` datetime DEFAULT NULL COMMENT '处理完成时间',
  `process_error` text COMMENT '处理错误信息',
  `version` int unsigned DEFAULT '1' COMMENT '版本号(更新时递增)',
  `is_latest` tinyint unsigned DEFAULT '1' COMMENT '是否最新版本',
  `previous_version_id` bigint unsigned DEFAULT NULL COMMENT '上一版本ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '是否删除',
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
```

### 3.2.5 计费与配额模块 (billing_*)

#### 3.2.5.1 Token消费记录表 (billing_token_usage)

```sql
CREATE TABLE `billing_token_usage` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `usage_id` varchar(64) NOT NULL COMMENT '消费记录ID',
  `user_id` bigint unsigned NOT NULL COMMENT '用户ID',
  `conversation_id` bigint unsigned DEFAULT NULL COMMENT '关联会话ID',
  `message_id` bigint unsigned DEFAULT NULL COMMENT '关联消息ID',
  `task_id` bigint unsigned DEFAULT NULL COMMENT '关联任务ID',
  `provider` varchar(32) NOT NULL COMMENT '提供商: openai/deepseek/claude',
  `model_id` varchar(64) NOT NULL COMMENT '模型ID',
  `operation_type` varchar(32) NOT NULL COMMENT '操作类型: chat/completion/embedding',
  `prompt_tokens` int unsigned NOT NULL DEFAULT '0' COMMENT '输入token数',
  `completion_tokens` int unsigned NOT NULL DEFAULT '0' COMMENT '输出token数',
  `total_tokens` int unsigned NOT NULL DEFAULT '0' COMMENT '总token数',
  `prompt_cost` decimal(12,8) NOT NULL DEFAULT '0.00000000' COMMENT '输入成本(USD)',
  `completion_cost` decimal(12,8) NOT NULL DEFAULT '0.00000000' COMMENT '输出成本(USD)',
  `total_cost` decimal(12,8) NOT NULL DEFAULT '0.00000000' COMMENT '总成本(USD)',
  `exchange_rate` decimal(10,6) DEFAULT '7.200000' COMMENT '汇率(USD to CNY)',
  `cost_cny` decimal(12,6) DEFAULT '0.000000' COMMENT '成本(CNY)',
  `latency_ms` int unsigned DEFAULT '0' COMMENT '延迟(毫秒)',
  `request_headers` json DEFAULT NULL COMMENT '请求头信息(脱敏)',
  `request_body` json DEFAULT NULL COMMENT '请求体(脱敏)',
  `response_body` json DEFAULT NULL COMMENT '响应体(脱敏)',
  `error_code` varchar(64) DEFAULT NULL COMMENT '错误码',
  `error_msg` text COMMENT '错误信息',
  `is_billed` tinyint unsigned NOT NULL DEFAULT '1' COMMENT '是否计费: 0-否 1-是',
  `billed_at` datetime DEFAULT NULL COMMENT '计费时间',
  `usage_date` date NOT NULL COMMENT '使用日期(用于分表)',
  `usage_hour` tinyint unsigned NOT NULL COMMENT '使用小时',
  `trace_id` varchar(64) DEFAULT NULL COMMENT '链路追踪ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_usage_id` (`usage_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_user_date` (`user_id`,`usage_date`),
  KEY `idx_conversation_id` (`conversation_id`),
  KEY `idx_task_id` (`task_id`),
  KEY `idx_provider_model` (`provider`,`model_id`),
  KEY `idx_usage_date` (`usage_date`),
  KEY `idx_billed_at` (`billed_at`),
  KEY `idx_trace_id` (`trace_id`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Token消费记录表'
PARTITION BY RANGE (TO_DAYS(usage_date)) (
    PARTITION p202401 VALUES LESS THAN (TO_DAYS('2024-02-01')),
    PARTITION p202402 VALUES LESS THAN (TO_DAYS('2024-03-01')),
    PARTITION p202403 VALUES LESS THAN (TO_DAYS('2024-04-01')),
    PARTITION p202404 VALUES LESS THAN (TO_DAYS('2024-05-01')),
    PARTITION p202405 VALUES LESS THAN (TO_DAYS('2024-06-01')),
    PARTITION p202406 VALUES LESS THAN (TO_DAYS('2024-07-01')),
    PARTITION p202407 VALUES LESS THAN (TO_DAYS('2024-08-01')),
    PARTITION p202408 VALUES LESS THAN (TO_DAYS('2024-09-01')),
    PARTITION p202409 VALUES LESS THAN (TO_DAYS('2024-10-01')),
    PARTITION p202410 VALUES LESS THAN (TO_DAYS('2024-11-01')),
    PARTITION p202411 VALUES LESS THAN (TO_DAYS('2024-12-01')),
    PARTITION p202412 VALUES LESS THAN (TO_DAYS('2025-01-01')),
    PARTITION p_future VALUES LESS THAN MAXVALUE
);
```

## 3.3 Redis数据结构

### 3.3.1 缓存Key设计

```
# 用户会话缓存
user:session:{token} -> Hash {userId, username, role, expireAt}
TTL: 7天

# 用户配额缓存
user:quota:{userId}:{quotaType} -> Hash {dailyLimit, dailyUsed, lastResetDate}
TTL: 1天

# 对话上下文缓存
chat:context:{conversationId} -> List [message1, message2, ...]
TTL: 30天

# 对话流式响应缓存
chat:stream:{messageId} -> Stream {content, finishReason}
TTL: 1小时

# 限流计数
ratelimit:{userId}:{api} -> String count
TTL: 1分钟

# 分布式锁
lock:{resource} -> String {owner, expireAt}
TTL: 30秒

# 任务进度
async:progress:{taskId} -> Hash {progress, status, result}
TTL: 7天

# 热点数据缓存
hot:data:{key} -> String/Hash/JSON
TTL: 10分钟
```

### 3.3.2 Redis数据结构示例

```java
/**
 * Redis Key 管理器
 */
public final class RedisKeys {
    
    private static final String PREFIX = "ai:platform:";
    
    // ========== 用户会话 ==========
    public static String userSession(String token) {
        return PREFIX + "user:session:" + token;
    }
    
    // ========== 用户配额 ==========
    public static String userQuota(Long userId, String quotaType) {
        return PREFIX + "user:quota:" + userId + ":" + quotaType;
    }
    
    // ========== 对话上下文 ==========
    public static String chatContext(Long conversationId) {
        return PREFIX + "chat:context:" + conversationId;
    }
    
    // ========== 流式消息 ==========
    public static String chatStream(Long messageId) {
        return PREFIX + "chat:stream:" + messageId;
    }
    
    // ========== 限流计数 ==========
    public static String rateLimit(Long userId, String api) {
        return PREFIX + "ratelimit:" + userId + ":" + api;
    }
    
    // ========== 分布式锁 ==========
    public static String lock(String resource) {
        return PREFIX + "lock:" + resource;
    }
    
    // ========== 任务进度 ==========
    public static String taskProgress(String taskId) {
        return PREFIX + "task:progress:" + taskId;
    }
    
    // ========== 幂等令牌 ==========
    public static String idempotent(String token) {
        return PREFIX + "idempotent:" + token;
    }
}
```

## 3.4 数据库关系图

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                          数据库ER关系图                                          │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                 │
│   ┌─────────────────────┐         ┌─────────────────────┐                      │
│   │   user_account      │         │   user_quota        │                      │
│   ├─────────────────────┤         ├─────────────────────┤                      │
│   │ PK: id              │────1:n──│ FK: user_id         │                      │
│   │     username        │         │     quota_type      │                      │
│   │     email           │         │     daily_limit     │                      │
│   │     user_type       │         │     daily_used      │                      │
│   └─────────────────────┘         └─────────────────────┘                      │
│            │                                                                    │
│            │ 1:n                                                                 │
│            ▼                                                                    │
│   ┌─────────────────────┐         ┌─────────────────────┐                      │
│   │  chat_conversation  │         │  chat_message       │                      │
│   ├─────────────────────┤         ├─────────────────────┤                      │
│   │ PK: id              │────1:n──│ FK: conversation_id │                      │
│   │ FK: user_id         │         │ FK: user_id         │                      │
│   │     title           │         │     message_type    │                      │
│   │     model_id        │         │     content         │                      │
│   │     status          │         │     prompt_tokens   │                      │
│   └─────────────────────┘         │     total_cost      │                      │
│                                     └─────────────────────┘                      │
│   ┌─────────────────────┐                                                       │
│   │chat_prompt_template│                                                       │
│   ├─────────────────────┤                                                       │
│   │ PK: id              │                                                       │
│   │     template_code   │                                                       │
│   │     category        │                                                       │
│   │     system_prompt   │                                                       │
│   └─────────────────────┘                                                       │
│                                                                                 │
│   ┌─────────────────────┐         ┌─────────────────────┐                      │
│   │   task_async_job    │         │billing_token_usage  │                      │
│   ├─────────────────────┤         ├─────────────────────┤                      │
│   │ PK: id              │         │ PK: id              │                      │
│   │     task_id         │         │ FK: user_id         │                      │
│   │ FK: user_id         │         │     conversation_id │                      │
│   │     task_type       │         │     model_id        │                      │
│   │     status          │         │     prompt_tokens   │                      │
│   │     input_params    │         │     total_cost      │                      │
│   │     output_result   │         │     usage_date      │                      │
│   │     retry_count     │         │                     │                      │
│   │     error_msg       │         │                     │                      │
│   └─────────────────────┘         └─────────────────────┘                      │
│                                                                                 │
│   ┌─────────────────────┐         ┌─────────────────────┐                      │
│   │  rag_knowledge_base │         │    rag_document     │                      │
│   ├─────────────────────┤         ├─────────────────────┤                      │
│   │ PK: id              │────1:n──│ FK: kb_id           │                      │
│   │     kb_id           │         │ FK: user_id         │                      │
│   │ FK: user_id         │         │     doc_name        │                      │
│   │     kb_name         │         │     doc_type        │                      │
│   │     embedding_model │         │     storage_key     │                      │
│   │     chunk_size      │         │     process_status  │                      │
│   │     status          │         │     chunk_count     │                      │
│   └─────────────────────┘         └─────────────────────┘                      │
│                                                                                 │
└─────────────────────────────────────────────────────────────────────────────────┘
```

