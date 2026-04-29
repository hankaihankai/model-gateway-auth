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

NETWORK_NAME=$(grep -E '^AUTH_NEW_API_DOCKER_NETWORK=' "$ENV_FILE" | cut -d= -f2- | tr -d ' \t' || true)
NETWORK_NAME=${NETWORK_NAME:-new-api_default}

if ! docker network ls --format '{{.Name}}' | grep -qx "$NETWORK_NAME"; then
  echo "  ✗ 外部 Docker 网络不存在: $NETWORK_NAME"
  echo "    请确认 Redis 容器已启动并加入该网络，或修改 .env 中的 AUTH_NEW_API_DOCKER_NETWORK"
  echo "    可用网络列表:"
  docker network ls --format '    - {{.Name}}' | grep -v '^    - bridge$' | grep -v '^    - host$' | grep -v '^    - none$' || true
  exit 1
fi

echo "  ✓ Docker 网络存在: $NETWORK_NAME"

# 4. 检查 MySQL 和 Redis 连接
echo ""
echo "[4/6] 检查外部依赖..."

MYSQL_URL=$(grep -E '^AUTH_MYSQL_URL=' "$ENV_FILE" | cut -d= -f2- || true)
MYSQL_USER=$(grep -E '^AUTH_MYSQL_USERNAME=' "$ENV_FILE" | cut -d= -f2- || true)
MYSQL_PASS=$(grep -E '^AUTH_MYSQL_PASSWORD=' "$ENV_FILE" | cut -d= -f2- || true)
MYSQL_USER=${MYSQL_USER:-new_api}
MYSQL_PASS=${MYSQL_PASS:-123456}

# 尝试从 JDBC URL 提取 host:port
if [[ -n "$MYSQL_URL" ]]; then
  MYSQL_HOSTPORT=$(echo "$MYSQL_URL" | sed -n 's/.*mysql:\/\/\([^/]*\).*/\1/p')
  MYSQL_HOST=${MYSQL_HOSTPORT%:*}
  MYSQL_PORT=${MYSQL_HOSTPORT##*:}
  MYSQL_HOST=${MYSQL_HOST:-127.0.0.1}
  MYSQL_PORT=${MYSQL_PORT:-3306}
else
  MYSQL_HOST=127.0.0.1
  MYSQL_PORT=3306
fi

echo -n "  MySQL ($MYSQL_HOST:$MYSQL_PORT) ... "
if timeout 5 bash -c "</dev/tcp/$MYSQL_HOST/$MYSQL_PORT" 2>/dev/null; then
  echo "✓ 可连接"
else
  echo "✗ 无法连接"
  echo "    请确认 MySQL 已启动且防火墙放行端口"
fi

REDIS_URL=$(grep -E '^AUTH_REDIS_URL=' "$ENV_FILE" | cut -d= -f2- || true)
if [[ -n "$REDIS_URL" ]]; then
  REDIS_HOST=$(echo "$REDIS_URL" | sed -n 's/.*@\([^:@]*\):.*/\1/p')
  REDIS_PORT=$(echo "$REDIS_URL" | sed -n 's/.*:\([0-9]*\)\/.*/\1/p')
  REDIS_HOST=${REDIS_HOST:-new-api-redis}
  REDIS_PORT=${REDIS_PORT:-6379}
else
  REDIS_HOST=new-api-redis
  REDIS_PORT=6379
fi

echo -n "  Redis ($REDIS_HOST:$REDIS_PORT) ... "
if docker exec "$REDIS_HOST" redis-cli -p "$REDIS_PORT" ping 2>/dev/null | grep -q PONG; then
  echo "✓ 可连接"
else
  echo "? 未通过 docker exec 检测到（如果 Redis 不在本机 Docker 中，请手动确认）"
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
