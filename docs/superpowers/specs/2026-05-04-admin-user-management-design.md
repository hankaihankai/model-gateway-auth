# Admin 用户管理后台设计

## 背景

前端 `UserManage/List` 已基于 mock 数据完成列表展示,后端 `AdminUserController` 仅提供额度调整接口。需要将后端补齐为完整的 admin API,让前端管理后台真正可用。

## 目标

1. 管理员可分页查询用户列表(支持 username/role/status 过滤)
2. 管理员可创建用户,并可选是否同步绑定 new-api 账号
3. 管理员可查看任意用户的详情:基础信息 + new-api 绑定状态 + 额度概览
4. 管理员可查看任意用户的 AI 调用记录(Token 使用记录)

## 非目标

- 修改密码/重置密码
- 禁用/启用用户的管理按钮(本期只读)
- 手动补绑 new-api(本期只展示未绑状态,按钮留接口位)
- 修改用户基础信息(本期只读)

## 后端接口设计

### 保留已有接口

- `POST /api/admin/users/{userId}/amount` — 管理员调整额度(行为不变)

### 新增接口

#### 1. 用户列表

```
GET /api/admin/users
```

**Query 参数:**
- `pageNo` (int, default=1)
- `pageSize` (int, default=10)
- `username` (string, optional, 模糊匹配)
- `role` (string, optional, ADMIN/USER)
- `status` (int, optional, 0/1/2/3)

**Response:** `ApiResponse<AdminUserListPageVo>`

```java
@Data
@Builder
public class AdminUserListPageVo {
    private List<AdminUserListItemVo> list;
    private long total;
    private int pageNo;
    private int pageSize;
}

@Data
@Builder
public class AdminUserListItemVo {
    private Long userId;
    private String username;
    private String nickname;
    private String phone;
    private String email;
    private String role;
    private Integer status;
    private Boolean newApiBound; // newApiUserId != null
}
```

#### 2. 创建用户(管理员)

```
POST /api/admin/users
```

**Request Body:** `AdminUserCreateRequest`

```java
@Data
@Builder
public class AdminUserCreateRequest {
    private String username;
    private String password;
    private String nickname;
    private String phone;
    private String email;
    private Boolean bindNewApi; // default true
}
```

**Response:** `ApiResponse<UserCreateResponse>`(复用已有)

#### 3. 用户详情

```
GET /api/admin/users/{userId}
```

**Response:** `ApiResponse<AdminUserDetailVo>`

```java
@Data
@Builder
public class AdminUserDetailVo {
    // 本地用户信息
    private Long userId;
    private String username;
    private String nickname;
    private String phone;
    private String email;
    private String role;
    private Integer status;

    // new-api 绑定信息
    private Boolean newApiBound;
    private Long newApiUserId;
    private String newApiUserName;
    private Integer newApiStatus;

    // 额度概览(来自 new-api)
    private BigDecimal currentBalanceAmount;
    private BigDecimal usedQuotaAmount;
    private BigDecimal totalQuotaAmount;
    private Long quota;
    private Long usedQuota;
    private Long totalQuota;
    private Long quotaPerUnit;
}
```

#### 4. AI 调用记录

```
GET /api/admin/users/{userId}/token-records
```

**Query 参数:**
- `pageNo` (int, default=1)
- `pageSize` (int, default=10)
- `startTime` (string, optional, yyyy-MM-dd HH:mm:ss)
- `endTime` (string, optional, yyyy-MM-dd HH:mm:ss)
- `modelName` (string, optional)

**Response:** `ApiResponse<UserTokenRecordsVo>`(复用已有)

### 状态语义

后端返回原始状态码,前端调整枚举以匹配后端:

| 后端值 | 语义 | 前端显示 |
|--------|------|----------|
| 0 | ENABLE | 启用 |
| 1 | DISABLE | 禁用 |
| 2 | PENDING | 处理中 |
| 3 | ERROR | 异常 |

## 后端代码变更

### Controller — `AdminUserController`

注入 `UserProfileApplicationService`,新增 4 个端点:

```java
@GetMapping
public ApiResponse<AdminUserListPageVo> listUsers(AdminUserListQueryRequest query)

@PostMapping
public ApiResponse<UserCreateResponse> createUser(@RequestBody AdminUserCreateRequest request)

@GetMapping("/{userId}")
public ApiResponse<AdminUserDetailVo> getUserDetail(@PathVariable Long userId)

@GetMapping("/{userId}/token-records")
public ApiResponse<UserTokenRecordsVo> getTokenRecords(
    @PathVariable Long userId,
    @RequestParam(value = "pageNo", required = false) Integer pageNo,
    @RequestParam(value = "pageSize", required = false) Integer pageSize,
    @RequestParam(value = "startTime", required = false) String startTime,
    @RequestParam(value = "endTime", required = false) String endTime,
    @RequestParam(value = "modelName", required = false) String modelName)
```

### Service — `UserProfileApplicationService`

新增 4 个 `admin` 前缀方法:

- `adminListUsers(AdminUserListQueryRequest)` → `AdminUserListPageVo`
- `adminCreateUser(AdminUserCreateRequest)` → `UserCreateResponse`
- `adminGetUserDetail(Long userId)` → `AdminUserDetailVo`
- `adminGetTokenRecords(Long userId, Integer pageNo, ...)` → `UserTokenRecordsVo`

#### 注册逻辑重构

现有 `createUser(UserCreateRequest)` 保持行为不变(公开注册,强制绑定 new-api)。

把注册流程拆成两个内部步骤:

1. `createPendingUserInternal(UserCreateRequest request, boolean bindNewApi)`
   - 建本地 `sys_user`
   - 若 `bindNewApi=false`,本地用户 `status = DISABLE`(1),binding 行 `status = PENDING`(2)
   - 若 `bindNewApi=true`,与现有行为一致(本地 `status = DISABLE`,binding `status = PENDING`,后续绑定成功后改为 `ENABLE`)
2. `bindNewApiUserIfNeeded(RegisterContext context, UserCreateRequest request, boolean bindNewApi)`
   - 仅在 `bindNewApi=true` 时执行

`adminCreateUser` 和 `createUser` 共用同一套内部逻辑,仅 `bindNewApi` 参数不同。

### Mapper — `UserMapper`

新增条件查询方法,使用 MyBatis 注解 + `<script>` 实现动态 SQL(项目尚无 XML mapper,保持风格一致):

```java
@Select("""
    <script>
    SELECT COUNT(*) FROM sys_user
    WHERE 1 = 1
    <if test="username != null and username != ''">
      AND username LIKE CONCAT('%', #{username}, '%')
    </if>
    <if test="role != null and role != ''">
      AND role = #{role}
    </if>
    <if test="status != null">
      AND status = #{status}
    </if>
    </script>
    """)
long selectCountByCondition(@Param("username") String username,
                            @Param("role") String role,
                            @Param("status") Integer status);

@Select("""
    <script>
    SELECT
      user_id AS userId,
      username,
      password,
      nickname,
      phone,
      email,
      role,
      status
    FROM sys_user
    WHERE 1 = 1
    <if test="username != null and username != ''">
      AND username LIKE CONCAT('%', #{username}, '%')
    </if>
    <if test="role != null and role != ''">
      AND role = #{role}
    </if>
    <if test="status != null">
      AND status = #{status}
    </if>
    ORDER BY user_id DESC
    LIMIT #{limit} OFFSET #{offset}
    </script>
    """)
List<SysUser> selectListByCondition(@Param("username") String username,
                                    @Param("role") String role,
                                    @Param("status") Integer status,
                                    @Param("offset") int offset,
                                    @Param("limit") int limit);
```

### DTO / VO 新增

位于 `identity/interfaces/dto/` 和 `identity/interfaces/vo/`:

- `AdminUserCreateRequest`
- `AdminUserListQueryRequest`(内部查询参数,可放在 application 层或 interfaces 层)
- `AdminUserListPageVo`
- `AdminUserListItemVo`
- `AdminUserDetailVo`

## 前端变更

### 列表页 `pages/UserManage/List/index.tsx`

- 数据层:对接真实 `GET /api/admin/users`,移除对 mock 的依赖
- 列调整:
  - 交换 `STATUS_VALUE_ENUM` 语义为 `0:启用 / 1:禁用 / 2:处理中 / 3:异常`
  - 新增「newapi 绑定状态」列(从后端 `newApiBound` boolean 渲染「已绑/未绑」Tag)
- 交互调整:
  - 顶部工具栏新增「添加用户」按钮 → 打开创建用户 Modal
  - 操作列「查看」改为 `<Link to={`/user/detail/${record.userId}`}>查看</Link>`

### 创建用户弹窗

- 以 Modal + Form 形式挂在 List 页内(不需要独立路由)
- 字段:用户名、密码、昵称、手机号、邮箱、`bindNewApi`(Switch,默认开)
- 提交 `POST /api/admin/users`,成功后刷新 ProTable

### 新增详情页 `pages/UserManage/Detail/index.tsx`

- 路由 `/user/detail/:userId`,需 `access: 'canAdmin'`
- 页面结构:`PageContainer` + `Tabs`:
  - **Tab 1: 用户信息** — 展示 `username/nickname/phone/email/role/status/newApiUserId/newApiUserName`
  - **Tab 2: 额度概览** — 展示 `currentBalanceAmount/usedQuotaAmount/totalQuotaAmount/quota/usedQuota/totalQuota/quotaPerUnit`
  - **Tab 3: AI 调用记录** — `ProTable` 请求 `GET /api/admin/users/{userId}/token-records`,支持时间范围 + 模型名称过滤

### 服务层 `services/user.ts`

- `listUsers` 保留,确认路径为 `GET /api/admin/users`
- 新增 `createUser(body)` → `POST /api/admin/users`
- 新增 `getUserDetail(userId)` → `GET /api/admin/users/{userId}`
- 新增 `getUserTokenRecords(userId, params)` → `GET /api/admin/users/{userId}/token-records`

### Mock 处理

- `mock/user.ts` 改为仅供本地开发 fallback 使用,或注释掉 `'GET /api/admin/users'` 让真实请求优先

### 路由配置 `config/routes.ts`

- 新增 `{ path: '/user/detail/:userId', component: './UserManage/Detail', access: 'canAdmin' }`

## 数据流与边界情况

### 不绑定 new-api 的用户创建流程

- `adminCreateUser(bindNewApi=false)` 时:
  1. 本地 `sys_user` 创建成功,`status = DISABLE`(1)
  2. `user_new_api_binding` 插入一行,`status = PENDING`(2),`newApiUserId = null`
  3. 返回 `UserCreateResponse`,前端列表中该用户显示为「未绑 newapi」
- 后续管理员可在详情页看到补绑入口(本期只做展示,补绑按钮留接口位)

### 禁用用户的详情查询

- `adminGetUserDetail` 和 `adminGetTokenRecords` 不限制用户状态,管理员有权查看任何用户的详情与记录

### new-api 外部接口异常处理

- 若用户无 binding 或 `newApiUserId` 为空 → 详情返回 `newApiBound = false`,额度字段全部为 `null`
- 若 new-api 服务本身异常 → 按现有 `NewApiUserAcl` 的 `execute()` 逻辑抛出 `AuthException`,前端统一错误提示

### 分页与时间默认值

- `pageNo` 默认 `1`,`pageSize` 默认 `10`
- Token 记录接口的 `startTime`/`endTime` 默认值与现有 `/api/user/token-records` 保持一致(默认最近一年)

### 额度调整

- 现有 `POST /api/admin/users/{userId}/amount` 保持不变
- 详情页「额度概览」Tab 可预留快捷调整入口(本期前端可选实现)
