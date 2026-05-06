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
  `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
  `email` VARCHAR(128) DEFAULT NULL COMMENT '邮箱',
  `status` INT NOT NULL DEFAULT 0 COMMENT '用户状态：0启用、1禁用、2处理中、3异常',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`user_id`),
  UNIQUE KEY `uk_sys_user_username` (`username`),
  UNIQUE KEY `uk_sys_user_phone` (`phone`),
  UNIQUE KEY `uk_sys_user_email` (`email`),
  KEY `idx_sys_user_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统用户表';

CREATE TABLE IF NOT EXISTS `sys_role` (
  `role_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '角色ID',
  `role_code` VARCHAR(64) NOT NULL COMMENT '角色编码',
  `role_name` VARCHAR(64) NOT NULL COMMENT '角色名称',
  `description` VARCHAR(255) DEFAULT NULL COMMENT '角色说明',
  `status` INT NOT NULL DEFAULT 0 COMMENT '角色状态：0启用、1禁用',
  `builtin` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否内置角色：1是、0否',
  `sort` INT NOT NULL DEFAULT 0 COMMENT '排序值',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`role_id`),
  UNIQUE KEY `uk_sys_role_code` (`role_code`),
  KEY `idx_sys_role_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统角色表';

CREATE TABLE IF NOT EXISTS `sys_user_role` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户角色关联ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `role_id` BIGINT NOT NULL COMMENT '角色ID',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sys_user_role_user_role` (`user_id`, `role_id`),
  KEY `idx_sys_user_role_role_id` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户角色关联表';

CREATE TABLE IF NOT EXISTS `sys_menu` (
  `menu_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '菜单ID',
  `parent_id` BIGINT NOT NULL DEFAULT 0 COMMENT '父级菜单ID，0表示根节点',
  `menu_type` VARCHAR(16) NOT NULL COMMENT '菜单类型：DIR目录、MENU菜单、BUTTON按钮',
  `menu_name` VARCHAR(64) NOT NULL COMMENT '菜单名称',
  `path` VARCHAR(255) DEFAULT NULL COMMENT '前端路由路径',
  `component_key` VARCHAR(128) DEFAULT NULL COMMENT '前端组件白名单Key',
  `permission_code` VARCHAR(128) DEFAULT NULL COMMENT '权限编码',
  `icon` VARCHAR(64) DEFAULT NULL COMMENT '菜单图标',
  `visible` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否可见：1可见、0隐藏',
  `status` INT NOT NULL DEFAULT 0 COMMENT '菜单状态：0启用、1禁用',
  `builtin` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否内置菜单：1是、0否',
  `sort` INT NOT NULL DEFAULT 0 COMMENT '排序值',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`menu_id`),
  KEY `idx_sys_menu_parent_id` (`parent_id`),
  KEY `idx_sys_menu_permission_code` (`permission_code`),
  KEY `idx_sys_menu_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统菜单权限表';

CREATE TABLE IF NOT EXISTS `sys_api_permission` (
  `api_permission_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'API权限ID',
  `permission_code` VARCHAR(128) NOT NULL COMMENT '权限编码',
  `permission_name` VARCHAR(64) NOT NULL COMMENT '权限名称',
  `method` VARCHAR(16) NOT NULL COMMENT 'HTTP方法',
  `path_pattern` VARCHAR(255) NOT NULL COMMENT '接口路径匹配表达式',
  `description` VARCHAR(255) DEFAULT NULL COMMENT '权限说明',
  `status` INT NOT NULL DEFAULT 0 COMMENT '权限状态：0启用、1禁用',
  `builtin` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否内置权限：1是、0否',
  `sort` INT NOT NULL DEFAULT 0 COMMENT '排序值',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`api_permission_id`),
  UNIQUE KEY `uk_sys_api_permission_code` (`permission_code`),
  KEY `idx_sys_api_permission_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统API权限表';

CREATE TABLE IF NOT EXISTS `sys_role_menu` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '角色菜单关联ID',
  `role_id` BIGINT NOT NULL COMMENT '角色ID',
  `menu_id` BIGINT NOT NULL COMMENT '菜单ID',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sys_role_menu_role_menu` (`role_id`, `menu_id`),
  KEY `idx_sys_role_menu_menu_id` (`menu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色菜单关联表';

CREATE TABLE IF NOT EXISTS `sys_role_api_permission` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '角色API权限关联ID',
  `role_id` BIGINT NOT NULL COMMENT '角色ID',
  `api_permission_id` BIGINT NOT NULL COMMENT 'API权限ID',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sys_role_api_role_permission` (`role_id`, `api_permission_id`),
  KEY `idx_sys_role_api_permission_id` (`api_permission_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色API权限关联表';

INSERT IGNORE INTO `sys_role` (`role_id`, `role_code`, `role_name`, `description`, `status`, `builtin`, `sort`) VALUES
(1, 'SUPER_ADMIN', '超级管理员', '拥有系统全部管理权限', 0, 1, 1),
(2, 'ADMIN', '管理员', '拥有用户和权限管理权限', 0, 1, 2),
(3, 'USER', '普通用户', '普通业务用户', 0, 1, 3);

INSERT IGNORE INTO `sys_menu` (`menu_id`, `parent_id`, `menu_type`, `menu_name`, `path`, `component_key`, `permission_code`, `icon`, `visible`, `status`, `builtin`, `sort`) VALUES
(1, 0, 'DIR', '用户管理', '/user-manage', NULL, 'user:view', 'UserOutlined', 1, 0, 1, 10),
(2, 1, 'MENU', '用户列表', '/user-manage/list', 'UserManageList', 'user:view', NULL, 1, 0, 1, 11),
(3, 0, 'DIR', '权限管理', '/rbac', NULL, 'role:view', 'SafetyCertificateOutlined', 1, 0, 1, 20),
(4, 3, 'MENU', '角色管理', '/rbac/roles', 'RoleManageList', 'role:view', NULL, 1, 0, 1, 21),
(5, 3, 'MENU', '菜单管理', '/rbac/menus', 'MenuManageList', 'menu:view', NULL, 1, 0, 1, 22),
(6, 3, 'MENU', 'API权限', '/rbac/api-permissions', 'ApiPermissionManageList', 'api-permission:view', NULL, 1, 0, 1, 23),
(101, 2, 'BUTTON', '用户写入', NULL, NULL, 'user:write', NULL, 0, 0, 1, 101),
(102, 2, 'BUTTON', '用户额度', NULL, NULL, 'user:amount', NULL, 0, 0, 1, 102),
(103, 2, 'BUTTON', '用户角色', NULL, NULL, 'user:role', NULL, 0, 0, 1, 103),
(201, 4, 'BUTTON', '角色写入', NULL, NULL, 'role:write', NULL, 0, 0, 1, 201),
(301, 5, 'BUTTON', '菜单写入', NULL, NULL, 'menu:write', NULL, 0, 0, 1, 301),
(401, 6, 'BUTTON', 'API权限写入', NULL, NULL, 'api-permission:write', NULL, 0, 0, 1, 401);

INSERT IGNORE INTO `sys_api_permission` (`api_permission_id`, `permission_code`, `permission_name`, `method`, `path_pattern`, `description`, `status`, `builtin`, `sort`) VALUES
(1, 'user:view', '用户查看', 'GET', '/api/admin/users/**', '查看用户列表、详情、模型和调用记录', 0, 1, 10),
(2, 'user:write', '用户写入', 'POST', '/api/admin/users/**', '创建用户、修改状态、补绑new-api', 0, 1, 11),
(3, 'user:amount', '用户额度', 'POST', '/api/admin/users/*/amount', '调整用户额度', 0, 1, 12),
(4, 'user:role', '用户角色', 'PUT', '/api/admin/users/*/roles', '分配用户角色', 0, 1, 13),
(5, 'role:view', '角色查看', 'GET', '/api/admin/rbac/roles/**', '查看角色和授权', 0, 1, 20),
(6, 'role:write', '角色写入', '*', '/api/admin/rbac/roles/**', '创建、更新、删除角色和授权', 0, 1, 21),
(7, 'menu:view', '菜单查看', 'GET', '/api/admin/rbac/menus/**', '查看菜单', 0, 1, 30),
(8, 'menu:write', '菜单写入', '*', '/api/admin/rbac/menus/**', '创建、更新、删除菜单', 0, 1, 31),
(9, 'api-permission:view', 'API权限查看', 'GET', '/api/admin/rbac/api-permissions/**', '查看API权限', 0, 1, 40),
(10, 'api-permission:write', 'API权限写入', '*', '/api/admin/rbac/api-permissions/**', '创建、更新、删除API权限', 0, 1, 41);

INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.role_id, m.menu_id
FROM `sys_role` r
JOIN `sys_menu` m ON m.builtin = 1
WHERE r.role_code IN ('SUPER_ADMIN', 'ADMIN');

INSERT IGNORE INTO `sys_role_api_permission` (`role_id`, `api_permission_id`)
SELECT r.role_id, ap.api_permission_id
FROM `sys_role` r
JOIN `sys_api_permission` ap ON ap.builtin = 1
WHERE r.role_code IN ('SUPER_ADMIN', 'ADMIN');

INSERT IGNORE INTO `sys_user` (`user_id`, `username`, `password`, `nickname`, `phone`, `email`, `status`, `create_time`, `update_time`) VALUES
(1, 'hankai', '$2a$10$6l7EaZlkk95XqBzdGA2ZMOTGyYjONe2FMGbvel85mH3bdypaQUnS6', 'hankai', NULL, NULL, 0, '2026-04-28 08:54:16', '2026-05-01 14:57:54'),
(2, 'testuser', '$2a$10$6l7EaZlkk95XqBzdGA2ZMOTGyYjONe2FMGbvel85mH3bdypaQUnS6', 'zhangshao', NULL, NULL, 0, '2026-04-28 08:54:16', '2026-05-01 14:38:44'),
(3, 'admin', '$2a$10$HUVamuMiTEq8YmmcF8JwJucjkJaFv6qut/Q33WHJXlCs7e7sIXFaG', '管理员', '15298987890', 'test@qq.com', 0, '2026-05-01 16:40:34', '2026-05-01 16:40:34');

INSERT IGNORE INTO `sys_user_role` (`user_id`, `role_id`)
SELECT u.user_id, r.role_id
FROM (
  SELECT 'admin' AS username, 'SUPER_ADMIN' AS role_code
  UNION ALL SELECT 'hankai', 'ADMIN'
  UNION ALL SELECT 'testuser', 'USER'
) ur
JOIN `sys_user` u ON u.username = ur.username
JOIN `sys_role` r ON r.role_code = ur.role_code;

CREATE TABLE IF NOT EXISTS `user_new_api_binding` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '绑定ID',
  `user_id` BIGINT NOT NULL COMMENT '业务系统用户ID，对应sys_user.user_id',
  `new_api_user_id` BIGINT DEFAULT NULL COMMENT 'new-api用户ID',
  `new_api_user_name` VARCHAR(128) DEFAULT NULL COMMENT 'new-api用户名称',
  `new_api_api_key` TEXT DEFAULT NULL COMMENT '完整明文new-api API Key，格式为sk-xxx',
  `status` INT NOT NULL DEFAULT 2 COMMENT '绑定状态：0启用、1禁用、2处理中、3异常',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_new_api_binding_user_id` (`user_id`),
  KEY `idx_user_new_api_binding_status` (`status`),
  KEY `idx_user_new_api_binding_new_api_user_id` (`new_api_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='业务用户与new-api用户令牌绑定表';

-- 手工维护示例：先用sk_xxx占位，后续替换成真实new-api API Key。
-- INSERT INTO `user_new_api_binding` (`user_id`, `new_api_user_id`, `new_api_user_name`, `new_api_api_key`, `status`)
-- VALUES (1, 1, 'newapi_user', 'sk_xxx', 0);

INSERT IGNORE INTO `user_new_api_binding` (`id`, `user_id`, `new_api_user_id`, `new_api_user_name`, `new_api_api_key`, `status`, `create_time`, `update_time`) VALUES
(1, 1, 1, 'hankai', 'sk-CtAawcMde6GpUM0f73Qh7egtPBUX7qBp1mZdItroEY1XJCza', 0, '2026-04-28 09:05:42', '2026-05-01 14:38:44'),
(2, 2, 2, 'testuser', 'sk-6YoDGkWFyIi9WFT7mINQirqVEpV7JEsbZPnip1I9U49rPHsQ', 0, '2026-04-30 14:37:17', '2026-05-01 14:38:44'),
(3, 3, 6, 'admin_3_ytqog3', 'sk-EuMAnCTu2TpGABv8iSevMnX470Cf4Nqzj2Dwdn9x2o3eoFwR', 0, '2026-05-01 16:40:34', '2026-05-01 16:40:34');

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
