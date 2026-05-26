# 应用权限 Redis 快照缓存优化实施计划

> **给执行代理的要求：** 用户确认后再执行本计划；执行时优先使用 `superpowers:executing-plans`。步骤使用复选框语法便于跟踪。

**目标：** 将 APISIX 每次请求同步调用认证服务 `/app-permission/authorize` 的模式，优化为优先读取 Redis 权限快照并在 APISIX Lua 插件内完成本地匹配。

**架构：** 认证服务仍是用户、角色、应用和 API 权限的权威来源，并负责在快照缺失时生成用户应用权限快照。APISIX 每次请求仍验证 JWT，并通过 Redis 校验 Sa-Token `last-active`，保证登出立即失效；权限快照命中时不再回源调用认证服务。权限、角色或用户角色变更后删除相关快照，下一次请求自动重建。

**技术栈：** Spring Boot 3.3、Java 21、MyBatis Plus、Redis、Sa-Token、APISIX Lua 插件。

---

## 任务 1：新增权限快照数据结构

**文件：**
- 新建：`src/main/java/com/model/gateway/auth/identity/interfaces/dto/GatewayAppPermissionSnapshotEnsureRequest.java`
- 新建：`src/main/java/com/model/gateway/auth/identity/interfaces/vo/GatewayAppPermissionSnapshotResponse.java`
- 新建：`src/main/java/com/model/gateway/auth/identity/interfaces/vo/GatewayAppPermissionItemVo.java`

- [ ] 新建快照重建请求 DTO，字段为 `userId`、`appCode`、`requestId`，类、字段加中文注释。
- [ ] 新建快照响应 VO，字段为 `userId`、`appCode`、`permissions`，类、字段加中文注释。
- [ ] 新建权限明细 VO，字段为 `permissionCode`、`method`、`pathPattern`，类、字段加中文注释。
- [ ] 所有新增 VO/DTO 使用 Lombok `@Data`、`@Builder`、`@AllArgsConstructor`、`@NoArgsConstructor`。

## 任务 2：扩展 Redis 缓存服务

**文件：**
- 修改：`src/main/java/com/model/gateway/auth/identity/infrastructure/cache/GatewayCredentialCacheService.java`

- [ ] 增加权限快照 Key 前缀：`gateway:app-permission:snapshot:`。
- [ ] 增加写入快照方法：按 `userId + appCode` 写入 `GatewayAppPermissionSnapshotResponse` JSON，并设置 TTL。
- [ ] 增加读取快照方法：按 `userId + appCode` 读取 JSON 并反序列化。
- [ ] 增加删除单用户全部快照方法：使用 Redis SCAN 匹配 `gateway:app-permission:snapshot:{userId}:*`，不使用 `KEYS`。
- [ ] 增加删除全部权限快照方法：使用 Redis SCAN 匹配 `gateway:app-permission:snapshot:*`，不使用 `KEYS`。
- [ ] 序列化或反序列化失败时抛出 `AuthException`，错误信息使用中文。

## 任务 3：新增快照 TTL 配置

**文件：**
- 修改：`src/main/java/com/model/gateway/auth/identity/infrastructure/config/GatewayCredentialProperties.java`
- 修改：`src/main/resources/application.yml`

- [ ] 在 `GatewayCredentialProperties` 增加 `permissionSnapshotTtlSeconds` 字段并加中文注释。
- [ ] 在 `application.yml` 的 `gateway.credential` 下增加 `permission-snapshot-ttl-seconds: ${GATEWAY_PERMISSION_SNAPSHOT_TTL_SECONDS:7200}`。
- [ ] 默认 TTL 与当前 Sa-Token `active-timeout` 保持一致，默认值为 7200 秒。

## 任务 4：后端提供快照重建接口

**文件：**
- 修改：`src/main/java/com/model/gateway/auth/identity/application/GatewayCredentialApplicationService.java`
- 修改：`src/main/java/com/model/gateway/auth/identity/interfaces/http/GatewayCredentialController.java`
- 修改：`src/main/java/com/model/gateway/auth/system/application/RbacApplicationService.java`

- [ ] 在 `RbacApplicationService` 增加 `listEnabledApiPermissionsByUserAndApp(Long userId, String appCode)` 方法，复用现有 `SysApiPermissionMapper.selectEnabledByUserIdAndAppCode`。
- [ ] 在 `GatewayCredentialApplicationService` 增加 `ensureAppPermissionSnapshot(...)` 方法。
- [ ] 快照重建前复用现有网关密钥、Bearer Token、last-active、token 用户一致性、用户启用状态校验。
- [ ] 校验 `appCode` 非空；为空返回 400。
- [ ] 查询用户在应用下的启用权限，转换成 `GatewayAppPermissionItemVo` 列表。
- [ ] 写入 Redis 快照并返回 `GatewayAppPermissionSnapshotResponse`。
- [ ] 在 `GatewayCredentialController` 新增 `POST /api/gateway/app-permission/snapshot/ensure`。
- [ ] 保留现有 `/app-permission/authorize` 接口，作为兼容接口，不在本次删除。

## 任务 5：权限变更后立即删除快照

**文件：**
- 修改：`src/main/java/com/model/gateway/auth/identity/application/AuthApplicationService.java`
- 修改：`src/main/java/com/model/gateway/auth/system/application/RbacApplicationService.java`

- [ ] 用户登出时删除该用户所有权限快照。
- [ ] `updateUserRoles` 成功后删除该用户所有权限快照。
- [ ] `updateRoleGrant` 成功后删除全部权限快照。
- [ ] `deleteRole` 成功后删除全部权限快照。
- [ ] `createApiPermission`、`updateApiPermission`、`deleteApiPermission` 成功后删除全部权限快照。
- [ ] 如果后续新增应用禁用/启用功能，也必须删除全部权限快照；本计划不新增应用管理写接口。

## 任务 6：APISIX 插件改为 Redis 快照鉴权

**文件：**
- 修改：`apisix/apisix_plugins/model-gateway-auth.lua`

- [ ] 插件 schema 新增 `permission_snapshot_ensure_url`，替代运行时主路径上的 `permission_authorize_url`。
- [ ] 插件 schema 新增 `permission_cache_key_prefix`，默认 `gateway:app-permission:snapshot:`。
- [ ] 插件 schema 新增 `token_alive_key_prefix`，默认 `Authorization:login:last-active:`。
- [ ] 保留 `permission_authorize_url` 为可选兼容配置，但主逻辑不再调用它。
- [ ] JWT 校验成功后，先从 Redis 读取 `token_alive_key_prefix .. token`，不存在时返回 401。
- [ ] 从 Redis 读取 `permission_cache_key_prefix .. user_id .. ":" .. app_code`。
- [ ] 快照命中时解析 JSON，在 Lua 内按 `method + path` 匹配权限。
- [ ] 快照缺失时调用 `permission_snapshot_ensure_url`，请求体传 `userId`、`appCode`、`requestId`。
- [ ] 快照重建成功后继续用返回的权限列表在 Lua 内匹配。
- [ ] 匹配失败返回 403；Redis 错误返回 503；快照重建接口返回 401/403 时原样返回。
- [ ] Lua 路径匹配支持精确匹配、`*` 单段匹配、`**` 多段匹配，满足当前 `/v1/chat/completions` 和后台 Ant 风格路径表达式。
- [ ] 保留现有 new-api 凭证 Redis 读取、凭证补齐、AES 解密和 `Authorization` 头替换逻辑。

## 任务 7：更新 APISIX 独立 Compose 配置

**文件：**
- 修改：`apisix/docker-compose.yml`

- [ ] 增加环境变量 `APISIX_PERMISSION_SNAPSHOT_ENSURE_URL`，默认值为 `http://host.docker.internal:8188/api/gateway/app-permission/snapshot/ensure`。
- [ ] 在 `/v1/chat/completions` 路由插件配置中增加 `permission_snapshot_ensure_url`。
- [ ] 在同一插件配置中保留 `app_code = "other"`。
- [ ] 不把 APISIX 配置移动到项目根目录，继续保持 APISIX 独立部署目录结构。

## 任务 8：验证

- [ ] 使用 JDK 21 运行后端编译：

```bash
mvn -q -DskipTests clean compile
```

- [ ] 如果当前机器仍是 JDK 17，可临时验证代码语法：

```bash
mvn -q -DskipTests '-Djava.version=17' clean compile
```

- [ ] 在 `apisix/` 目录校验 Compose 配置：

```bash
docker compose --env-file .env config
```

- [ ] 手工验证首次请求快照缺失时，APISIX 调用 `/app-permission/snapshot/ensure` 并放行有权限用户。
- [ ] 手工验证第二次相同用户请求命中 Redis 快照，不再调用旧的 `/app-permission/authorize`。
- [ ] 手工验证用户登出后，即使权限快照仍存在，也因 last-active 缺失返回 401。
- [ ] 手工验证移除用户角色或角色 API 权限后，相关快照被删除，下一次请求重建后返回 403。
- [ ] 手工验证 auth 服务不可用且快照缺失时返回 503，不放行请求。

