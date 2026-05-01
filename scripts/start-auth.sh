#!/bin/bash
set -euo pipefail

# ============================================================
# model-gateway-auth 认证服务启动脚本
# ============================================================

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
COMPOSE_FILE="$PROJECT_DIR/docker-compose.yml"
ENV_FILE="$PROJECT_DIR/.env"
CERT_DIR="$PROJECT_DIR/cert"

echo "========================================"
echo "启动 model-gateway-auth 认证服务"
echo "========================================"
echo ""

cd "$PROJECT_DIR"

# 1. 检查必要文件
echo "[1/6] 检查必要文件..."

if [[ ! -f "$ENV_FILE" ]]; then
  echo "错误: 环境配置文件不存在: $ENV_FILE"
  echo "请参照 docs/deploy.md 第 3 节创建 .env 文件"
  exit 1
fi

if [[ ! -f "$COMPOSE_FILE" ]]; then
  echo "错误: Compose 文件不存在: $COMPOSE_FILE"
  exit 1
fi

echo "  ✓ .env 已存在"
echo "  ✓ docker-compose.yml 已存在"

# 2. 检查并生成密钥
echo ""
echo "[2/6] 检查密钥文件..."

mkdir -p "$CERT_DIR"

need_generate=false
if [[ ! -f "$CERT_DIR/gateway-jwt-private.pem" ]] || \
   [[ ! -f "$CERT_DIR/gateway-jwt-public.pem" ]] || \
   [[ ! -f "$CERT_DIR/gateway-credential-aes.key" ]] || \
   [[ ! -f "$CERT_DIR/apisix-gateway-secret.txt" ]]; then
  need_generate=true
fi

if [[ "$need_generate" == true ]]; then
  echo "  ! 密钥文件缺失，正在生成..."

  if [[ ! -f "$CERT_DIR/gateway-jwt-private.pem" ]]; then
    openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out "$CERT_DIR/gateway-jwt-private.pem"
    echo "  ✓ 生成 JWT 私钥"
  fi

  if [[ ! -f "$CERT_DIR/gateway-jwt-public.pem" ]]; then
    openssl rsa -pubout -in "$CERT_DIR/gateway-jwt-private.pem" -out "$CERT_DIR/gateway-jwt-public.pem"
    echo "  ✓ 生成 JWT 公钥"
  fi

  if [[ ! -f "$CERT_DIR/gateway-credential-aes.key" ]]; then
    openssl rand -base64 32 > "$CERT_DIR/gateway-credential-aes.key"
    echo "  ✓ 生成 AES 密钥"
  fi

  if [[ ! -f "$CERT_DIR/apisix-gateway-secret.txt" ]]; then
    openssl rand -base64 32 > "$CERT_DIR/apisix-gateway-secret.txt"
    echo "  ✓ 生成 APISIX 回源密钥"
  fi

  echo ""
  echo "  ⚠ 密钥已自动生成，请妥善备份 cert/ 目录下的密钥文件！"
else
  echo "  ✓ 所有密钥文件已存在"
fi

# 3. 检查 Docker 网络
echo ""
echo "[3/6] 检查 Docker 网络..."

NETWORK_NAME="new-api-network"

if ! docker network ls --format '{{.Name}}' | grep -qx "$NETWORK_NAME"; then
  echo "  ✗ 外部 Docker 网络不存在: $NETWORK_NAME"
  echo "    请确认 Redis 容器已启动并加入该网络"
  echo "    可用网络列表:"
  docker network ls --format '    - {{.Name}}' | grep -v '^    - bridge$' | grep -v '^    - host$' | grep -v '^    - none$' || true
  exit 1
fi

echo "  ✓ Docker 网络存在: $NETWORK_NAME"

# 4. 检查 MySQL 和 Redis 连接
echo ""
echo "[4/6] 检查外部依赖..."

SPRING_PROFILE=$(grep -E '^SPRING_PROFILES_ACTIVE=' "$ENV_FILE" | cut -d= -f2- || true)
MYSQL_HOST=$(grep -E '^MYSQL_HOST=' "$ENV_FILE" | cut -d= -f2- || true)
MYSQL_PORT=$(grep -E '^MYSQL_PORT=' "$ENV_FILE" | cut -d= -f2- || true)
MYSQL_DATABASE=$(grep -E '^MYSQL_DATABASE=' "$ENV_FILE" | cut -d= -f2- || true)
MYSQL_USER=$(grep -E '^MYSQL_USERNAME=' "$ENV_FILE" | cut -d= -f2- || true)
MYSQL_PASS=$(grep -E '^MYSQL_PASSWORD=' "$ENV_FILE" | cut -d= -f2- || true)

if [[ -z "$SPRING_PROFILE" ]]; then
  echo "  ✗ SPRING_PROFILES_ACTIVE 未配置，请在 .env 中手动配置 Spring 环境"
  exit 1
fi

if [[ -z "$MYSQL_HOST" ]]; then
  echo "  ✗ MYSQL_HOST 未配置，请在 .env 中手动配置 MySQL 主机地址"
  exit 1
fi

if [[ -z "$MYSQL_PORT" ]]; then
  echo "  ✗ MYSQL_PORT 未配置，请在 .env 中手动配置 MySQL 端口"
  exit 1
fi

if ! [[ "$MYSQL_PORT" =~ ^[0-9]+$ ]]; then
  echo "  ✗ MYSQL_PORT 格式不正确，请在 .env 中配置数字端口"
  exit 1
fi

if [[ -z "$MYSQL_DATABASE" ]]; then
  echo "  ✗ MYSQL_DATABASE 未配置，请在 .env 中手动配置 MySQL 数据库名"
  exit 1
fi

if [[ -z "$MYSQL_USER" ]]; then
  echo "  ✗ MYSQL_USERNAME 未配置，请在 .env 中手动配置 MySQL 用户名"
  exit 1
fi

if [[ -z "$MYSQL_PASS" ]]; then
  echo "  ✗ MYSQL_PASSWORD 未配置，请在 .env 中手动配置 MySQL 密码"
  exit 1
fi

echo -n "  MySQL ($MYSQL_HOST:$MYSQL_PORT) ... "
if timeout 5 bash -c "</dev/tcp/$MYSQL_HOST/$MYSQL_PORT" 2>/dev/null; then
  echo "✓ 可连接"
else
  echo "✗ 无法连接"
  echo "    请确认 MySQL 已启动且防火墙放行端口"
fi

REDIS_HOST=$(grep -E '^REDIS_HOST=' "$ENV_FILE" | cut -d= -f2- || true)
REDIS_PORT=$(grep -E '^REDIS_PORT=' "$ENV_FILE" | cut -d= -f2- || true)
REDIS_PASS=$(grep -E '^REDIS_PASSWORD=' "$ENV_FILE" | cut -d= -f2- || true)
REDIS_DATABASE=$(grep -E '^REDIS_DATABASE=' "$ENV_FILE" | cut -d= -f2- || true)

if [[ -z "$REDIS_HOST" ]]; then
  echo "  ✗ REDIS_HOST 未配置，请在 .env 中手动配置 Redis 主机地址"
  exit 1
fi

if [[ -z "$REDIS_PORT" ]]; then
  REDIS_PORT=6379
fi

if ! [[ "$REDIS_PORT" =~ ^[0-9]+$ ]]; then
  echo "  ✗ REDIS_PORT 格式不正确，请在 .env 中配置数字端口"
  exit 1
fi

if [[ -z "$REDIS_PASS" ]]; then
  echo "  ✗ REDIS_PASSWORD 未配置，请在 .env 中手动配置 Redis 密码"
  exit 1
fi

if [[ -z "$REDIS_DATABASE" ]]; then
  echo "  ✗ REDIS_DATABASE 未配置，请在 .env 中手动配置 Redis 数据库编号"
  exit 1
fi

if ! [[ "$REDIS_DATABASE" =~ ^[0-9]+$ ]]; then
  echo "  ✗ REDIS_DATABASE 格式不正确，请在 .env 中配置数字编号"
  exit 1
fi

echo -n "  Redis ($REDIS_HOST:$REDIS_PORT) ... "
if docker exec "$REDIS_HOST" redis-cli -a "$REDIS_PASS" -p "$REDIS_PORT" ping 2>/dev/null | grep -q PONG; then
  echo "✓ 可连接"
else
  echo "? 未通过 docker exec 检测到（如果 Redis 不在本机 Docker 中，请手动确认）"
fi

NEW_API_USER_MANAGER_BASE_URL=$(grep -E '^NEW_API_USER_MANAGER_BASE_URL=' "$ENV_FILE" | cut -d= -f2- || true)
NEW_API_USER_MANAGER_AUTH_KEY=$(grep -E '^NEW_API_USER_MANAGER_AUTH_KEY=' "$ENV_FILE" | cut -d= -f2- || true)

if [[ -z "$NEW_API_USER_MANAGER_BASE_URL" ]]; then
  echo "  ✗ NEW_API_USER_MANAGER_BASE_URL 未配置，请在 .env 中手动配置 new-api 用户管理地址"
  exit 1
fi

if [[ "$NEW_API_USER_MANAGER_BASE_URL" != http://* && "$NEW_API_USER_MANAGER_BASE_URL" != https://* ]]; then
  echo "  ✗ NEW_API_USER_MANAGER_BASE_URL 格式不正确，应以 http:// 或 https:// 开头"
  exit 1
fi

if [[ -z "$NEW_API_USER_MANAGER_AUTH_KEY" ]]; then
  echo "  ✗ NEW_API_USER_MANAGER_AUTH_KEY 未配置，请在 .env 中手动配置 new-api 用户管理授权码"
  exit 1
fi

GATEWAY_JWT_PRIVATE_KEY_FILE=$(grep -E '^GATEWAY_JWT_PRIVATE_KEY_FILE=' "$ENV_FILE" | cut -d= -f2- || true)
GATEWAY_JWT_PUBLIC_KEY_FILE=$(grep -E '^GATEWAY_JWT_PUBLIC_KEY_FILE=' "$ENV_FILE" | cut -d= -f2- || true)
GATEWAY_CREDENTIAL_KEY_ID=$(grep -E '^GATEWAY_CREDENTIAL_KEY_ID=' "$ENV_FILE" | cut -d= -f2- || true)
GATEWAY_CREDENTIAL_AES_KEY_FILE=$(grep -E '^GATEWAY_CREDENTIAL_AES_KEY_FILE=' "$ENV_FILE" | cut -d= -f2- || true)
APISIX_GATEWAY_SECRET_FILE=$(grep -E '^APISIX_GATEWAY_SECRET_FILE=' "$ENV_FILE" | cut -d= -f2- || true)

if [[ -z "$GATEWAY_JWT_PRIVATE_KEY_FILE" ]]; then
  echo "  ✗ GATEWAY_JWT_PRIVATE_KEY_FILE 未配置，请在 .env 中手动配置 JWT 私钥文件路径"
  exit 1
fi

if [[ -z "$GATEWAY_JWT_PUBLIC_KEY_FILE" ]]; then
  echo "  ✗ GATEWAY_JWT_PUBLIC_KEY_FILE 未配置，请在 .env 中手动配置 JWT 公钥文件路径"
  exit 1
fi

if [[ -z "$GATEWAY_CREDENTIAL_KEY_ID" ]]; then
  echo "  ✗ GATEWAY_CREDENTIAL_KEY_ID 未配置，请在 .env 中手动配置凭证密钥ID"
  exit 1
fi

if [[ -z "$GATEWAY_CREDENTIAL_AES_KEY_FILE" ]]; then
  echo "  ✗ GATEWAY_CREDENTIAL_AES_KEY_FILE 未配置，请在 .env 中手动配置 AES 密钥文件路径"
  exit 1
fi

if [[ -z "$APISIX_GATEWAY_SECRET_FILE" ]]; then
  echo "  ✗ APISIX_GATEWAY_SECRET_FILE 未配置，请在 .env 中手动配置 APISIX 回源密钥文件路径"
  exit 1
fi

# 5. 检查并启动 Docker Compose
echo ""
echo "[5/6] 检查 Compose 配置并启动服务..."

docker compose --env-file "$ENV_FILE" config > /dev/null 2>&1 || {
  echo "  ✗ Compose 配置检查失败，请检查 .env 和 docker-compose.yml"
  docker compose --env-file "$ENV_FILE" config
  exit 1
}
echo "  ✓ Compose 配置检查通过"

echo ""
echo "  正在停止旧的 model-gateway-auth 容器..."
docker compose --env-file "$ENV_FILE" down --remove-orphans

echo ""
echo "  正在启动 model-gateway-auth..."
docker compose --env-file "$ENV_FILE" up -d --build

# 6. 确认状态
echo ""
echo "[6/6] 确认服务状态..."

sleep 2

if docker compose --env-file "$ENV_FILE" ps --format json 2>/dev/null | grep -q '"State":"running"' 2>/dev/null || \
   docker compose --env-file "$ENV_FILE" ps | grep -q 'Up'; then
  echo "  ✓ 服务已启动"
else
  echo "  ! 服务状态异常，请查看日志"
fi

echo ""
echo "========================================"
echo "启动完成"
echo "========================================"
echo ""
echo "查看日志:  docker compose --env-file .env logs -f model-gateway-auth"
echo "停止服务:  docker compose --env-file .env down"
echo ""
