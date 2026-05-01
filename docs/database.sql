-- model-gateway-auth 数据库交付脚本
-- 说明：
-- 1. 本项目维护业务登录用户和new-api绑定关系。
-- 2. 注册用户时会通过外部用户管理接口创建new-api用户和默认Token。
-- 3. user_new_api_binding 使用PENDING/ENABLE/DISABLE/ERROR状态记录绑定生命周期。
-- 4. user_new_api_binding.new_api_api_key 存完整明文sk-xxx，仅供Java服务读取后加密写入Redis。

CREATE TABLE IF NOT EXISTS `sys_user` (
  `user_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username` VARCHAR(64) NOT NULL COMMENT '用户名',
  `password` VARCHAR(100) NOT NULL COMMENT 'BCrypt加密密码',
  `nickname` VARCHAR(64) DEFAULT NULL COMMENT '用户昵称',
  `role` VARCHAR(32) NOT NULL COMMENT '用户角色：USER或ADMIN',
  `status` VARCHAR(32) NOT NULL COMMENT '用户状态：ENABLE或DISABLE',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`user_id`),
  UNIQUE KEY `uk_sys_user_username` (`username`),
  KEY `idx_sys_user_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统用户表';

CREATE TABLE IF NOT EXISTS `user_new_api_binding` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '绑定ID',
  `user_id` BIGINT NOT NULL COMMENT '业务系统用户ID，对应sys_user.user_id',
  `new_api_user_id` BIGINT DEFAULT NULL COMMENT 'new-api用户ID',
  `new_api_user_name` VARCHAR(128) DEFAULT NULL COMMENT 'new-api用户名称',
  `new_api_api_key` TEXT DEFAULT NULL COMMENT '完整明文new-api API Key，格式为sk-xxx',
  `status` VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT '绑定状态：PENDING、ENABLE、DISABLE、ERROR',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_new_api_binding_user_id` (`user_id`),
  KEY `idx_user_new_api_binding_status` (`status`),
  KEY `idx_user_new_api_binding_new_api_user_id` (`new_api_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='业务用户与new-api用户令牌绑定表';

-- 手工维护示例：先用sk_xxx占位，后续替换成真实new-api API Key。
-- INSERT INTO `user_new_api_binding` (`user_id`, `new_api_user_id`, `new_api_user_name`, `new_api_api_key`, `status`)
-- VALUES (1, 1, 'newapi_user', 'sk_xxx', 'ENABLE');

CREATE TABLE IF NOT EXISTS `user_new_api_binding_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `user_id` BIGINT NOT NULL COMMENT '业务系统用户ID',
  `binding_id` BIGINT DEFAULT NULL COMMENT '绑定ID',
  `operate_type` VARCHAR(64) NOT NULL COMMENT '操作类型：CREATE_BINDING、UPDATE_QUOTA、SYNC_FAILED',
  `success` TINYINT(1) NOT NULL COMMENT '是否成功：1成功，0失败',
  `message` VARCHAR(1024) DEFAULT NULL COMMENT '操作说明或失败原因',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_binding_log_user_id` (`user_id`),
  KEY `idx_binding_log_binding_id` (`binding_id`),
  KEY `idx_binding_log_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='new-api绑定操作日志表';
