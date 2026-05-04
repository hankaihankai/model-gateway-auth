# DDD 最简版本设计文档

## 背景与目标

`model-gateway-auth` 当前采用经典 Spring 三层(Controller / Service / Mapper),目录按技术分层组织。随着业务图谱扩大(`docs/model-gateway-user-management-api.md` 中已规划认证、用户、模型、订阅、网关调用、计费、充值、配额、审计共 8 个上下文),需要先把代码组织方式切换到 DDD 限界上下文风格,为后续业务扩展打好结构基础。

本设计的目标是:**先实现最简版本的 DDD 改造**,只重构现有已工作的代码,不增加新业务,不引入战术 DDD 重型模式。

## 范围与非目标

### 范围

- 重新组织现有 `认证 / 用户 / 凭证 / new-api 绑定 / 邮件验证码` 五个功能模块的目录结构
- 按限界上下文 + 四层分层调整包路径与依赖方向
- 删除单实现的 Service 接口,简化代码

### 非目标

- 不新增任何业务功能(模型订阅、计费、充值、配额、审计 等留待后续 spec)
- 不引入仓储接口与端口适配器(轻量分层下不强制)
- 不引入富血模型(`SysUser` 等保持 `@Data` Lombok 实体)
- 不引入领域事件 / CQRS / 事件溯源
- 不调整 HTTP 接口路径与请求/响应结构
- 不引入新的测试基础设施

## 整体方案对比

| 方案 | 核心思路 | 适用场景 |
|------|----------|----------|
| **A:轻量四层 + 3 个 BC(采用)** | BC 优先包结构,内部 `interfaces / application / domain / infrastructure` 四层;贫血模型 + Service-Mapper;只调依赖方向不改业务 | 现状几乎可平滑过渡,改动量小 |
| 参照 B:扁平模块化 | 只按业务拆顶层包(identity / new-api / notification),内部不分层 | 比 A 更轻,但失去 DDD 形状 |
| 参照 C:六边形 + 端口适配器 | domain 用接口反向声明依赖,infrastructure 实现端口;聚合根富血化 | 强 DDD,但与"轻量分层"诉求冲突 |

**最终采用方案 A**。

## 1. 限界上下文与顶层包结构

3 个限界上下文 + 1 个共享内核 + 顶层应用配置。

| 包 | 类型 | 职责 |
|----|------|------|
| `identity` | 核心 BC | 用户、登录会话、网关凭证 |
| `newapi` | 防腐层 BC | 与 new-api 外部系统对接,把外部模型翻译成业务模型 |
| `notification` | 通用 BC | 邮件验证码发送(已分模块),后续可扩展 SMS |
| `shared` | 共享内核 | 跨 BC 真共享:统一响应、基础异常、通用枚举 |
| `config` | 应用级配置 | Sa-Token、Password、JWT 模板等横切配置 |

顶层包路径:

```
com.model.gateway.auth
├── identity            # 核心 BC
├── newapi              # 防腐层 BC
├── notification        # 通用 BC
├── shared              # 共享内核
├── config              # 应用级跨 BC 配置
└── ModelGatewayAuthApplication
```

`config/` 保留在顶层而非塞进 identity,因为 `SaTokenConfig`、`PasswordConfig`、`RsaSaJwtTemplate` 是应用级横切配置,放任一 BC 都不合适。

## 2. BC 内部四层结构(以 identity 为例)

每个 BC 内部按四层切包,依赖方向**自上而下,不可反向**:

```
identity/
├── interfaces/                # 1. 接口层:对外暴露,做参数校验和响应组装
│   ├── http/                  # HTTP 控制器
│   ├── dto/                   # 入参 DTO
│   └── vo/                    # 出参 VO
│
├── application/               # 2. 应用层:编排用例,事务边界
│
├── domain/                    # 3. 领域层:业务实体和领域服务,纯业务,无 Spring/MyBatis
│   ├── model/                 # 实体(贫血)
│   └── service/               # 领域服务
│
└── infrastructure/            # 4. 基础设施层:数据库、缓存、外部 SDK
    ├── persistence/
    │   └── mapper/            # MyBatis Mapper
    ├── cache/
    └── config/                # 该 BC 的私有配置
```

### 层间约束

- `interfaces` 只调 `application`,不直接调 `domain` / `infrastructure`
- `application` 调 `domain` 和 `infrastructure`(轻量分层不强制反向依赖,application 直接依赖 infrastructure 实现类)
- `domain` 不依赖任何其他层(纯 POJO + Lombok)
- `infrastructure` 可被 `application` 调用,内部含 Mapper / Cache / 外部 SDK 适配

### 与战术 DDD 的差异(本次不做)

- 不做仓储接口(`UserRepository` 在 domain 声明 + infrastructure 实现):本次直接用 Mapper
- 不做富血模型:`SysUser` 仍是 `@Data` Lombok 实体,业务方法不下沉到实体
- 不做端口/适配器反向依赖:application 直接依赖 infrastructure

`newapi` 与 `notification` BC 内部结构同构,层数视实际需要可裁剪(`notification` 没有自己的 `interfaces/http`,只有 application + domain + infrastructure)。

## 3. 现有类全量迁移映射

### 3.1 identity BC

| 当前位置 | 迁移后位置 |
|---------|-----------|
| `controller/AuthController` | `identity/interfaces/http/AuthController` |
| `controller/UserController` | `identity/interfaces/http/UserController` |
| `controller/AdminUserController` | `identity/interfaces/http/AdminUserController` |
| `controller/GatewayCredentialController` | `identity/interfaces/http/GatewayCredentialController` |
| `dto/LoginRequest` | `identity/interfaces/dto/LoginRequest` |
| `dto/UserCreateRequest` | `identity/interfaces/dto/UserCreateRequest` |
| `dto/UserAmountUpdateRequest` | `identity/interfaces/dto/UserAmountUpdateRequest` |
| `dto/GatewayCredentialEnsureRequest` | `identity/interfaces/dto/GatewayCredentialEnsureRequest` |
| `vo/LoginResponse` | `identity/interfaces/vo/LoginResponse` |
| `vo/UserCreateResponse` | `identity/interfaces/vo/UserCreateResponse` |
| `vo/UserInfoVo` | `identity/interfaces/vo/UserInfoVo` |
| `vo/UserProfileVo` | `identity/interfaces/vo/UserProfileVo` |
| `vo/UserTokenRecordsVo` | `identity/interfaces/vo/UserTokenRecordsVo` |
| `vo/GatewayCredentialResponse` | `identity/interfaces/vo/GatewayCredentialResponse` |
| `service/AuthService` + `service/impl/AuthServiceImpl` | `identity/application/AuthApplicationService`(合并接口与实现) |
| `service/UserProfileService` | `identity/application/UserProfileApplicationService` |
| `service/GatewayCredentialService` | `identity/application/GatewayCredentialApplicationService` |
| `service/CredentialCryptoService` | `identity/domain/service/CredentialCryptoService` |
| `domain/SysUser` | `identity/domain/model/SysUser` |
| `context/LoginUser` | `identity/domain/model/LoginUser` |
| `mapper/UserMapper` | `identity/infrastructure/persistence/mapper/UserMapper` |
| `service/GatewayCredentialCacheService` | `identity/infrastructure/cache/GatewayCredentialCacheService` |
| `config/GatewayCredentialProperties` | `identity/infrastructure/config/GatewayCredentialProperties` |
| `config/GatewayJwtProperties` | `identity/infrastructure/config/GatewayJwtProperties` |

> **关于 `AuthService` 接口**:轻量分层下,单实现类的接口直接去掉,`AuthServiceImpl` 改名为 `AuthApplicationService`。这是简化项目的常规做法,符合"最简版本"诉求。如果后续要保留接口供测试 mock 或多实现替换,再加。

### 3.2 newapi BC

| 当前位置 | 迁移后位置 |
|---------|-----------|
| `service/NewApiBindingService` | `newapi/application/NewApiBindingApplicationService` |
| `acl/NewApiUserAcl` | `newapi/infrastructure/external/NewApiUserAcl` |
| `domain/UserNewApiBinding` | `newapi/domain/model/UserNewApiBinding` |
| `domain/UserNewApiBindingLog` | `newapi/domain/model/UserNewApiBindingLog` |
| `mapper/UserNewApiBindingMapper` | `newapi/infrastructure/persistence/mapper/UserNewApiBindingMapper` |
| `mapper/UserNewApiBindingLogMapper` | `newapi/infrastructure/persistence/mapper/UserNewApiBindingLogMapper` |
| `config/NewApiUserManagerProperties` | `newapi/infrastructure/config/NewApiUserManagerProperties` |

`newapi` 没有自己的 `interfaces/http`(目前是被 identity 调用,无独立对外接口)。

### 3.3 notification BC

| 当前位置 | 迁移后位置 |
|---------|-----------|
| `email/EmailService`(接口) | `notification/domain/service/EmailService` |
| `email/EmailProviderType` | `notification/domain/model/EmailProviderType` |
| `email/EmailFactory` | `notification/application/EmailFactory` |
| `email/QqEmailService` | `notification/infrastructure/sender/QqEmailService` |
| `email/NeteaseEmailService` | `notification/infrastructure/sender/NeteaseEmailService` |
| `email/GenericEmailService` | `notification/infrastructure/sender/GenericEmailService` |
| `email/config/EmailProperties` | `notification/infrastructure/config/EmailProperties` |
| `email/config/EmailSenderConfig` | `notification/infrastructure/config/EmailSenderConfig` |
| `email/exception/EmailSendException` | `notification/domain/exception/EmailSendException` |

### 3.4 shared 共享内核

| 当前位置 | 迁移后位置 |
|---------|-----------|
| `common/ApiResponse` | `shared/api/ApiResponse` |
| `common/AuthConstants` | `shared/constant/AuthConstants` |
| `common/UserRoleEnum` | `shared/enums/UserRoleEnum` |
| `common/UserStatusEnum` | `shared/enums/UserStatusEnum` |
| `exception/AuthException` | `shared/exception/AuthException` |
| `exception/AuthStatusException` | `shared/exception/AuthStatusException` |
| `exception/GlobalExceptionHandler` | `shared/exception/GlobalExceptionHandler` |
| `support/SecretFileUtils` | `shared/util/SecretFileUtils` |

### 3.5 应用级配置(顶层 config 保留)

| 当前位置 | 迁移后位置 |
|---------|-----------|
| `config/SaTokenConfig` | `config/SaTokenConfig`(不动) |
| `config/SaTokenRoleConfig` | `config/SaTokenRoleConfig`(不动) |
| `config/RsaSaJwtTemplate` | `config/RsaSaJwtTemplate`(不动) |
| `config/PasswordConfig` | `config/PasswordConfig`(不动) |
| `ModelGatewayAuthApplication` | `ModelGatewayAuthApplication`(不动) |

### 3.6 删除项

- `service/AuthService` 接口删除(单实现合并到 `AuthApplicationService`)
- 其余 `service/` 下的类目前都没有独立接口文件,直接搬迁后改名

## 4. 数据流(分层调用示例)

### 4.1 用户登录(单 BC,展示分层)

```
HTTP POST /api/auth/login
  ↓
[interfaces]   AuthController.login(LoginRequest)
  ↓
[application]  AuthApplicationService.login(username, password)
                ├─→ [infrastructure]  UserMapper.findByUsername()
                ├─→ BCryptPasswordEncoder.matches(raw, hashed)  (Spring Security crypto)
                ├─→ [infrastructure]  StpUtil.login()  (Sa-Token)
                └─→ 组装 LoginResponse
  ↓
[interfaces]   返回 ApiResponse<LoginResponse>
```

层间约束:

- Controller 不直接 `@Autowired UserMapper`(违反:跨层)
- AuthApplicationService 可以直接调 UserMapper(轻量分层不强制仓储接口)
- 密码校验直接复用 Spring Security 的 `BCryptPasswordEncoder` Bean,不为本次重构新增 `PasswordDomainService` 之类的领域服务

### 4.2 注册用户 + 创建 new-api 绑定(跨 BC)

```
HTTP POST /api/user/registerUser
  ↓
[identity.interfaces]      UserController.register(UserCreateRequest)
  ↓
[identity.application]     UserProfileApplicationService.register(req)
                            ├─→ [identity.infrastructure] UserMapper.insert(SysUser)
                            └─→ [newapi.application]      NewApiBindingApplicationService
                                                           .createBindingFor(userId, username)
                                                            ├─→ [newapi.infrastructure]
                                                            │     NewApiUserAcl.createUser()
                                                            └─→ [newapi.infrastructure]
                                                                  UserNewApiBindingMapper.insert()
  ↓
[identity.interfaces]      返回 ApiResponse<UserCreateResponse>
```

跨 BC 调用规则:

- BC 之间只通过 `application` 层互相调用,**不允许跨 BC 直接调对方 mapper / domain**
- `identity.application` → `newapi.application` ✅
- `identity.application` → `newapi.infrastructure.UserNewApiBindingMapper` ❌ 禁止

### 4.3 APISIX lua 插件查询凭证(单 BC,带缓存)

```
HTTP POST /api/gateway/new-api-credential/ensure
  ↓
[identity.interfaces]      GatewayCredentialController
  ↓
[identity.application]     GatewayCredentialApplicationService
                            ├─→ [identity.infrastructure] GatewayCredentialCacheService (Redis)
                            ├─→ 缓存未命中 → [identity.infrastructure] UserMapper
                            └─→ [identity.domain] CredentialCryptoService.encrypt/decrypt
  ↓
[identity.interfaces]      返回 ApiResponse<GatewayCredentialResponse>
```

## 5. 错误处理

`shared/exception` 集中放跨 BC 的异常基类与全局处理器,各 BC 内部专有异常放本 BC `domain/exception/`(如 `notification/domain/exception/EmailSendException`)。`GlobalExceptionHandler` 留在 `shared/exception/`,统一捕获并通过 `shared/api/ApiResponse` 包装。

异常分层:

- 领域异常(继承自 `shared/exception/AuthException` 或独立)由 domain / application 抛出
- HTTP 层不再自己 try-catch 业务异常,统一交给 `GlobalExceptionHandler`

## 6. 测试

AGENTS.md 已声明"单元测试不强制"。本次重构验收标准:

- `mvn -q -DskipTests compile` 通过
- `mvn spring-boot:run` 启动成功,无 Bean 创建错误
- 手动 smoke:
  - 登录(`POST /api/auth/login`)
  - 注册(`POST /api/user/registerUser`,含 new-api 绑定)
  - 查询凭证(APISIX 调用的内部接口)
  - 发送验证码邮件(若已暴露接口,否则跳过)

不引入新的测试基础设施。

## 7. 落地策略

**一次性重构,单 commit**。理由:

- 现有代码量小(约 50 个 Java 文件)
- 全是包/路径改动,业务逻辑 0 改动
- IDE 重构功能(Move Class)能批量完成,出错概率低
- 新旧并存反而引入歧义

落地步骤:

1. 创建新包目录结构(空骨架)
2. 用 IDE Move Class 把每个类搬到新位置(自动改 import)
3. 删除空旧包
4. 删除单实现接口(`AuthService`)
5. `mvn compile` + 启动 smoke
6. 提交单个 commit:`refactor: 按 DDD 限界上下文重组代码结构`

## 8. 风险

| 风险 | 概率 | 缓解 |
|------|------|------|
| MyBatis Mapper 路径变化导致 `@MapperScan` 失效 | 中 | 重构时同步更新 `@MapperScan(basePackages = ...)` 或改用 `@Mapper` 注解逐类标注 |
| `application.yml` 中 `mybatis.mapper-locations` 指向旧路径 | 低 | 检查并同步 |
| Lombok `@Data` + Sa-Token JWT 序列化对类位置敏感 | 低 | 启动后清理 Redis 中旧的 Sa-Token 会话(空数据库部署期间无影响) |
| 顶层 `config/` 下的 Bean 跨 BC 引用包不存在导致 ComponentScan 漏扫 | 低 | 主类 `@SpringBootApplication` 默认扫包,不会漏 |
| APISIX lua 插件依赖的 HTTP 接口路径不变 | — | 路径在 `@RequestMapping`,不受类位置影响,无风险 |

## 9. 后续工作(不在本 spec 范围)

DDD 改造完成后,后续按各 BC 独立 spec 推进新功能:

- 模型管理 + 用户模型订阅(可能合并为 `model` BC,或归入 `identity`)
- 模型网关调用 + 计费(`gateway` 核心 BC + `billing` BC)
- 兑换码 + 充值记录(`recharge` BC)
- 配额管理(`quota` BC,可能与 `gateway` 合并)
- 操作审计(`audit` BC,通用域)

每个新增 BC 都应遵守本 spec 定义的四层结构与跨 BC 调用规则。
