# 应用化权限与 APISIX 应用鉴权实施计划

> **给执行代理的要求：** 按任务逐项执行；如果在同一会话中执行，优先使用 `superpowers:executing-plans`。步骤使用复选框语法便于跟踪。

**目标：** 在现有系统权限基础上加入 `sys_app` 应用表，让应用提供权限分组和固定权限前缀，并让 APISIX 按路由配置的 `app_code` 调用认证服务完成应用 API 权限校验。

**架构：** Java 认证服务仍是用户、角色、API 权限和 new-api 凭证的权威来源。`sys_app` 只负责权限归属和权限编码前缀，不直接绑定用户。APISIX 先校验 JWT，再用 `app_code + method + 内部路径` 调认证服务鉴权，通过后继续复用现有 new-api 凭证注入逻辑并转发到上游。

**技术栈：** Spring Boot 3.3、Java 21、MyBatis Plus、MySQL、Sa-Token、APISIX Lua 插件。

---

## 任务 1：新增应用数据模型

**文件：**
- 新建：`src/main/java/com/model/gateway/auth/system/domain/model/SysApp.java`
- 新建：`src/main/java/com/model/gateway/auth/system/infrastructure/persistence/mapper/SysAppMapper.java`
- 修改：`src/main/java/com/model/gateway/auth/system/domain/model/SysApiPermission.java`

- [ ] 新建 `SysApp` 实体，使用 Lombok `@Data`、`@Builder`、`@AllArgsConstructor`、`@NoArgsConstructor`，并添加 `@TableName("sys_app")`。
- [ ] `SysApp` 字段为 `appId`、`appCode`、`appName`、`description`、`status`、`builtin`、`sort`，每个字段都加简洁中文注释。
- [ ] `appId` 使用 `@TableId(value = "app_id", type = IdType.AUTO)`。
- [ ] 新建 `SysAppMapper extends BaseMapper<SysApp>`，类和方法保持中文注释。
- [ ] 在 `SysApiPermission` 增加 `appId` 字段并加中文注释。
- [ ] 保持 `pathPattern` 语义为应用内部路径匹配表达式。

## 任务 2：更新数据库脚本与种子数据

**文件：**
- 修改：`docs/database.sql`

- [ ] 在 `sys_api_permission` 建表前新增 `sys_app` 表。
- [ ] `sys_app` 字段为 `app_id`、`app_code`、`app_name`、`description`、`status`、`builtin`、`sort`、`create_time`、`update_time`。
- [ ] 新增内置应用种子：
  - `system`：后台系统。
  - `other`：其他应用。
- [ ] 给 `sys_api_permission` 增加 `app_id` 字段。
- [ ] 将 `sys_api_permission` 唯一约束调整为按应用隔离，例如 `(app_id, permission_code)`。
- [ ] 后台系统 API 权限编码全部改为 `system:*`，例如 `system:user:view`、`system:role:write`。
- [ ] 后台菜单 `sys_menu.permission_code` 同步改为 `system:*`。
- [ ] 新增 `other:chat:completions` 权限，`method` 为 `POST`，`path_pattern` 为 `/v1/chat/completions`。
- [ ] 角色 API 权限种子继续把内置 API 权限授予 `SUPER_ADMIN` 和 `ADMIN`。

## 任务 3：更新后端权限常量

**文件：**
- 修改：`src/main/java/com/model/gateway/auth/system/shared/RbacPermissionConstants.java`

- [ ] 将现有后台权限常量值改为 `system:*`。
- [ ] 保持常量名不变，避免控制器大范围改名。
- [ ] 如果 Java 代码需要直接引用聊天权限，再新增 `OTHER_CHAT_COMPLETIONS = "other:chat:completions"`；否则该权限只作为数据库和 APISIX 鉴权数据存在。

## 任务 4：改造 API 权限管理

**文件：**
- 修改：`src/main/java/com/model/gateway/auth/system/interfaces/dto/ApiPermissionSaveRequest.java`
- 修改：`src/main/java/com/model/gateway/auth/system/application/RbacApplicationService.java`
- 修改：`src/main/java/com/model/gateway/auth/system/infrastructure/persistence/mapper/SysApiPermissionMapper.java`

- [ ] 在 `ApiPermissionSaveRequest` 增加 `appId` 字段并添加中文注释。
- [ ] 在 `RbacApplicationService` 注入 `SysAppMapper`。
- [ ] 创建和更新 API 权限时要求 `appId` 非空。
- [ ] 根据 `appId` 查询启用应用；不存在或禁用时抛出 `AuthException("应用不存在或已禁用")`。
- [ ] 校验 `permissionCode` 必须以 `appCode + ":"` 开头，前缀不一致时抛出 `AuthException("权限编码必须以应用编码为前缀")`。
- [ ] 保存 API 权限时写入 `appId`。
- [ ] API 权限列表查询按应用排序、权限排序、权限 ID 排序；实现上可用 MyBatis Plus 查询或保留注解 SQL。
- [ ] 角色授权仍使用 API 权限 ID，不改变前端授权请求结构。

## 任务 5：新增网关应用权限鉴权接口

**文件：**
- 新建：`src/main/java/com/model/gateway/auth/identity/interfaces/dto/GatewayAppPermissionAuthorizeRequest.java`
- 新建：`src/main/java/com/model/gateway/auth/identity/interfaces/vo/GatewayAppPermissionAuthorizeResponse.java`
- 修改：`src/main/java/com/model/gateway/auth/identity/interfaces/http/GatewayCredentialController.java`
- 修改：`src/main/java/com/model/gateway/auth/identity/application/GatewayCredentialApplicationService.java`

- [ ] 新建请求 DTO，字段为 `userId`、`appCode`、`method`、`path`、`requestId`，字段加中文注释。
- [ ] 新建响应 VO，字段为 `allowed`、`permissionCode`，字段加中文注释。
- [ ] 在 `GatewayCredentialController` 新增 `POST /api/gateway/app-permission/authorize`。
- [ ] 新接口复用 `X-Gateway-Secret` 和 `Authorization` 请求头。
- [ ] 在 `GatewayCredentialApplicationService` 增加应用权限鉴权方法。
- [ ] 鉴权时先校验网关密钥，再校验 Bearer Token，确保 token 用户 ID 与请求 `userId` 一致。
- [ ] 校验用户存在且状态启用。
- [ ] 校验应用存在且状态启用。
- [ ] 根据用户角色查询启用 API 权限，限定应用、HTTP method 和内部路径。
- [ ] `method = "*"` 匹配所有方法；其他 method 大小写不敏感。
- [ ] 无匹配权限时抛出 403；匹配成功时返回 `allowed=true` 和命中的 `permissionCode`。

## 任务 6：扩展 APISIX 插件

**文件：**
- 修改：`apisix/apisix_plugins/model-gateway-auth.lua`

- [ ] 在插件 schema 增加 `app_code`。
- [ ] 在插件 schema 增加 `permission_authorize_url`。
- [ ] 在插件 schema 增加可选 `permission_path_prefix_to_strip`，用于把外部路径转换成应用内部路径。
- [ ] 在现有凭证注入前调用 Java 应用权限鉴权接口。
- [ ] 请求体传入 `userId`、`appCode`、`method`、`path`、`requestId`。
- [ ] `/v1/chat/completions` 这类固定应用路由不需要路径前缀转换，直接用当前 URI 作为鉴权路径。
- [ ] Java 返回 401 或 403 时，APISIX 原样返回对应状态。
- [ ] 保留现有 Redis 凭证读取、凭证补齐、AES 解密和 `Authorization` 头替换逻辑。

## 任务 7：更新 APISIX 路由配置

**文件：**
- 修改：`apisix/docker-compose.yml`

- [ ] 增加环境变量 `APISIX_PERMISSION_AUTHORIZE_URL`，默认值为 `http://host.docker.internal:8188/api/gateway/app-permission/authorize`。
- [ ] 在 `/v1/chat/completions` 路由的 `model-gateway-auth` 插件配置中增加 `app_code = "other"`。
- [ ] 在同一插件配置中增加 `permission_authorize_url`。
- [ ] 后台认证服务路由 `/model-gateway-auth/*` 仍然不挂 `model-gateway-auth` 插件，避免登录和后台接口被 APISIX 插件拦截。

## 任务 8：验证

- [ ] 运行后端编译：

```bash
mvn -q -DskipTests compile
```

- [ ] 在 `apisix/` 目录校验 APISIX Compose 配置：

```bash
docker compose --env-file .env config
```

- [ ] 搜索旧后台短权限编码，确认 Java 常量和数据库种子已切到 `system:*`：

```bash
rg -n "user:view|role:view|menu:view|api-permission:view" src/main/java docs/database.sql
```

- [ ] 手工验证后台登录后权限上下文返回 `system:*` 权限。
- [ ] 手工验证后台管理接口仍通过 Sa-Token 权限注解鉴权。
- [ ] 手工验证 APISIX `/v1/chat/completions` 路由配置为 `app_code = "other"`。
- [ ] 用户拥有 `other:chat:completions` 时，APISIX 请求通过并转发到 new-api。
- [ ] 用户没有对应权限、应用禁用、权限禁用、method/path 不匹配时返回 403。
- [ ] JWT 无效、过期或登出后返回 401。
