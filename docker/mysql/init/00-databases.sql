-- 统一建库脚本（docker MySQL 首次初始化自动执行）
CREATE DATABASE IF NOT EXISTS `ai_platform`
  DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS `ai_saas_rag`
  DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
