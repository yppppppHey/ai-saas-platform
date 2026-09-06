-- ============================================================
-- AI SaaS User Service - RBAC and VIP Tables
-- Version: 1.0.0
-- Date: 2026-05-27
-- ============================================================

USE `ai_platform`;

-- ============================================================
-- 角色表 (user_role)
-- ============================================================
CREATE TABLE IF NOT EXISTS `user_role` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `role_code` varchar(64) NOT NULL COMMENT '角色编码',
  `role_name` varchar(128) NOT NULL COMMENT '角色名称',
  `description` varchar(512) DEFAULT NULL COMMENT '角色描述',
  `role_type` tinyint unsigned NOT NULL DEFAULT '1' COMMENT '角色类型: 1-系统角色 2-自定义角色',
  `data_scope` tinyint unsigned DEFAULT '1' COMMENT '数据范围: 1-全部 2-本部门 3-本部门及以下 4-仅本人',
  `sort` int unsigned DEFAULT '0' COMMENT '排序号',
  `status` tinyint unsigned NOT NULL DEFAULT '1' COMMENT '状态: 0-禁用 1-启用',
  `effective_at` datetime DEFAULT NULL COMMENT '生效时间',
  `expire_at` datetime DEFAULT NULL COMMENT '过期时间',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '是否删除: 0-否 1-是',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_code` (`role_code`),
  KEY `idx_status` (`status`),
  KEY `idx_role_type` (`role_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色表';

-- ============================================================
-- 用户角色关联表 (user_role_relation)
-- ============================================================
CREATE TABLE IF NOT EXISTS `user_role_relation` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint unsigned NOT NULL COMMENT '用户ID',
  `role_id` bigint unsigned NOT NULL COMMENT '角色ID',
  `grant_type` tinyint unsigned NOT NULL DEFAULT '2' COMMENT '授予方式: 1-系统分配 2-手动分配 3-条件触发',
  `granted_by` bigint unsigned DEFAULT NULL COMMENT '授予者ID',
  `effective_at` datetime DEFAULT NULL COMMENT '生效时间',
  `expire_at` datetime DEFAULT NULL COMMENT '过期时间',
  `status` tinyint unsigned NOT NULL DEFAULT '1' COMMENT '状态: 0-禁用 1-启用',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '是否删除: 0-否 1-是',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_role` (`user_id`,`role_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_role_id` (`role_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户角色关联表';

-- ============================================================
-- 权限表 (user_permission)
-- ============================================================
CREATE TABLE IF NOT EXISTS `user_permission` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `permission_code` varchar(128) NOT NULL COMMENT '权限编码',
  `permission_name` varchar(128) NOT NULL COMMENT '权限名称',
  `description` varchar(512) DEFAULT NULL COMMENT '权限描述',
  `permission_type` tinyint unsigned NOT NULL DEFAULT '1' COMMENT '权限类型: 1-菜单 2-按钮 3-接口 4-数据',
  `resource` varchar(255) DEFAULT NULL COMMENT '资源路径',
  `method` varchar(32) DEFAULT NULL COMMENT 'HTTP方法: GET/POST/PUT/DELETE',
  `parent_id` bigint unsigned DEFAULT NULL COMMENT '父权限ID',
  `sort` int unsigned DEFAULT '0' COMMENT '排序号',
  `status` tinyint unsigned NOT NULL DEFAULT '1' COMMENT '状态: 0-禁用 1-启用',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '是否删除: 0-否 1-是',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_permission_code` (`permission_code`),
  KEY `idx_permission_type` (`permission_type`),
  KEY `idx_parent_id` (`parent_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='权限表';

-- ============================================================
-- 角色权限关联表 (user_role_permission)
-- ============================================================
CREATE TABLE IF NOT EXISTS `user_role_permission` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `role_id` bigint unsigned NOT NULL COMMENT '角色ID',
  `permission_id` bigint unsigned NOT NULL COMMENT '权限ID',
  `status` tinyint unsigned NOT NULL DEFAULT '1' COMMENT '状态: 0-禁用 1-启用',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '是否删除: 0-否 1-是',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_permission` (`role_id`,`permission_id`),
  KEY `idx_role_id` (`role_id`),
  KEY `idx_permission_id` (`permission_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色权限关联表';

-- ============================================================
-- VIP会员表 (user_vip_membership)
-- ============================================================
CREATE TABLE IF NOT EXISTS `user_vip_membership` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint unsigned NOT NULL COMMENT '用户ID',
  `vip_level` tinyint unsigned NOT NULL DEFAULT '1' COMMENT 'VIP等级: 1-普通VIP 2-高级VIP 3-至尊VIP',
  `vip_name` varchar(64) DEFAULT '普通VIP' COMMENT 'VIP名称',
  `subscribe_type` tinyint unsigned NOT NULL DEFAULT '1' COMMENT '开通方式: 1-月付 2-季付 3-年付 4-永久',
  `pay_amount` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '支付金额',
  `start_at` datetime NOT NULL COMMENT '开通时间',
  `expire_at` datetime DEFAULT NULL COMMENT '到期时间',
  `auto_renew` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '是否自动续费: 0-否 1-是',
  `status` tinyint unsigned NOT NULL DEFAULT '1' COMMENT '状态: 0-已过期 1-生效中 2-已取消',
  `source` tinyint unsigned DEFAULT '1' COMMENT '来源: 1-直接购买 2-兑换码 3-赠送 4-活动',
  `remark` varchar(512) DEFAULT NULL COMMENT '备注',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '是否删除: 0-否 1-是',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_vip_level` (`vip_level`),
  KEY `idx_status` (`status`),
  KEY `idx_expire_at` (`expire_at`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='VIP会员表';

-- ============================================================
-- 用户设置表 (user_settings)
-- ============================================================
CREATE TABLE IF NOT EXISTS `user_settings` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint unsigned NOT NULL COMMENT '用户ID',
  `theme` varchar(32) DEFAULT 'system' COMMENT '主题: light/dark/system',
  `language` varchar(32) DEFAULT 'zh-CN' COMMENT '语言: zh-CN/en-US/ja-JP',
  `timezone` varchar(64) DEFAULT 'Asia/Shanghai' COMMENT '时区',
  `default_model` varchar(64) DEFAULT NULL COMMENT '默认模型',
  `notification` json DEFAULT NULL COMMENT '通知设置',
  `privacy` json DEFAULT NULL COMMENT '隐私设置',
  `extra_config` json DEFAULT NULL COMMENT '其他设置',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '是否删除: 0-否 1-是',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_id` (`user_id`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户设置表';

-- ============================================================
-- 初始化数据
-- ============================================================

-- 初始化角色数据
INSERT INTO `user_role` (`role_code`, `role_name`, `description`, `role_type`, `sort`, `status`) VALUES
('ROLE_USER', '普通用户', '系统默认角色，普通用户权限', 1, 1, 1),
('ROLE_VIP', 'VIP用户', 'VIP用户角色，享有更多权限', 1, 2, 1),
('ROLE_ADMIN', '管理员', '系统管理员，拥有最高权限', 1, 3, 1),
('ROLE_SUPER_ADMIN', '超级管理员', '超级管理员，系统最高权限', 1, 4, 1);

-- 初始化权限数据
INSERT INTO `user_permission` (`permission_code`, `permission_name`, `description`, `permission_type`, `resource`, `method`, `sort`, `status`) VALUES
('user:view', '查看用户', '查看用户列表和详情', 3, '/api/user/**', 'GET', 1, 1),
('user:create', '创建用户', '创建新用户', 3, '/api/user/**', 'POST', 2, 1),
('user:update', '更新用户', '更新用户信息', 3, '/api/user/**', 'PUT', 3, 1),
('user:delete', '删除用户', '删除用户', 3, '/api/user/**', 'DELETE', 4, 1),
('role:manage', '角色管理', '管理角色和权限', 3, '/api/role/**', '*', 5, 1),
('quota:manage', '配额管理', '管理用户配额', 3, '/api/quota/**', '*', 6, 1),
('vip:manage', 'VIP管理', '管理VIP会员', 3, '/api/vip/**', '*', 7, 1),
('system:admin', '系统管理', '系统管理权限', 3, '/**', '*', 99, 1);

-- 初始化角色权限关联
-- 普通用户权限
INSERT INTO `user_role_permission` (`role_id`, `permission_id`)
SELECT r.id, p.id FROM user_role r, user_permission p 
WHERE r.role_code = 'ROLE_USER' AND p.permission_code IN ('user:view');

-- VIP用户权限
INSERT INTO `user_role_permission` (`role_id`, `permission_id`)
SELECT r.id, p.id FROM user_role r, user_permission p 
WHERE r.role_code = 'ROLE_VIP' AND p.permission_code IN ('user:view', 'user:update');

-- 管理员权限
INSERT INTO `user_role_permission` (`role_id`, `permission_id`)
SELECT r.id, p.id FROM user_role r, user_permission p 
WHERE r.role_code = 'ROLE_ADMIN' AND p.permission_code IN (
    'user:view', 'user:create', 'user:update', 'user:delete',
    'role:manage', 'quota:manage', 'vip:manage'
);

-- 超级管理员拥有所有权限
INSERT INTO `user_role_permission` (`role_id`, `permission_id`)
SELECT r.id, p.id FROM user_role r, user_permission p 
WHERE r.role_code = 'ROLE_SUPER_ADMIN';
