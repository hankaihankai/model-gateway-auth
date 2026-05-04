# Admin 用户管理后台 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 补齐后端 admin 用户管理 API(列表/创建/详情/调用记录),并替换前端 mock 为真实接口,新增用户详情页。

**Architecture:** 后端在现有 `UserProfileApplicationService` 中以 `admin` 前缀方法承载 admin 用例,`createUser` 重构为支持可选 `bindNewApi`;前端列表页对接真实 API 并新增创建弹窗,新增独立详情页(Tabs 分信息/额度/记录)。

**Tech Stack:** Spring Boot 3.3 / Java 17 / MyBatis / Sa-Token; Ant Design Pro (Umi Max / React / TypeScript)

---

## File Structure

### 后端 — 新增文件

| 文件 | 职责 |
|------|------|
| `identity/interfaces/dto/AdminUserCreateRequest.java` | 管理员创建用户入参(含 `bindNewApi` 开关) |
| `identity/interfaces/vo/AdminUserListPageVo.java` | 用户列表分页出参 |
| `identity/interfaces/vo/AdminUserListItemVo.java` | 用户列表单项出参 |
| `identity/interfaces/vo/AdminUserDetailVo.java` | 用户详情出参(信息+绑定+额度) |

### 后端 — 修改文件

| 文件 | 职责 |
|------|------|
| `identity/infrastructure/persistence/mapper/UserMapper.java` | 新增 `selectCountByCondition` / `selectListByCondition` 动态 SQL |
| `identity/application/UserProfileApplicationService.java` | 重构 `createUser` 为内部 `createUserInternal`;新增 `adminListUsers` / `adminCreateUser` / `adminGetUserDetail` / `adminGetTokenRecords` |
| `identity/interfaces/http/AdminUserController.java` | 新增 4 个端点:列表/创建/详情/调用记录 |

### 前端 — 新增文件

| 文件 | 职责 |
|------|------|
| `pages/UserManage/Detail/index.tsx` | 用户详情页(Tabs:信息/额度/调用记录) |

### 前端 — 修改文件

| 文件 | 职责 |
|------|------|
| `services/typings.d.ts` | 新增 `AdminUserCreateRequest` / `AdminUserDetailVo` / `UserTokenRecordsQuery`;修正 `UserListItem.status` 为 `0\|1\|2\|3`,新增 `newApiBound` |
| `services/user.ts` | 新增 `createUser` / `getUserDetail` / `getUserTokenRecords` |
| `pages/UserManage/List/index.tsx` | 修正 `STATUS_VALUE_ENUM` 语义;新增「newapi 绑定」列;新增「添加用户」Modal;「查看」改为跳转到详情页 |
| `config/routes.ts` | 新增 `/user-manage/detail/:userId` 路由 |
| `mock/user.ts` | 注释掉 `GET /api/admin/users`,让真实请求优先 |

---

### Task 1: 后端 VO/DTO 新增

**Files:**
- Create: `src/main/java/com/model/gateway/auth/identity/interfaces/dto/AdminUserCreateRequest.java`
- Create: `src/main/java/com/model/gateway/auth/identity/interfaces/vo/AdminUserListPageVo.java`
- Create: `src/main/java/com/model/gateway/auth/identity/interfaces/vo/AdminUserListItemVo.java`
- Create: `src/main/java/com/model/gateway/auth/identity/interfaces/vo/AdminUserDetailVo.java`

- [ ] **Step 1: 创建 `AdminUserCreateRequest.java`**

```java
package com.model.gateway.auth.identity.interfaces.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 管理员创建用户请求。
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AdminUserCreateRequest {

    /**
     * 用户名。
     */
    private String username;

    /**
     * 明文密码。
     */
    private String password;

    /**
     * 用户昵称。
     */
    private String nickname;

    /**
     * 手机号。
     */
    private String phone;

    /**
     * 邮箱。
     */
    private String email;

    /**
     * 是否同步绑定 new-api,默认 true。
     */
    private Boolean bindNewApi;
}
```

- [ ] **Step 2: 创建 `AdminUserListPageVo.java`**

```java
package com.model.gateway.auth.identity.interfaces.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 管理员用户列表分页响应。
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AdminUserListPageVo {

    /**
     * 用户列表。
     */
    private List<AdminUserListItemVo> list;

    /**
     * 总数。
     */
    private long total;

    /**
     * 页码。
     */
    private int pageNo;

    /**
     * 每页数量。
     */
    private int pageSize;
}
```

- [ ] **Step 3: 创建 `AdminUserListItemVo.java`**

```java
package com.model.gateway.auth.identity.interfaces.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 管理员用户列表项。
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AdminUserListItemVo {

    /**
     * 用户ID。
     */
    private Long userId;

    /**
     * 用户名。
     */
    private String username;

    /**
     * 用户昵称。
     */
    private String nickname;

    /**
     * 手机号。
     */
    private String phone;

    /**
     * 邮箱。
     */
    private String email;

    /**
     * 用户角色。
     */
    private String role;

    /**
     * 用户状态。
     */
    private Integer status;

    /**
     * 是否已绑定 new-api。
     */
    private Boolean newApiBound;
}
```

- [ ] **Step 4: 创建 `AdminUserDetailVo.java`**

```java
package com.model.gateway.auth.identity.interfaces.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 管理员用户详情响应。
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AdminUserDetailVo {

    /**
     * 用户ID。
     */
    private Long userId;

    /**
     * 用户名。
     */
    private String username;

    /**
     * 用户昵称。
     */
    private String nickname;

    /**
     * 手机号。
     */
    private String phone;

    /**
     * 邮箱。
     */
    private String email;

    /**
     * 用户角色。
     */
    private String role;

    /**
     * 用户状态。
     */
    private Integer status;

    /**
     * 是否已绑定 new-api。
     */
    private Boolean newApiBound;

    /**
     * new-api 用户ID。
     */
    private Long newApiUserId;

    /**
     * new-api 用户名。
     */
    private String newApiUserName;

    /**
     * new-api 绑定状态。
     */
    private Integer newApiStatus;

    /**
     * 当前余额金额。
     */
    private BigDecimal currentBalanceAmount;

    /**
     * 已用额度金额。
     */
    private BigDecimal usedQuotaAmount;

    /**
     * 总额度金额。
     */
    private BigDecimal totalQuotaAmount;

    /**
     * 剩余原始额度。
     */
    private Long quota;

    /**
     * 已用原始额度。
     */
    private Long usedQuota;

    /**
     * 总原始额度。
     */
    private Long totalQuota;

    /**
     * 额度金额换算比例。
     */
    private Long quotaPerUnit;
}
```

- [ ] **Step 5: 编译验证**

Run: `mvn -q -DskipTests compile`
Expected: BUILD SUCCESS

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/model/gateway/auth/identity/interfaces/dto/AdminUserCreateRequest.java \
  src/main/java/com/model/gateway/auth/identity/interfaces/vo/AdminUserListPageVo.java \
  src/main/java/com/model/gateway/auth/identity/interfaces/vo/AdminUserListItemVo.java \
  src/main/java/com/model/gateway/auth/identity/interfaces/vo/AdminUserDetailVo.java
git commit -m "feat(admin): 添加管理员用户管理 VO/DTO"
```

---

### Task 2: UserMapper 条件查询

**Files:**
- Modify: `src/main/java/com/model/gateway/auth/identity/infrastructure/persistence/mapper/UserMapper.java`

- [ ] **Step 1: 新增 `selectCountByCondition` 和 `selectListByCondition`**

在 `UserMapper.java` 现有方法之后添加以下两个方法,并确保 `java.util.List` 已导入:

```java
    /**
     * 条件查询用户总数。
     *
     * @param username 用户名(模糊)
     * @param role 角色
     * @param status 状态
     * @return 用户总数
     */
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

    /**
     * 条件查询用户分页列表。
     *
     * @param username 用户名(模糊)
     * @param role 角色
     * @param status 状态
     * @param offset 偏移量
     * @param limit 数量限制
     * @return 用户列表
     */
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

- [ ] **Step 2: 编译验证**

Run: `mvn -q -DskipTests compile`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/model/gateway/auth/identity/infrastructure/persistence/mapper/UserMapper.java
git commit -m "feat(mapper): 添加用户条件分页查询"
```

---

### Task 3: UserProfileApplicationService 重构与 admin 方法

**Files:**
- Modify: `src/main/java/com/model/gateway/auth/identity/application/UserProfileApplicationService.java`

- [ ] **Step 1: 导入新增类型**

在现有 import 区添加:
```java
import com.model.gateway.auth.identity.interfaces.dto.AdminUserCreateRequest;
import com.model.gateway.auth.identity.interfaces.vo.AdminUserListItemVo;
import com.model.gateway.auth.identity.interfaces.vo.AdminUserListPageVo;
import com.model.gateway.auth.identity.interfaces.vo.AdminUserDetailVo;
```

- [ ] **Step 2: 重构 `createUser` 为支持可选绑定**

替换现有 `createUser` 方法(约第 203-212 行)为:

```java
    /**
     * 创建系统用户(公开注册,强制绑定 new-api)。
     *
     * @param request 创建用户请求
     * @return 创建用户响应
     */
    public UserCreateResponse createUser(UserCreateRequest request) {
        return createUserInternal(request, true);
    }

    /**
     * 管理员创建用户。
     *
     * @param request 管理员创建用户请求
     * @return 创建用户响应
     */
    public UserCreateResponse adminCreateUser(AdminUserCreateRequest request) {
        UserCreateRequest userRequest = UserCreateRequest.builder()
                .username(request.getUsername())
                .password(request.getPassword())
                .nickname(request.getNickname())
                .phone(request.getPhone())
                .email(request.getEmail())
                .build();
        boolean bindNewApi = request.getBindNewApi() == null || request.getBindNewApi();
        return createUserInternal(userRequest, bindNewApi);
    }

    /**
     * 内部创建用户逻辑。
     *
     * @param request 创建用户请求
     * @param bindNewApi 是否绑定 new-api
     * @return 创建用户响应
     */
    private UserCreateResponse createUserInternal(UserCreateRequest request, boolean bindNewApi) {
        checkCreateRequest(request);
        RegisterContext context = createPendingUser(request);
        if (bindNewApi) {
            bindNewApiUser(context, request);
        }
        return UserCreateResponse.builder()
                .userId(context.getUserId())
                .username(context.getUsername())
                .newApiBound(bindNewApi)
                .build();
    }
```

- [ ] **Step 3: 新增 `adminListUsers`**

在 `getModels` 方法之后(约第 262 行之后)添加:

```java
    /**
     * 管理员查询用户列表。
     *
     * @param username 用户名(模糊)
     * @param role 角色
     * @param status 状态
     * @param pageNo 页码
     * @param pageSize 每页数量
     * @return 用户列表分页
     */
    public AdminUserListPageVo adminListUsers(String username, String role, Integer status, int pageNo, int pageSize) {
        int offset = (pageNo - 1) * pageSize;
        long total = userMapper.selectCountByCondition(username, role, status);
        List<SysUser> users = userMapper.selectListByCondition(username, role, status, offset, pageSize);
        List<AdminUserListItemVo> list = users.stream()
                .map(this::buildAdminListItem)
                .toList();
        return AdminUserListPageVo.builder()
                .list(list)
                .total(total)
                .pageNo(pageNo)
                .pageSize(pageSize)
                .build();
    }

    /**
     * 构建管理员列表项。
     *
     * @param user 系统用户
     * @return 管理员列表项
     */
    private AdminUserListItemVo buildAdminListItem(SysUser user) {
        UserNewApiBinding binding = bindingMapper.selectByUserId(user.getUserId());
        boolean newApiBound = binding != null && binding.getNewApiUserId() != null;
        return AdminUserListItemVo.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .phone(user.getPhone())
                .email(user.getEmail())
                .role(user.getRole())
                .status(user.getStatus())
                .newApiBound(newApiBound)
                .build();
    }
```

- [ ] **Step 4: 新增 `adminGetUserDetail`**

继续添加:

```java
    /**
     * 管理员查询用户详情。
     *
     * @param userId 用户ID
     * @return 用户详情
     */
    public AdminUserDetailVo adminGetUserDetail(Long userId) {
        SysUser user = userMapper.selectByUserId(userId);
        if (user == null) {
            throw new AuthException("用户不存在");
        }
        UserNewApiBinding binding = bindingMapper.selectByUserId(userId);
        boolean newApiBound = binding != null && binding.getNewApiUserId() != null;

        AdminUserDetailVo.AdminUserDetailVoBuilder builder = AdminUserDetailVo.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .phone(user.getPhone())
                .email(user.getEmail())
                .role(user.getRole())
                .status(user.getStatus())
                .newApiBound(newApiBound);

        if (binding != null) {
            builder.newApiUserId(binding.getNewApiUserId())
                    .newApiUserName(binding.getNewApiUserName())
                    .newApiStatus(binding.getStatus());
        }

        if (newApiBound) {
            NewApiUserAcl.NewApiUserStatsData stats = newApiUserAcl.getUserStats(binding.getNewApiUserId(), null, null);
            NewApiUserAcl.AccountData accountData = stats.getAccountData();
            if (accountData != null) {
                builder.currentBalanceAmount(accountData.getCurrentBalanceAmount())
                        .usedQuotaAmount(accountData.getUsedQuotaAmount())
                        .totalQuotaAmount(accountData.getTotalQuotaAmount())
                        .quota(accountData.getQuota())
                        .usedQuota(accountData.getUsedQuota())
                        .totalQuota(accountData.getTotalQuota())
                        .quotaPerUnit(accountData.getQuotaPerUnit());
            }
        }

        return builder.build();
    }
```

- [ ] **Step 5: 新增 `adminGetTokenRecords`**

继续添加:

```java
    /**
     * 管理员查询用户Token使用记录。
     *
     * @param userId 用户ID
     * @param pageNo 页码
     * @param pageSize 每页数量
     * @param startTimestamp 开始Unix时间戳秒
     * @param endTimestamp 结束Unix时间戳秒
     * @param modelName 模型名称
     * @return Token使用记录分页
     */
    public UserTokenRecordsVo adminGetTokenRecords(
            Long userId,
            Integer pageNo,
            Integer pageSize,
            Long startTimestamp,
            Long endTimestamp,
            String modelName) {
        SysUser user = userMapper.selectByUserId(userId);
        if (user == null) {
            throw new AuthException("用户不存在");
        }
        UserNewApiBinding binding = bindingMapper.selectByUserId(userId);
        if (binding == null || binding.getNewApiUserId() == null) {
            return UserTokenRecordsVo.builder()
                    .page(pageNo != null ? pageNo : 1)
                    .pageSize(pageSize != null ? pageSize : 10)
                    .total(0L)
                    .items(Collections.emptyList())
                    .build();
        }
        NewApiUserAcl.NewApiQuotaRecordsData records = newApiUserAcl.getQuotaRecords(
                binding.getNewApiUserId(), pageNo, pageSize, startTimestamp, endTimestamp, modelName);
        List<NewApiUserAcl.QuotaRecordItem> items = records.getItems() == null
                ? Collections.emptyList()
                : records.getItems();
        return UserTokenRecordsVo.builder()
                .page(records.getPage())
                .pageSize(records.getPageSize())
                .total(records.getTotal())
                .items(items.stream().map(this::buildTokenRecordItem).toList())
                .build();
    }
```

- [ ] **Step 6: 编译验证**

Run: `mvn -q -DskipTests compile`
Expected: BUILD SUCCESS

- [ ] **Step 7: Commit**

```bash
git add src/main/java/com/model/gateway/auth/identity/application/UserProfileApplicationService.java
git commit -m "feat(service): 添加 admin 用户列表/创建/详情/调用记录方法,重构 createUser 支持可选绑定"
```

---

### Task 4: AdminUserController 端点

**Files:**
- Modify: `src/main/java/com/model/gateway/auth/identity/interfaces/http/AdminUserController.java`

- [ ] **Step 1: 导入新增类型与工具类**

在现有 import 区添加:
```java
import com.model.gateway.auth.identity.interfaces.dto.AdminUserCreateRequest;
import com.model.gateway.auth.identity.interfaces.vo.AdminUserListPageVo;
import com.model.gateway.auth.identity.interfaces.vo.AdminUserDetailVo;
import com.model.gateway.auth.identity.interfaces.vo.UserTokenRecordsVo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
```

- [ ] **Step 2: 新增 4 个端点与辅助方法**

在现有 `updateUserAmount` 方法之后添加:

```java
    /**
     * 管理员查询用户列表。
     *
     * @param pageNo 页码
     * @param pageSize 每页数量
     * @param username 用户名(模糊)
     * @param role 角色
     * @param status 状态
     * @return 用户列表分页
     */
    @GetMapping
    public ApiResponse<AdminUserListPageVo> listUsers(
            @RequestParam(value = "pageNo", required = false, defaultValue = "1") int pageNo,
            @RequestParam(value = "pageSize", required = false, defaultValue = "10") int pageSize,
            @RequestParam(value = "username", required = false) String username,
            @RequestParam(value = "role", required = false) String role,
            @RequestParam(value = "status", required = false) Integer status) {
        return ApiResponse.success(userProfileService.adminListUsers(username, role, status, pageNo, pageSize));
    }

    /**
     * 管理员创建用户。
     *
     * @param request 创建用户请求
     * @return 创建用户响应
     */
    @PostMapping
    public ApiResponse<UserCreateResponse> createUser(@RequestBody AdminUserCreateRequest request) {
        return ApiResponse.success(userProfileService.adminCreateUser(request));
    }

    /**
     * 管理员查询用户详情。
     *
     * @param userId 用户ID
     * @return 用户详情
     */
    @GetMapping("/{userId}")
    public ApiResponse<AdminUserDetailVo> getUserDetail(@PathVariable Long userId) {
        return ApiResponse.success(userProfileService.adminGetUserDetail(userId));
    }

    /**
     * 管理员查询用户Token使用记录。
     *
     * @param userId 用户ID
     * @param pageNo 页码
     * @param pageSize 每页数量
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param modelName 模型名称
     * @return Token使用记录分页
     */
    @GetMapping("/{userId}/token-records")
    public ApiResponse<UserTokenRecordsVo> getTokenRecords(
            @PathVariable Long userId,
            @RequestParam(value = "pageNo", required = false) Integer pageNo,
            @RequestParam(value = "pageSize", required = false) Integer pageSize,
            @RequestParam(value = "startTime", required = false) String startTime,
            @RequestParam(value = "endTime", required = false) String endTime,
            @RequestParam(value = "modelName", required = false) String modelName) {
        Long startTimestamp = parseTimestamp(startTime);
        Long endTimestamp = parseTimestamp(endTime);
        if (startTimestamp == null) {
            startTimestamp = LocalDateTime.now().minusYears(1).atZone(ZoneId.systemDefault()).toEpochSecond();
        }
        if (endTimestamp == null) {
            endTimestamp = LocalDateTime.now().atZone(ZoneId.systemDefault()).toEpochSecond();
        }
        return ApiResponse.success(userProfileService.adminGetTokenRecords(
                userId, pageNo, pageSize, startTimestamp, endTimestamp, modelName));
    }

    /**
     * 解析时间字符串为 Unix 时间戳秒。
     *
     * @param timeStr 时间字符串,格式 yyyy-MM-dd HH:mm:ss
     * @return Unix 时间戳秒
     */
    private Long parseTimestamp(String timeStr) {
        if (!org.springframework.util.StringUtils.hasText(timeStr)) {
            return null;
        }
        try {
            LocalDateTime dateTime = LocalDateTime.parse(timeStr, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            return dateTime.atZone(ZoneId.systemDefault()).toEpochSecond();
        } catch (DateTimeParseException exception) {
            throw new AuthException("时间格式不正确,应为 yyyy-MM-dd HH:mm:ss");
        }
    }
```

- [ ] **Step 3: 编译验证**

Run: `mvn -q -DskipTests compile`
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/model/gateway/auth/identity/interfaces/http/AdminUserController.java
git commit -m "feat(controller): 添加 admin 用户列表/创建/详情/调用记录端点"
```

---

### Task 5: 前端类型与服务层

**Files:**
- Modify: `model-gateway-auth-front/src/services/typings.d.ts`
- Modify: `model-gateway-auth-front/src/services/user.ts`

- [ ] **Step 1: 更新 `services/typings.d.ts`**

修改 `UserListItem` 的 `status` 类型并新增 `newApiBound`,同时新增以下接口:

```typescript
  interface AdminUserCreateRequest {
    username: string;
    password: string;
    nickname?: string;
    phone?: string;
    email?: string;
    bindNewApi?: boolean;
  }

  interface AdminUserDetailVo {
    userId: number;
    username: string;
    nickname: string;
    phone: string;
    email: string;
    role: 'ADMIN' | 'USER';
    status: 0 | 1 | 2 | 3;
    newApiBound: boolean;
    newApiUserId?: number;
    newApiUserName?: string;
    newApiStatus?: number;
    currentBalanceAmount?: string;
    usedQuotaAmount?: string;
    totalQuotaAmount?: string;
    quota?: number;
    usedQuota?: number;
    totalQuota?: number;
    quotaPerUnit?: number;
  }

  interface UserTokenRecordsQuery {
    pageNo?: number;
    pageSize?: number;
    startTime?: string;
    endTime?: string;
    modelName?: string;
  }
```

同时把 `UserListItem` 改为:
```typescript
  interface UserListItem {
    userId: number;
    username: string;
    nickname: string;
    phone: string;
    email: string;
    role: 'ADMIN' | 'USER';
    status: 0 | 1 | 2 | 3;
    newApiBound: boolean;
  }
```

- [ ] **Step 2: 更新 `services/user.ts`**

在现有 `listUsers` 下方追加:

```typescript
/**
 * 管理员创建用户。
 */
export async function createUser(body: API.AdminUserCreateRequest) {
  return request<API.ApiResponse<API.UserCreateResponse>>('/api/admin/users', {
    method: 'POST',
    data: body,
  });
}

/**
 * 管理员查询用户详情。
 */
export async function getUserDetail(userId: number) {
  return request<API.ApiResponse<API.AdminUserDetailVo>>(`/api/admin/users/${userId}`, {
    method: 'GET',
  });
}

/**
 * 管理员查询用户Token使用记录。
 */
export async function getUserTokenRecords(userId: number, params: API.UserTokenRecordsQuery) {
  return request<API.ApiResponse<API.UserTokenRecordsVo>>(`/api/admin/users/${userId}/token-records`, {
    method: 'GET',
    params,
  });
}
```

- [ ] **Step 3: 前端类型检查**

Run (在 `model-gateway-auth-front` 目录下): `npm run tsc`
Expected: 无类型错误(可能有一些既有错误,但新增代码不应引入新错误)

- [ ] **Step 4: Commit**

```bash
cd model-gateway-auth-front
git add src/services/typings.d.ts src/services/user.ts
git commit -m "feat(front): 添加 admin 用户管理类型定义与服务方法"
```

---

### Task 6: 前端列表页

**Files:**
- Modify: `model-gateway-auth-front/src/pages/UserManage/List/index.tsx`

- [ ] **Step 1: 修正状态枚举并新增 newapi 绑定列**

将 `STATUS_VALUE_ENUM` 替换为:
```typescript
const STATUS_VALUE_ENUM = {
  0: { text: '启用', color: 'green' },
  1: { text: '禁用', color: 'default' },
  2: { text: '处理中', color: 'blue' },
  3: { text: '异常', color: 'red' },
};
```

在 `columns` 数组的「状态」列之前插入新列:
```typescript
    {
      title: 'newapi 绑定',
      dataIndex: 'newApiBound',
      width: 120,
      hideInSearch: true,
      render: (_, record) => (
        <Tag color={record.newApiBound ? 'green' : 'default'}>
          {record.newApiBound ? '已绑' : '未绑'}
        </Tag>
      ),
    },
```

- [ ] **Step 2: 修改「查看」为路由跳转并新增「添加用户」Modal**

将操作列的 render 改为:
```typescript
    {
      title: '操作',
      valueType: 'option',
      width: 80,
      render: (_, record) => [
        <Link key="view" to={`/user-manage/detail/${record.userId}`}>
          查看
        </Link>,
      ],
    },
```

新增 import:
```typescript
import { Link } from '@umijs/max';
import { Modal, Form, Input, Switch } from 'antd';
import { createUser } from '@/services/user';
```

在组件内新增 state 和 form:
```typescript
  const [createModalOpen, setCreateModalOpen] = useState(false);
  const [createForm] = Form.useForm();

  const handleCreate = async (values: any) => {
    await createUser({
      ...values,
      bindNewApi: values.bindNewApi ?? true,
    });
    message.success('创建成功');
    setCreateModalOpen(false);
    createForm.resetFields();
    actionRef.current?.reload();
  };
```

修改 `ProTable` 的 `toolBarRender`:
```typescript
        toolBarRender={() => [
          <Button
            key="create"
            type="primary"
            onClick={() => setCreateModalOpen(true)}
          >
            添加用户
          </Button>,
        ]}
```

在 JSX 末尾(`,</PageContainer>` 之前)添加 Modal:
```tsx
      <Modal
        title="添加用户"
        open={createModalOpen}
        onOk={() => createForm.submit()}
        onCancel={() => {
          setCreateModalOpen(false);
          createForm.resetFields();
        }}
        destroyOnClose
      >
        <Form form={createForm} onFinish={handleCreate} layout="vertical">
          <Form.Item name="username" label="用户名" rules={[{ required: true, message: '请输入用户名' }]}>
            <Input />
          </Form.Item>
          <Form.Item name="password" label="密码" rules={[{ required: true, message: '请输入密码' }]}>
            <Input.Password />
          </Form.Item>
          <Form.Item name="nickname" label="昵称">
            <Input />
          </Form.Item>
          <Form.Item name="phone" label="手机号">
            <Input />
          </Form.Item>
          <Form.Item name="email" label="邮箱">
            <Input />
          </Form.Item>
          <Form.Item
            name="bindNewApi"
            label="同步创建 new-api 账号"
            valuePropName="checked"
            initialValue={true}
          >
            <Switch />
          </Form.Item>
        </Form>
      </Modal>
```

- [ ] **Step 3: 前端类型检查**

Run: `npm run tsc`
Expected: 无新增类型错误

- [ ] **Step 4: Commit**

```bash
git add src/pages/UserManage/List/index.tsx
git commit -m "feat(front): 用户列表页对接真实 API,新增创建用户 Modal,修正状态枚举"
```

---

### Task 7: 前端详情页与路由

**Files:**
- Create: `model-gateway-auth-front/src/pages/UserManage/Detail/index.tsx`
- Modify: `model-gateway-auth-front/config/routes.ts`
- Modify: `model-gateway-auth-front/mock/user.ts`

- [ ] **Step 1: 创建详情页 `pages/UserManage/Detail/index.tsx`**

```tsx
import { PageContainer } from '@ant-design/pro-components';
import { Card, Tabs, Descriptions, Tag, ProTable } from '@ant-design/pro-components';
import { useParams } from '@umijs/max';
import { useRequest } from '@umijs/max';
import React from 'react';
import { getUserDetail, getUserTokenRecords } from '@/services/user';

const UserDetail: React.FC = () => {
  const { userId } = useParams<{ userId: string }>();
  const id = Number(userId);
  const { data: detailRes, loading: detailLoading } = useRequest(() => getUserDetail(id));

  const detail = detailRes?.data;

  const recordColumns = [
    { title: 'ID', dataIndex: 'id', width: 80 },
    { title: '模型', dataIndex: 'modelName', width: 140 },
    { title: '时间', dataIndex: 'createdAt', width: 160 },
    { title: 'Token 消耗', dataIndex: 'tokenUsed', width: 100 },
    { title: '请求次数', dataIndex: 'count', width: 100 },
    { title: '额度消耗', dataIndex: 'quota', width: 100 },
  ];

  const statusText = (s?: number) => {
    if (s === 0) return '启用';
    if (s === 1) return '禁用';
    if (s === 2) return '处理中';
    if (s === 3) return '异常';
    return '未知';
  };

  const statusColor = (s?: number) => {
    if (s === 0) return 'green';
    if (s === 1) return 'default';
    if (s === 2) return 'blue';
    if (s === 3) return 'red';
    return 'default';
  };

  const items = [
    {
      key: 'info',
      label: '用户信息',
      children: (
        <Descriptions loading={detailLoading} bordered column={2}>
          <Descriptions.Item label="用户ID">{detail?.userId}</Descriptions.Item>
          <Descriptions.Item label="用户名">{detail?.username}</Descriptions.Item>
          <Descriptions.Item label="昵称">{detail?.nickname}</Descriptions.Item>
          <Descriptions.Item label="手机号">{detail?.phone}</Descriptions.Item>
          <Descriptions.Item label="邮箱">{detail?.email}</Descriptions.Item>
          <Descriptions.Item label="角色">{detail?.role}</Descriptions.Item>
          <Descriptions.Item label="状态">
            <Tag color={statusColor(detail?.status)}>{statusText(detail?.status)}</Tag>
          </Descriptions.Item>
          <Descriptions.Item label="newapi 用户ID">{detail?.newApiUserId ?? '-'}</Descriptions.Item>
          <Descriptions.Item label="newapi 用户名">{detail?.newApiUserName ?? '-'}</Descriptions.Item>
        </Descriptions>
      ),
    },
    {
      key: 'quota',
      label: '额度概览',
      children: (
        <Descriptions loading={detailLoading} bordered column={2}>
          <Descriptions.Item label="当前余额">{detail?.currentBalanceAmount ?? '-'}</Descriptions.Item>
          <Descriptions.Item label="已用额度">{detail?.usedQuotaAmount ?? '-'}</Descriptions.Item>
          <Descriptions.Item label="总额度">{detail?.totalQuotaAmount ?? '-'}</Descriptions.Item>
          <Descriptions.Item label="剩余额度(原始)">{detail?.quota ?? '-'}</Descriptions.Item>
          <Descriptions.Item label="已用额度(原始)">{detail?.usedQuota ?? '-'}</Descriptions.Item>
          <Descriptions.Item label="总额度(原始)">{detail?.totalQuota ?? '-'}</Descriptions.Item>
          <Descriptions.Item label="换算比例">{detail?.quotaPerUnit ?? '-'}</Descriptions.Item>
        </Descriptions>
      ),
    },
    {
      key: 'records',
      label: 'AI 调用记录',
      children: (
        <ProTable
          columns={recordColumns}
          rowKey="id"
          search={false}
          request={async (params) => {
            const res = await getUserTokenRecords(id, {
              pageNo: params.current,
              pageSize: params.pageSize,
            });
            if (res.code !== 200 || !res.data) {
              return { success: false, data: [], total: 0 };
            }
            return { success: true, data: res.data.items, total: res.data.total };
          }}
          pagination={{ defaultPageSize: 10 }}
        />
      ),
    },
  ];

  return (
    <PageContainer title={`用户详情 #${id}`}>
      <Card>
        <Tabs items={items} />
      </Card>
    </PageContainer>
  );
};

export default UserDetail;
```

- [ ] **Step 2: 更新路由 `config/routes.ts`**

在 `user-manage` 的 `routes` 数组中添加详情路由:
```typescript
      { path: '/user-manage/detail/:userId', name: '用户详情', component: './UserManage/Detail', hideInMenu: true },
```

- [ ] **Step 3: 注释 mock `mock/user.ts`**

将 `'GET /api/admin/users':` 整个处理器注释掉,保留文件但让真实后端优先:
```typescript
  // 'GET /api/admin/users': (req: Request, res: Response) => { ... },
```

- [ ] **Step 4: 前端类型检查**

Run: `npm run tsc`
Expected: 无新增类型错误

- [ ] **Step 5: Commit**

```bash
git add src/pages/UserManage/Detail/index.tsx config/routes.ts mock/user.ts
git commit -m "feat(front): 新增用户详情页,添加路由,注释 admin 用户列表 mock"
```

---

## Self-Review

### 1. Spec Coverage

| Spec 需求 | 对应 Task |
|-----------|-----------|
| GET /api/admin/users (列表) | Task 2 + Task 3 + Task 4 |
| POST /api/admin/users (创建,可选 bindNewApi) | Task 1 + Task 3 + Task 4 |
| GET /api/admin/users/{userId} (详情) | Task 1 + Task 3 + Task 4 |
| GET /api/admin/users/{userId}/token-records (调用记录) | Task 3 + Task 4 |
| 前端列表对接真实 API | Task 5 + Task 6 |
| 前端创建用户 Modal | Task 6 |
| 前端详情页 | Task 7 |
| 状态枚举修正 | Task 6 |
| Mock 注释 | Task 7 |

**无遗漏。**

### 2. Placeholder Scan

- 无 "TBD" / "TODO" / "implement later"
- 所有步骤均包含具体代码
- 无 "Similar to Task N"
- 所有命令均包含预期输出

### 3. Type Consistency

- `AdminUserCreateRequest.bindNewApi` → Boolean(可为 null,默认 true)
- `AdminUserListItemVo.status` / `AdminUserDetailVo.status` → Integer(0/1/2/3)
- `UserListItem.status` 前端类型 → `0 | 1 | 2 | 3`
- `createUserInternal` 中 `bindNewApi` 为 boolean primitive
- 前后端 `pageNo`/`pageSize` 命名一致

**一致,无冲突。**
