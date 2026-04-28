# APISIX 本地 Docker 部署说明

本文档记录 `D:\Project\model-gateway-auth\apisix` 目录部署到 WSL/Linux Docker 环境的操作步骤和登录验证方式。

## 环境前提

- Docker Desktop 已启动，并使用 Linux 容器环境。
- 当前 Docker context 为 `desktop-linux`。
- APISIX 配置目录包含以下文件：
  - `docker-compose.yml`
  - `apisix_conf/config.yaml`
  - `dashboard_conf/conf.yaml`

目录路径：

| 环境 | 路径 |
| --- | --- |
| Windows PowerShell | `D:\Project\model-gateway-auth\apisix` |
| WSL/Linux shell | `/mnt/d/Project/model-gateway-auth/apisix` |

## 镜像说明

部署过程中直连 Docker Hub 出现 EOF，因此 `docker-compose.yml` 使用了可拉取成功的代理镜像：

| 服务 | 镜像 |
| --- | --- |
| etcd | `docker.m.daocloud.io/bitnamilegacy/etcd:3.5` |
| APISIX | `docker.m.daocloud.io/apache/apisix:3.9.1-debian` |
| Dashboard | `docker.m.daocloud.io/apache/apisix-dashboard:3.0.1-alpine` |

## 启动部署

在 APISIX 配置目录执行。

Windows PowerShell：

```powershell
cd D:\Project\model-gateway-auth\apisix
docker compose up -d
```

WSL/Linux shell：

```bash
cd /mnt/d/Project/model-gateway-auth/apisix
docker compose up -d
```

查看容器状态：

```powershell
docker compose ps
```

正常状态示例：

```text
NAME                     SERVICE     STATUS
apisix-local             apisix      Up
apisix-local-dashboard   dashboard   Up
apisix-local-etcd        etcd        Up
```

## 访问地址

| 服务 | 地址 |
| --- | --- |
| APISIX 网关 | http://localhost:9080 |
| APISIX Admin API | http://localhost:9180 |
| APISIX Dashboard | http://localhost:9000 |
| Prometheus metrics | http://localhost:9091/apisix/prometheus/metrics |

## Dashboard 登录

访问：

```text
http://localhost:9000
```

登录账号：

```text
用户名：admin
密码：admin123
```

账号密码来自 `dashboard_conf/conf.yaml`：

```yaml
authentication:
  users:
    - username: admin
      password: admin123
```

## 登录与接口验证

### 验证 Dashboard 可访问

```powershell
(Invoke-WebRequest -UseBasicParsing -Uri "http://localhost:9000" -TimeoutSec 10).StatusCode
```

返回 `200` 表示 Dashboard 页面可访问。

### 验证 Admin API 可访问

```powershell
Invoke-WebRequest `
  -UseBasicParsing `
  -Uri "http://localhost:9180/apisix/admin/routes" `
  -Headers @{ "X-API-KEY" = "api-admin-key-1234567654321" } `
  -TimeoutSec 10
```

正常返回示例：

```json
{"total":0,"list":[]}
```

### 验证 Prometheus metrics 可访问

```powershell
(Invoke-WebRequest -UseBasicParsing -Uri "http://localhost:9091/apisix/prometheus/metrics" -TimeoutSec 10).StatusCode
```

返回 `200` 表示 metrics 端点可访问。

### 验证 APISIX 网关端口

```powershell
try {
  (Invoke-WebRequest -UseBasicParsing -Uri "http://localhost:9080" -TimeoutSec 10).StatusCode
} catch {
  $_.Exception.Response.StatusCode.value__
}
```

未配置路由时返回 `404` 是正常现象。

## 常用运维命令

以下命令都在 APISIX 配置目录执行：

```powershell
cd D:\Project\model-gateway-auth\apisix
```

如果在 WSL/Linux shell 中执行，先进入：

```bash
cd /mnt/d/Project/model-gateway-auth/apisix
```

查看日志：

```powershell
docker compose logs -f
```

查看 APISIX 日志：

```powershell
docker compose logs -f apisix
```

重启 APISIX：

```powershell
docker compose restart apisix
```

停止并删除容器：

```powershell
docker compose down
```

重新启动：

```powershell
docker compose up -d
```

## 注意事项

`apisix_conf/config.yaml` 中暂时注释了 `model-gateway-auth` 插件：

```yaml
# - model-gateway-auth
```

原因是当前 `apisix_plugins` 目录没有 `model-gateway-auth.lua` 文件。等插件文件补齐后，需要：

1. 将插件文件挂载到 APISIX 容器可加载目录。
2. 在 `apisix_conf/config.yaml` 中恢复 `model-gateway-auth` 插件配置。
3. 执行 `docker compose restart apisix` 让配置生效。

本地部署为了方便宿主机访问 Admin API，`apisix_conf/config.yaml` 已配置：

```yaml
allow_admin:
  - 0.0.0.0/0
```

该配置仅适合本地开发环境。生产或共享网络环境应限制为可信 IP 段，并修改默认 Admin Key 和 Dashboard 密码。
