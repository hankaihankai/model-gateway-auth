# Docker 发布文档

本文档只使用两个部署文件：

- `Dockerfile`：构建 `model-gateway-auth` 镜像。
- `docker-compose.yml`：启动 `model-gateway-auth`、APISIX、etcd、Dashboard，并通过 `apisix-init` 自动配置 APISIX 路由。

说明：MySQL 和 Redis 不由本项目创建。MySQL 使用宿主机已有实例，Redis 使用已有 Docker 容器 `new-api-redis`，密码为 `123456`。

## 1. 上传代码

把整个项目上传到服务器：

```bash
/opt/model-gateway-auth
```

目录中需要包含：

```text
Dockerfile
docker-compose.yml
.env
pom.xml
src
docs/database.sql
apisix/apisix_conf/config.yaml
apisix/apisix_plugins/model-gateway-auth.lua
apisix/dashboard_conf/conf.yaml
```

进入目录：

```bash
cd /opt/model-gateway-auth
```

## 2. 确认外部 MySQL 和 Redis

MySQL 使用宿主机连接：

```text
host.docker.internal:3306
database: new_api
username: new_api
password: 123456
```

Redis 使用已有 Docker 容器：

```text
container: new-api-redis
port: 6379
password: 123456
```

确认 Redis 容器所在 Docker 网络：

```bash
docker inspect new-api-redis --format '{{range $k,$v := .NetworkSettings.Networks}}{{$k}}{{end}}'
```

把输出值写到 `.env` 的 `AUTH_NEW_API_DOCKER_NETWORK`。例如输出是 `new-api_default`，则保持：

```text
AUTH_NEW_API_DOCKER_NETWORK=new-api_default
```

如果 MySQL 未初始化业务表，执行：

```bash
mysql -h 127.0.0.1 -P 3306 -unew_api -p123456 new_api < docs/database.sql
```

## 3. 配置 `.env`

项目根目录已经提供 `.env`，上线前按服务器实际值修改：

```env
AUTH_MYSQL_HOST=host.docker.internal
AUTH_MYSQL_PORT=3306
AUTH_MYSQL_DATABASE=new_api
AUTH_MYSQL_USERNAME=new_api
AUTH_MYSQL_PASSWORD=123456

AUTH_REDIS_HOST=new-api-redis
AUTH_REDIS_PORT=6379
AUTH_REDIS_PASSWORD=123456
AUTH_NEW_API_DOCKER_NETWORK=new-api_default

AUTH_PORT=8080
APISIX_PORT=9080
APISIX_ADMIN_PORT=9180
APISIX_METRICS_PORT=9091
APISIX_DASHBOARD_PORT=9000

APISIX_ADMIN_KEY=api-admin-key-1234567654321
NEW_API_NODE=120.53.240.51:3000

GATEWAY_CREDENTIAL_KEY_ID=main
GATEWAY_JWT_PRIVATE_KEY=REPLACE_WITH_PRIVATE_KEY_PEM_USE_BACKSLASH_N
GATEWAY_JWT_PUBLIC_KEY=REPLACE_WITH_PUBLIC_KEY_PEM_USE_BACKSLASH_N
GATEWAY_CREDENTIAL_AES_KEY=REPLACE_WITH_BASE64_32_BYTE_AES_KEY
APISIX_GATEWAY_SECRET=REPLACE_WITH_APISIX_GATEWAY_SECRET
```

注意：`APISIX_ADMIN_KEY` 必须和 `apisix/apisix_conf/config.yaml` 中的 Admin Key 保持一致。

## 4. 生成密钥

生成 JWT 私钥和公钥：

```bash
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out jwt-private.pem
openssl rsa -pubout -in jwt-private.pem -out jwt-public.pem
```

生成 AES 密钥和 APISIX 回源密钥：

```bash
openssl rand -base64 32 > aes-key.txt
openssl rand -base64 32 > gateway-secret.txt
```

把密钥写入 `.env`。注意：PEM 私钥和公钥不能直接多行粘贴到 `.env`，必须转换成带 `\n` 的单行字符串：

```bash
python3 - <<'PY'
from pathlib import Path

env_path = Path(".env")
env_lines = env_path.read_text(encoding="utf-8").splitlines()

def pem_to_env_value(path):
    return "".join(line.strip() + "\\n" for line in Path(path).read_text(encoding="utf-8").splitlines() if line.strip())

values = {
    "GATEWAY_JWT_PRIVATE_KEY": pem_to_env_value("jwt-private.pem"),
    "GATEWAY_JWT_PUBLIC_KEY": pem_to_env_value("jwt-public.pem"),
    "GATEWAY_CREDENTIAL_AES_KEY": Path("aes-key.txt").read_text(encoding="utf-8").strip(),
    "APISIX_GATEWAY_SECRET": Path("gateway-secret.txt").read_text(encoding="utf-8").strip(),
}

written = set()
next_lines = []
for line in env_lines:
    key = line.split("=", 1)[0] if "=" in line else None
    if key in values:
        next_lines.append(f"{key}={values[key]}")
        written.add(key)
    else:
        next_lines.append(line)

for key, value in values.items():
    if key not in written:
        next_lines.append(f"{key}={value}")

env_path.write_text("\n".join(next_lines) + "\n", encoding="utf-8")
PY
```

检查关键密钥不是占位符：

```bash
grep -E 'GATEWAY_JWT_PRIVATE_KEY|GATEWAY_JWT_PUBLIC_KEY|GATEWAY_CREDENTIAL_AES_KEY|APISIX_GATEWAY_SECRET' .env
```

检查 `.env` 是否仍包含错误的 PEM 多行内容：

```bash
grep -nE '^[A-Za-z0-9+/=]{20,}$|-----BEGIN|-----END' .env
```

正常情况下，只应该看到 `GATEWAY_JWT_PRIVATE_KEY=` 和 `GATEWAY_JWT_PUBLIC_KEY=` 两行中包含 `-----BEGIN`、`-----END`，不应该出现单独一行 `MII...`。

## 5. 启动服务

检查 compose 配置：

```bash
docker compose --env-file .env config
```

启动：

```bash
docker compose --env-file .env up -d --build
```

这个命令会：

1. 构建 `model-gateway-auth` 镜像。
2. 启动 `model-gateway-auth`。
3. 启动 APISIX、etcd、Dashboard。
4. 通过 `apisix-init` 自动创建 APISIX 路由：
   `/v1/chat/completions -> http://120.53.240.51:3000/v1/chat/completions`

## 6. 检查服务状态

```bash
docker compose --env-file .env ps
```

重点确认这些服务是 `Up`：

```text
model-gateway-auth
apisix
apisix-etcd
apisix-dashboard
```

查看日志：

```bash
docker compose --env-file .env logs --tail=100 model-gateway-auth
docker compose --env-file .env logs --tail=100 apisix
docker compose --env-file .env logs --tail=100 apisix-init
```

`apisix-init` 日志中应看到：

```text
APISIX route model-gateway-chat-completions configured
```

## 7. 确认 APISIX 路由

正常情况下，`apisix-init` 会自动创建路由。先读取 APISIX Admin 参数：

```bash
APISIX_ADMIN_KEY=$(grep -E '^APISIX_ADMIN_KEY=' .env | cut -d= -f2-)
APISIX_ADMIN_PORT=$(grep -E '^APISIX_ADMIN_PORT=' .env | cut -d= -f2-)
APISIX_ADMIN_PORT=${APISIX_ADMIN_PORT:-9180}
```

确认 APISIX Admin API 可访问：

```bash
curl -i "http://127.0.0.1:${APISIX_ADMIN_PORT:-9180}/apisix/admin/routes" \
  -H "X-API-KEY: ${APISIX_ADMIN_KEY}"
```

查看自动创建的路由：

```bash
curl -s "http://127.0.0.1:${APISIX_ADMIN_PORT:-9180}/apisix/admin/routes/model-gateway-chat-completions" \
  -H "X-API-KEY: ${APISIX_ADMIN_KEY}"
```

如果 `apisix-init` 没有成功执行，再用下面的备用流程手动生成路由配置。这里不要 `source .env`，避免 PEM 密钥中的 `\n` 被 shell 处理：

```bash
python3 - <<'PY'
import json
from pathlib import Path

def read_env(path):
    values = {}
    for raw_line in Path(path).read_text(encoding="utf-8").splitlines():
        line = raw_line.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        key, value = line.split("=", 1)
        values[key] = value
    return values

env = read_env(".env")

def value(key, default=None):
    result = env.get(key, default)
    if result is None or result == "":
        raise SystemExit(f"缺少 .env 配置：{key}")
    return result

route = {
    "name": "model-gateway-chat-completions",
    "uri": "/v1/chat/completions",
    "methods": ["POST"],
    "plugins": {
        "model-gateway-auth": {
            "jwt_public_key": value("GATEWAY_JWT_PUBLIC_KEY"),
            "jwt_issuer": "model-gateway-auth",
            "jwt_audience": "apisix-llm-gateway",
            "redis_host": value("AUTH_REDIS_HOST", "new-api-redis"),
            "redis_port": int(value("AUTH_REDIS_PORT", "6379")),
            "redis_database": 0,
            "redis_password": value("AUTH_REDIS_PASSWORD"),
            "credential_ensure_url": "http://model-gateway-auth:8080/api/gateway/new-api-credential/ensure",
            "gateway_secret": value("APISIX_GATEWAY_SECRET"),
            "aes_keys": {
                value("GATEWAY_CREDENTIAL_KEY_ID", "main"): value("GATEWAY_CREDENTIAL_AES_KEY")
            }
        }
    },
    "upstream": {
        "type": "roundrobin",
        "scheme": "http",
        "pass_host": "node",
        "nodes": {
            value("NEW_API_NODE", "120.53.240.51:3000"): 1
        }
    }
}

Path("/tmp/model-gateway-chat-route.json").write_text(
    json.dumps(route, ensure_ascii=False, indent=2) + "\n",
    encoding="utf-8",
)
PY
```

写入 APISIX：

```bash
curl -i -X PUT "http://127.0.0.1:${APISIX_ADMIN_PORT:-9180}/apisix/admin/routes/model-gateway-chat-completions" \
  -H "X-API-KEY: ${APISIX_ADMIN_KEY}" \
  -H "Content-Type: application/json" \
  --data-binary @/tmp/model-gateway-chat-route.json
```

返回 `200` 或 `201` 表示路由创建成功。查看路由：

```bash
curl -s "http://127.0.0.1:${APISIX_ADMIN_PORT:-9180}/apisix/admin/routes/model-gateway-chat-completions" \
  -H "X-API-KEY: ${APISIX_ADMIN_KEY}"
```

## 8. 创建用户和 new-api 绑定

创建业务用户：

```bash
curl -X POST http://127.0.0.1:${AUTH_PORT:-8080}/api/admin/users/registerUser \
  -H "Content-Type: application/json" \
  -d '{
    "username": "user001",
    "password": "123456",
    "nickname": "测试用户",
    "role": "USER",
    "status": "ENABLE",
    "newApiUserId": null,
    "newApiUserName": "",
    "newApiApiKey": ""
  }'
```

如果你手动维护绑定，直接连宿主机 MySQL：

```bash
mysql -h 127.0.0.1 -P 3306 -unew_api -p123456 new_api
```

执行：

```sql
INSERT INTO user_new_api_binding (
  user_id,
  new_api_user_id,
  new_api_user_name,
  new_api_api_key,
  status
) VALUES (
  1,
  1,
  'newapi_user',
  'sk_xxx',
  'ENABLE'
);
```

说明：

- `new_api_api_key` 先填 `sk_xxx`，后续替换成真实 new-api API Key。
- `status` 必须是 `ENABLE`。

## 9. 登录并确认 Redis 凭证

登录：

```bash
LOGIN_RESPONSE=$(curl -s -X POST http://127.0.0.1:${AUTH_PORT:-8080}/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "user001",
    "password": "123456"
  }')

echo "$LOGIN_RESPONSE"
TOKEN=$(echo "$LOGIN_RESPONSE" | sed -n 's/.*"accessToken":"\([^"]*\)".*/\1/p')
```

确认 Redis 里有加密后的 API Key：

```bash
docker exec new-api-redis redis-cli -a 123456 GET gateway:newapi:credential:1
docker exec new-api-redis redis-cli -a 123456 TTL gateway:newapi:credential:1
```

Redis 内容应包含：

```text
newApiUserId
newApiUserName
apiKeyCipher
status
```

Redis 中不应该出现明文 `sk_xxx`。

## 10. 调用 APISIX

无 Token 验证：

```bash
curl -i -X POST http://127.0.0.1:${APISIX_PORT:-9080}/v1/chat/completions \
  -H "Content-Type: application/json" \
  -d '{
    "model": "test",
    "messages": [
      {"role": "user", "content": "hi"}
    ]
  }'
```

预期：

```text
401
缺少Bearer Token
```

带登录 JWT 调用：

```bash
curl -i -X POST http://127.0.0.1:${APISIX_PORT:-9080}/v1/chat/completions \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "model": "gpt-4o-mini",
    "messages": [
      {"role": "user", "content": "你好"}
    ],
    "stream": false
  }'
```

如果 `new_api_api_key` 还是 `sk_xxx`，new-api 可能返回 key 无效；替换真实 key 后应返回标准 OpenAI 格式响应。

## 11. 刷新 Token 并延长 Redis TTL

查看刷新前 TTL：

```bash
docker exec new-api-redis redis-cli -a 123456 TTL gateway:newapi:credential:1
```

刷新：

```bash
REFRESH_RESPONSE=$(curl -s -X POST http://127.0.0.1:${AUTH_PORT:-8080}/api/auth/refresh \
  -H "Authorization: Bearer $TOKEN")

echo "$REFRESH_RESPONSE"
TOKEN=$(echo "$REFRESH_RESPONSE" | sed -n 's/.*"accessToken":"\([^"]*\)".*/\1/p')
```

查看刷新后 TTL：

```bash
docker exec new-api-redis redis-cli -a 123456 TTL gateway:newapi:credential:1
```

刷新后 TTL 应重新接近系统配置的 JWT 过期秒数。

## 12. 停止和重启

停止：

```bash
docker compose --env-file .env down
```

重启：

```bash
docker compose --env-file .env up -d --build
```

查看日志：

```bash
docker compose --env-file .env logs -f model-gateway-auth
docker compose --env-file .env logs -f apisix
```

## 13. 常见问题

### Compose 提示 external network not found

说明 `.env` 中的 `AUTH_NEW_API_DOCKER_NETWORK` 不正确，重新确认：

```bash
docker inspect new-api-redis --format '{{range $k,$v := .NetworkSettings.Networks}}{{$k}}{{end}}'
```

改完后重新启动：

```bash
docker compose --env-file .env up -d --build
```

### APISIX route 没有创建

先查看 `apisix-init` 日志：

```bash
docker compose --env-file .env logs apisix-init
```

再按第 7 步查看路由：

```bash
curl -s "http://127.0.0.1:${APISIX_ADMIN_PORT:-9180}/apisix/admin/routes/model-gateway-chat-completions" \
  -H "X-API-KEY: ${APISIX_ADMIN_KEY}"
```

常见原因：

- `APISIX_ADMIN_KEY` 和 `apisix/apisix_conf/config.yaml` 不一致。
- APISIX 还没启动完成。
- `new-api-redis` 所在网络没有正确配置到 `AUTH_NEW_API_DOCKER_NETWORK`。
- 服务器代理不可用，导致 `curlimages/curl:8.11.1` 拉取失败。

可以重新执行初始化服务：

```bash
docker compose --env-file .env up apisix-init
```

如果 `apisix-init` 镜像暂时无法拉取，可以执行第 7 步的备用 `curl -X PUT` 命令手动覆盖写入。

### Java 服务连不上宿主机 MySQL

确认 MySQL 允许当前用户从 Docker 容器访问，并检查宿主机防火墙：

```bash
mysql -h 127.0.0.1 -P 3306 -unew_api -p123456 new_api
```

Linux Docker 环境下，`docker-compose.yml` 已配置：

```text
host.docker.internal:host-gateway
```

### Java 服务提示 JWT 私钥无效

检查 PEM 是否是一行 `\n` 格式：

```bash
grep -E 'GATEWAY_JWT_PRIVATE_KEY|GATEWAY_JWT_PUBLIC_KEY' .env
```

值应该类似：

```text
-----BEGIN PRIVATE KEY-----\n...\n-----END PRIVATE KEY-----\n
```

### Compose 提示 unexpected character "/" in variable name

这是 `.env` 中把 PEM 私钥或公钥按多行写入导致的。`.env` 只能写单行：

```text
GATEWAY_JWT_PRIVATE_KEY=-----BEGIN PRIVATE KEY-----\nMII...\n-----END PRIVATE KEY-----\n
```

不要写成：

```text
GATEWAY_JWT_PRIVATE_KEY=-----BEGIN PRIVATE KEY-----
MII...
-----END PRIVATE KEY-----
```

修复方式：重新执行第 4 步的 Python 写入脚本，然后再检查：

```bash
docker compose --env-file .env config
```

### APISIX 返回凭证解密失败

确认 Java 和 APISIX 使用的是同一个 AES key：

```bash
grep 'GATEWAY_CREDENTIAL_AES_KEY' .env
curl -s "http://127.0.0.1:${APISIX_ADMIN_PORT:-9180}/apisix/admin/routes/model-gateway-chat-completions" \
  -H "X-API-KEY: ${APISIX_ADMIN_KEY}"
```

### APISIX 无法回源到 Java 服务

Compose 内部 route 使用：

```text
http://model-gateway-auth:8080/api/gateway/new-api-credential/ensure
```

检查服务是否在同一个网络：

```bash
docker compose --env-file .env exec apisix curl -i http://model-gateway-auth:8080/api/auth/login
```
