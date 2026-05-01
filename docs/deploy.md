# Docker 发布文档

本文档使用两个独立的 Compose 文件部署：

- 根目录 `docker-compose.yml`：只启动 `model-gateway-auth` 认证服务。
- `apisix/docker-compose.yml`：启动 APISIX、etcd、Dashboard，并通过 `apisix-init` 自动配置 APISIX 路由。

MySQL 和 Redis 不由本项目创建。认证服务通过 MySQL JDBC URL 和 Redis URL 连接外部服务，APISIX 也通过 Redis URL 访问同一个 Redis。APISIX 和认证服务后续可以分开部署，分开部署时只需要让各自 `.env` 中的 URL 地址对当前服务可达。

## 1. 上传代码

把整个项目上传到服务器，例如：

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
cert/.gitkeep
apisix/docker-compose.yml
apisix/.env
apisix/cert/.gitkeep
apisix/apisix_conf/config.yaml
apisix/apisix_plugins/model-gateway-auth.lua
apisix/dashboard_conf/conf.yaml
```

进入项目根目录：

```bash
cd /opt/model-gateway-auth
```

## 2. 确认外部 MySQL 和 Redis

MySQL 默认通过 JDBC URL 连接：

```text
url: jdbc:mysql://host.docker.internal:3306/new_api?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
username: new_api
password: 123456
```

Redis 默认通过 URL 复用已有 Docker 容器：

```text
url: redis://:123456@new-api-redis:6379/0
```

确认 Redis 容器已加入固定 Docker 网络 `new-api-network`：

```bash
docker network inspect new-api-network
```

如果网络不存在，先创建：

```bash
docker network create new-api-network
```

如果 MySQL 未初始化业务表，执行：

```bash
mysql -h 127.0.0.1 -P 3306 -unew_api -p123456 new_api < docs/database.sql
```

## 3. 配置根目录 `.env`

根目录 `.env` 只负责认证服务启动：

```env
AUTH_MYSQL_URL=jdbc:mysql://host.docker.internal:3306/new_api?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
AUTH_MYSQL_USERNAME=new_api
AUTH_MYSQL_PASSWORD=123456

AUTH_REDIS_URL=redis://:123456@new-api-redis:6379/0

AUTH_PORT=8188

GATEWAY_JWT_PRIVATE_KEY_FILE=/app/cert/gateway-jwt-private.pem
GATEWAY_JWT_PUBLIC_KEY_FILE=/app/cert/gateway-jwt-public.pem
GATEWAY_CREDENTIAL_KEY_ID=main
GATEWAY_CREDENTIAL_AES_KEY_FILE=/app/cert/gateway-credential-aes.key
APISIX_GATEWAY_SECRET_FILE=/app/cert/apisix-gateway-secret.txt
```

## 4. 配置 `apisix/.env`

`apisix/.env` 只负责 APISIX 和路由初始化：

```env
APISIX_PORT=9080
APISIX_ADMIN_PORT=9180
APISIX_METRICS_PORT=9091
APISIX_DASHBOARD_PORT=9000

APISIX_ADMIN_KEY=api-admin-key-1234567654321
NEW_API_NODE=120.53.240.51:3000
AUTH_API_NODE=host.docker.internal:8188
AUTH_API_ROUTE_PREFIX=/model-gateway-auth
APISIX_CREDENTIAL_ENSURE_URL=http://host.docker.internal:8188/api/gateway/new-api-credential/ensure

AUTH_REDIS_URL=redis://:123456@new-api-redis:6379/0

GATEWAY_JWT_PUBLIC_KEY_FILE=/cert/gateway-jwt-public.pem
GATEWAY_CREDENTIAL_KEY_ID=main
GATEWAY_CREDENTIAL_AES_KEY_FILE=/cert/gateway-credential-aes.key
APISIX_GATEWAY_SECRET_FILE=/cert/apisix-gateway-secret.txt
```

注意：

- `APISIX_ADMIN_KEY` 必须和 `apisix/apisix_conf/config.yaml` 中的 Admin Key 保持一致。
- APISIX 不需要 JWT 私钥，不要把私钥文件复制到 `apisix/cert/`。
- `GATEWAY_JWT_PUBLIC_KEY_FILE`、`GATEWAY_CREDENTIAL_AES_KEY_FILE`、`APISIX_GATEWAY_SECRET_FILE` 指向 APISIX 容器内的文件路径。
- `AUTH_API_NODE` 是 APISIX 转发认证服务 API 的上游节点。
- `AUTH_API_ROUTE_PREFIX` 是认证服务项目对外路由前缀，默认 `/model-gateway-auth`，APISIX 会去掉此前缀后原样转发到认证服务。
- 同机双 Compose 默认使用 `host.docker.internal` 回调认证服务；分开部署时把 `APISIX_CREDENTIAL_ENSURE_URL` 改成 APISIX 能访问到的认证服务地址。
- `AUTH_REDIS_URL` 是 APISIX 访问 Redis 的完整地址，格式为 `redis://[:password@]host:port/database`；密码包含特殊字符时需要 URL 编码。
- APISIX 和认证服务固定复用外部 Docker 网络 `new-api-network` 访问 `new-api-redis`。

## 5. 生成密钥

创建密钥目录：

```bash
mkdir -p cert apisix/cert
```

生成 JWT 私钥和公钥，私钥只保存在根目录 `cert/`：

```bash
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out cert/gateway-jwt-private.pem
openssl rsa -pubout -in cert/gateway-jwt-private.pem -out cert/gateway-jwt-public.pem
```

生成 AES 密钥和 APISIX 回源密钥：

```bash
openssl rand -base64 32 > cert/gateway-credential-aes.key
openssl rand -base64 32 > cert/apisix-gateway-secret.txt
```

把 APISIX 需要的非私钥文件复制到 `apisix/cert/`：

```bash
cp cert/gateway-jwt-public.pem apisix/cert/gateway-jwt-public.pem
cp cert/gateway-credential-aes.key apisix/cert/gateway-credential-aes.key
cp cert/apisix-gateway-secret.txt apisix/cert/apisix-gateway-secret.txt
```

检查密钥文件存在：

```bash
ls -l cert
ls -l apisix/cert
```

注意：`cert/` 和 `apisix/cert/` 里的真实密钥文件不提交到 Git。

## 6. 启动认证服务

在项目根目录检查 Compose 配置：

```bash
docker compose --env-file .env config
```

启动认证服务：

```bash
docker compose --env-file .env up -d --build
```

确认认证服务状态：

```bash
docker compose --env-file .env ps
docker compose --env-file .env logs --tail=100 model-gateway-auth
```

根目录 Compose 只应该包含 `model-gateway-auth`。

## 7. 启动 APISIX

进入 APISIX 目录：

```bash
cd /opt/model-gateway-auth/apisix
```

检查 Compose 配置：

```bash
docker compose --env-file .env config
```

启动 APISIX：

```bash
docker compose --env-file .env up -d
```

这个命令会：

1. 启动 APISIX 依赖的 etcd。
2. 启动 APISIX 和 Dashboard。
3. 通过 `apisix-init` 自动创建 APISIX 路由：
   `/model-gateway-auth/* -> AUTH_API_NODE/*`
   `/v1/chat/completions -> NEW_API_NODE/v1/chat/completions`

确认服务状态：

```bash
docker compose --env-file .env ps
docker compose --env-file .env logs --tail=100 apisix
docker compose --env-file .env logs --tail=100 apisix-init
```

`apisix-init` 日志中应看到：

```text
APISIX routes model-gateway-auth-api and model-gateway-chat-completions configured
```

## 8. 确认 APISIX 路由

在 `apisix` 目录读取 Admin 参数：

```bash
APISIX_ADMIN_KEY=$(grep -E '^APISIX_ADMIN_KEY=' .env | cut -d= -f2-)
APISIX_ADMIN_PORT=$(grep -E '^APISIX_ADMIN_PORT=' .env | cut -d= -f2-)
APISIX_ADMIN_PORT=${APISIX_ADMIN_PORT:-9180}
APISIX_PORT=$(grep -E '^APISIX_PORT=' .env | cut -d= -f2-)
APISIX_PORT=${APISIX_PORT:-9080}
```

确认 APISIX Admin API 可访问：

```bash
curl -i "http://127.0.0.1:${APISIX_ADMIN_PORT}/apisix/admin/routes" \
  -H "X-API-KEY: ${APISIX_ADMIN_KEY}"
```

查看自动创建的路由：

```bash
curl -s "http://127.0.0.1:${APISIX_ADMIN_PORT}/apisix/admin/routes/model-gateway-auth-api" \
  -H "X-API-KEY: ${APISIX_ADMIN_KEY}"

curl -s "http://127.0.0.1:${APISIX_ADMIN_PORT}/apisix/admin/routes/model-gateway-chat-completions" \
  -H "X-API-KEY: ${APISIX_ADMIN_KEY}"
```

路由中的 `credential_ensure_url` 应等于 `apisix/.env` 里的 `APISIX_CREDENTIAL_ENSURE_URL`。

如果需要手动配置认证服务路由，在 `apisix` 目录执行：

```bash
AUTH_API_NODE=$(grep -E '^AUTH_API_NODE=' .env | cut -d= -f2-)
AUTH_API_NODE=${AUTH_API_NODE:-host.docker.internal:8188}
AUTH_API_ROUTE_PREFIX=$(grep -E '^AUTH_API_ROUTE_PREFIX=' .env | cut -d= -f2-)
AUTH_API_ROUTE_PREFIX=${AUTH_API_ROUTE_PREFIX:-/model-gateway-auth}

cat > /tmp/model-gateway-auth-api-route.json <<EOF
{
  "name": "model-gateway-auth-api",
  "uri": "${AUTH_API_ROUTE_PREFIX}/*",
  "methods": ["GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"],
  "plugins": {
    "proxy-rewrite": {
      "regex_uri": ["^${AUTH_API_ROUTE_PREFIX}/(.*)", "/\$1"]
    }
  },
  "upstream": {
    "type": "roundrobin",
    "scheme": "http",
    "pass_host": "node",
    "nodes": {
      "${AUTH_API_NODE}": 1
    }
  }
}
EOF

curl -i -X PUT "http://127.0.0.1:${APISIX_ADMIN_PORT}/apisix/admin/routes/model-gateway-auth-api" \
  -H "X-API-KEY: ${APISIX_ADMIN_KEY}" \
  -H "Content-Type: application/json" \
  --data-binary @/tmp/model-gateway-auth-api-route.json
```

认证服务项目路由不要配置 `model-gateway-auth` 插件，否则登录接口会被认证插件拦截。也不要直接使用 `/api/*` 作为认证服务路由，后续其他服务也可能使用 `/api` 前缀，应该使用 `/model-gateway-auth/*` 这类项目级前缀，再去掉此前缀后原样转发到后端。

如果需要手动配置 new-api 聊天路由，在 `apisix` 目录执行：

```bash
NEW_API_NODE=$(grep -E '^NEW_API_NODE=' .env | cut -d= -f2-)
NEW_API_NODE=${NEW_API_NODE:-120.53.240.51:3000}
APISIX_CREDENTIAL_ENSURE_URL=$(grep -E '^APISIX_CREDENTIAL_ENSURE_URL=' .env | cut -d= -f2-)
APISIX_CREDENTIAL_ENSURE_URL=${APISIX_CREDENTIAL_ENSURE_URL:-http://host.docker.internal:8188/api/gateway/new-api-credential/ensure}
AUTH_REDIS_URL=$(grep -E '^AUTH_REDIS_URL=' .env | cut -d= -f2-)
AUTH_REDIS_URL=${AUTH_REDIS_URL:-redis://:123456@new-api-redis:6379/0}
GATEWAY_CREDENTIAL_KEY_ID=$(grep -E '^GATEWAY_CREDENTIAL_KEY_ID=' .env | cut -d= -f2-)
GATEWAY_CREDENTIAL_KEY_ID=${GATEWAY_CREDENTIAL_KEY_ID:-main}

APISIX_JWT_PUBLIC_KEY=$(awk '{printf "%s\\n", $0}' cert/gateway-jwt-public.pem)
APISIX_AES_KEY=$(tr -d '\r\n' < cert/gateway-credential-aes.key)
APISIX_GATEWAY_SECRET=$(tr -d '\r\n' < cert/apisix-gateway-secret.txt)

cat > /tmp/model-gateway-chat-route.json <<EOF
{
  "name": "model-gateway-chat-completions",
  "uri": "/v1/chat/completions",
  "methods": ["POST"],
  "plugins": {
    "model-gateway-auth": {
      "jwt_public_key": "${APISIX_JWT_PUBLIC_KEY}",
      "jwt_issuer": "model-gateway-auth",
      "jwt_audience": "apisix-llm-gateway",
      "redis_url": "${AUTH_REDIS_URL}",
      "credential_ensure_url": "${APISIX_CREDENTIAL_ENSURE_URL}",
      "gateway_secret": "${APISIX_GATEWAY_SECRET}",
      "aes_keys": {
        "${GATEWAY_CREDENTIAL_KEY_ID}": "${APISIX_AES_KEY}"
      }
    }
  },
  "upstream": {
    "type": "roundrobin",
    "scheme": "http",
    "pass_host": "node",
    "nodes": {
      "${NEW_API_NODE}": 1
    }
  }
}
EOF

curl -i -X PUT "http://127.0.0.1:${APISIX_ADMIN_PORT}/apisix/admin/routes/model-gateway-chat-completions" \
  -H "X-API-KEY: ${APISIX_ADMIN_KEY}" \
  -H "Content-Type: application/json" \
  --data-binary @/tmp/model-gateway-chat-route.json
```

JWT公钥、AES密钥和APISIX回源密钥只会被配置到挂载 `model-gateway-auth` 插件的路由上。当前是 `/v1/chat/completions`；后续新增需要统一鉴权和new-api凭证注入的路由时，复用同一套插件配置即可，不需要每个路由生成一套新密钥。

## 9. 创建用户和 new-api 绑定

回到项目根目录：

```bash
cd /opt/model-gateway-auth
```

创建业务用户：

```bash
curl -X POST http://127.0.0.1:${APISIX_PORT:-9080}/model-gateway-auth/api/admin/users/registerUser \
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

如果服务器 MySQL 使用 `root` 账号：

```bash
mysql -h 127.0.0.1 -P 3306 -uroot -p123456 new_api
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

`new_api_api_key` 需要替换成真实 new-api API Key，`status` 必须是 `ENABLE`。

## 10. 登录并确认 Redis 凭证

登录：

```bash
LOGIN_RESPONSE=$(curl -s -X POST http://127.0.0.1:${APISIX_PORT:-9080}/model-gateway-auth/api/auth/login \
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

## 11. 调用 APISIX

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

## 12. 停止和重启

停止认证服务：

```bash
cd /opt/model-gateway-auth
docker compose --env-file .env down
```

停止 APISIX：

```bash
cd /opt/model-gateway-auth/apisix
docker compose --env-file .env down
```

重启认证服务：

```bash
cd /opt/model-gateway-auth
docker compose --env-file .env up -d --build
```

重启 APISIX：

```bash
cd /opt/model-gateway-auth/apisix
docker compose --env-file .env up -d
```

## 13. 分开部署说明

如果 APISIX 和认证服务部署在不同机器：

- 根目录 `.env` 保持认证服务自己的 MySQL URL、Redis URL 和密钥文件路径。
- 认证服务部署目录保留完整 `cert/`，其中包含 JWT 私钥。
- APISIX 部署目录只需要 `apisix/cert/` 中的 JWT 公钥、AES key 和回源密钥，不需要 JWT 私钥。
- `apisix/.env` 中的 `AUTH_API_NODE` 改成 APISIX 能访问的认证服务节点，例如 `10.0.0.12:8188`。
- `apisix/.env` 中的 `APISIX_CREDENTIAL_ENSURE_URL` 改成 APISIX 能访问的认证服务地址，例如：

```text
APISIX_CREDENTIAL_ENSURE_URL=http://10.0.0.12:8188/api/gateway/new-api-credential/ensure
```

- `apisix/.env` 中的 `AUTH_REDIS_URL` 改成 APISIX 能访问的 Redis 地址。
- `apisix/cert/gateway-jwt-public.pem`、`apisix/cert/gateway-credential-aes.key`、`apisix/cert/apisix-gateway-secret.txt` 仍然必须和认证服务侧对应文件保持一致。

## 14. 常见问题

### Compose 提示 external network not found

说明固定外部网络 `new-api-network` 不存在，重新确认：

```bash
docker network inspect new-api-network
```

根目录和 `apisix/.env` 都要同步修改。

### APISIX route 没有创建

先查看 `apisix-init` 日志：

```bash
cd /opt/model-gateway-auth/apisix
docker compose --env-file .env logs apisix-init
```

常见原因：

- `APISIX_ADMIN_KEY` 和 `apisix/apisix_conf/config.yaml` 不一致。
- APISIX 还没启动完成。
- `apisix/cert/` 中缺少 JWT 公钥、AES key 或回源密钥文件。
- `docker.m.daocloud.io/curlimages/curl:8.11.1` 镜像暂时无法拉取。

可以重新执行初始化服务：

```bash
docker compose --env-file .env up apisix-init
```

### APISIX 无法回调认证服务

检查 `apisix/.env`：

```bash
grep '^APISIX_CREDENTIAL_ENSURE_URL=' apisix/.env
```

同机双 Compose 默认应为：

```text
APISIX_CREDENTIAL_ENSURE_URL=http://host.docker.internal:8188/api/gateway/new-api-credential/ensure
```

分开部署时改成认证服务真实地址，并确认服务器防火墙允许 APISIX 访问认证服务端口。

### APISIX 返回凭证解密失败

确认根目录 `cert/` 和 `apisix/cert/` 使用同一个 AES key：

```bash
diff cert/gateway-credential-aes.key apisix/cert/gateway-credential-aes.key
```

### Java 服务提示 JWT 私钥无效

检查私钥文件是否存在且是PKCS8 PEM格式：

```bash
ls -l cert/gateway-jwt-private.pem
head -n 1 cert/gateway-jwt-private.pem
```

第一行应该是：

```text
-----BEGIN PRIVATE KEY-----
```
