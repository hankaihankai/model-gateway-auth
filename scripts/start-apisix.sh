#!/bin/bash
set -euo pipefail

# ============================================================
# APISIX 启动脚本
# ============================================================

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
APISIX_DIR="$PROJECT_DIR/apisix"
COMPOSE_FILE="$APISIX_DIR/docker-compose.yml"
ENV_FILE="$APISIX_DIR/.env"
CERT_DIR="$PROJECT_DIR/cert"
APISIX_CERT_DIR="$APISIX_DIR/cert"

echo "========================================"
echo "启动 APISIX 网关服务"
echo "========================================"
echo ""

cd "$APISIX_DIR"

# 1. 检查必要文件
echo "[1/7] 检查必要文件..."

if [[ ! -f "$ENV_FILE" ]]; then
  echo "错误: 环境配置文件不存在: $ENV_FILE"
  echo "请参照 docs/deploy.md 第 4 节创建 apisix/.env 文件"
  exit 1
fi

if [[ ! -f "$COMPOSE_FILE" ]]; then
  echo "错误: Compose 文件不存在: $COMPOSE_FILE"
  exit 1
fi

if [[ ! -f "$APISIX_DIR/apisix_conf/config.yaml" ]]; then
  echo "错误: APISIX 配置不存在: apisix/apisix_conf/config.yaml"
  exit 1
fi

if [[ ! -f "$APISIX_DIR/apisix_plugins/model-gateway-auth.lua" ]]; then
  echo "错误: APISIX 插件不存在: apisix/apisix_plugins/model-gateway-auth.lua"
  exit 1
fi

if [[ ! -f "$APISIX_DIR/dashboard_conf/conf.yaml" ]]; then
  echo "警告: Dashboard 配置不存在: apisix/dashboard_conf/conf.yaml"
fi

echo "  ✓ apisix/.env 已存在"
echo "  ✓ apisix/docker-compose.yml 已存在"
echo "  ✓ apisix/apisix_conf/config.yaml 已存在"
echo "  ✓ apisix/apisix_plugins/model-gateway-auth.lua 已存在"

# 2. 检查并同步密钥文件
echo ""
echo "[2/7] 检查密钥文件..."

mkdir -p "$APISIX_CERT_DIR"

missing_certs=false
for file in gateway-jwt-public.pem gateway-credential-aes.key apisix-gateway-secret.txt; do
  if [[ ! -f "$APISIX_CERT_DIR/$file" ]]; then
    missing_certs=true
    break
  fi
done

if [[ "$missing_certs" == true ]]; then
  echo "  ! apisix/cert/ 中缺少密钥文件"

  # 尝试从根目录 cert/ 复制
  if [[ -d "$CERT_DIR" ]]; then
    for file in gateway-jwt-public.pem gateway-credential-aes.key apisix-gateway-secret.txt; do
      if [[ -f "$CERT_DIR/$file" && ! -f "$APISIX_CERT_DIR/$file" ]]; then
        cp "$CERT_DIR/$file" "$APISIX_CERT_DIR/$file"
        echo "  ✓ 从 cert/$file 复制到 apisix/cert/$file"
      fi
    done
  fi

  # 再次检查
  still_missing=false
  for file in gateway-jwt-public.pem gateway-credential-aes.key apisix-gateway-secret.txt; do
    if [[ ! -f "$APISIX_CERT_DIR/$file" ]]; then
      still_missing=true
      echo "  ✗ 仍缺失: apisix/cert/$file"
    fi
  done

  if [[ "$still_missing" == true ]]; then
    echo ""
    echo "请先生成密钥（在项目根目录执行）:"
    echo "  openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out cert/gateway-jwt-private.pem"
    echo "  openssl rsa -pubout -in cert/gateway-jwt-private.pem -out cert/gateway-jwt-public.pem"
    echo "  openssl rand -base64 32 > cert/gateway-credential-aes.key"
    echo "  openssl rand -base64 32 > cert/apisix-gateway-secret.txt"
    echo "  cp cert/gateway-jwt-public.pem apisix/cert/"
    echo "  cp cert/gateway-credential-aes.key apisix/cert/"
    echo "  cp cert/apisix-gateway-secret.txt apisix/cert/"
    exit 1
  fi
else
  echo "  ✓ apisix/cert/ 密钥文件完整"
fi

# 3. 检查 Admin Key 一致性
echo ""
echo "[3/7] 检查 Admin Key 一致性..."

ENV_ADMIN_KEY=$(grep -E '^APISIX_ADMIN_KEY=' "$ENV_FILE" | cut -d= -f2- || true)
CONFIG_ADMIN_KEY=$(awk '
  /admin_key:/ { in_admin = 1; next }
  in_admin && /^[[:space:]]*key:/ {
    sub(/#.*/, "")
    sub(/^[[:space:]]*key:[[:space:]]*/, "")
    gsub(/[[:space:]"]/, "")
    print
    exit
  }
' "$APISIX_DIR/apisix_conf/config.yaml" || true)

if [[ -z "$ENV_ADMIN_KEY" ]]; then
  echo "  ✗ APISIX_ADMIN_KEY 未配置，请在 apisix/.env 中手动配置"
  exit 1
fi

if [[ -z "$CONFIG_ADMIN_KEY" ]]; then
  echo "  ✗ apisix_conf/config.yaml 中未配置 Admin Key"
  exit 1
fi

if [[ -n "$ENV_ADMIN_KEY" && -n "$CONFIG_ADMIN_KEY" && "$ENV_ADMIN_KEY" != "$CONFIG_ADMIN_KEY" ]]; then
  echo "  ✗ .env 中的 APISIX_ADMIN_KEY 与 apisix_conf/config.yaml 不一致"
  echo "    .env:        $ENV_ADMIN_KEY"
  echo "    config.yaml: $CONFIG_ADMIN_KEY"
  exit 1
fi

echo "  ✓ Admin Key 一致"

AUTH_REDIS_URL=$(grep -E '^AUTH_REDIS_URL=' "$ENV_FILE" | cut -d= -f2- || true)
if [[ -z "$AUTH_REDIS_URL" ]]; then
  echo "  ✗ AUTH_REDIS_URL 未配置，请在 apisix/.env 中手动配置"
  exit 1
fi

if [[ "$AUTH_REDIS_URL" != redis://* && "$AUTH_REDIS_URL" != rediss://* ]]; then
  echo "  ✗ AUTH_REDIS_URL 格式不正确，应类似 redis://:password@host:6379/0"
  exit 1
fi

# 4. 检查 Docker 网络
echo ""
echo "[4/7] 检查 Docker 网络..."

NETWORK_NAME="new-api-network"

if ! docker network ls --format '{{.Name}}' | grep -qx "$NETWORK_NAME"; then
  echo "  ✗ 外部 Docker 网络不存在: $NETWORK_NAME"
  echo "    请确认 Redis 容器已启动并加入该网络"
  echo "    可用网络列表:"
  docker network ls --format '    - {{.Name}}' | grep -v '^    - bridge$' | grep -v '^    - host$' | grep -v '^    - none$' || true
  exit 1
fi
echo "  ✓ 外部 Docker 网络存在: $NETWORK_NAME"

# 5. 检查 Compose 配置
echo ""
echo "[5/7] 检查 Compose 配置..."

docker compose --env-file "$ENV_FILE" config > /dev/null 2>&1 || {
  echo "  ✗ Compose 配置检查失败"
  docker compose --env-file "$ENV_FILE" config
  exit 1
}
echo "  ✓ Compose 配置检查通过"

# 6. 启动服务
echo ""
echo "[6/7] 启动 APISIX 及相关服务..."

echo "  正在停止旧的 APISIX 容器..."
docker compose --env-file "$ENV_FILE" down --remove-orphans

echo ""
echo "  正在启动 APISIX、etcd、Dashboard..."
docker compose --env-file "$ENV_FILE" up -d

echo "  ✓ APISIX、etcd、Dashboard 已启动"

# 7. 等待并确认路由初始化
echo ""
echo "[7/7] 等待路由初始化..."

APISIX_ADMIN_KEY=$(grep -E '^APISIX_ADMIN_KEY=' "$ENV_FILE" | cut -d= -f2-)
APISIX_ADMIN_PORT=$(grep -E '^APISIX_ADMIN_PORT=' "$ENV_FILE" | cut -d= -f2- || true)
APISIX_ADMIN_PORT=${APISIX_ADMIN_PORT:-9180}
APISIX_PORT=$(grep -E '^APISIX_PORT=' "$ENV_FILE" | cut -d= -f2- || true)
APISIX_PORT=${APISIX_PORT:-9080}
APISIX_DASHBOARD_PORT=$(grep -E '^APISIX_DASHBOARD_PORT=' "$ENV_FILE" | cut -d= -f2- || true)
APISIX_DASHBOARD_PORT=${APISIX_DASHBOARD_PORT:-9000}

# 等待 apisix-init 完成
for i in $(seq 1 60); do
  init_status=$(docker inspect -f '{{.State.ExitCode}}' apisix-init 2>/dev/null || echo "running")
  if [[ "$init_status" == "0" ]]; then
    echo "  ✓ 路由初始化成功"
    break
  elif [[ "$init_status" != "running" && "$init_status" != "null" ]]; then
    echo "  ✗ 路由初始化失败 (exit code: $init_status)"
    echo "    查看日志: docker compose --env-file .env logs apisix-init"
    break
  fi
  if [[ $i -eq 1 ]]; then
    echo -n "  等待 apisix-init 完成"
  else
    echo -n "."
  fi
  sleep 2
done

# 验证路由
if curl -fsS "http://127.0.0.1:${APISIX_ADMIN_PORT}/apisix/admin/routes/model-gateway-auth-api" \
  -H "X-API-KEY: ${APISIX_ADMIN_KEY}" > /dev/null 2>&1; then
  echo "  ✓ 认证服务路由已创建: /model-gateway-auth/*"
else
  echo "  ! 认证服务路由未检测到"
fi

if curl -fsS "http://127.0.0.1:${APISIX_ADMIN_PORT}/apisix/admin/routes/model-gateway-chat-completions" \
  -H "X-API-KEY: ${APISIX_ADMIN_KEY}" > /dev/null 2>&1; then
  echo "  ✓ 聊天路由已创建: /v1/chat/completions"
else
  echo "  ! 聊天路由未检测到"
fi

echo ""
echo "========================================"
echo "启动完成"
echo "========================================"
echo ""
echo "服务地址:"
echo "  APISIX 网关:     http://127.0.0.1:${APISIX_PORT}"
echo "  APISIX Admin:    http://127.0.0.1:${APISIX_ADMIN_PORT}"
echo "  APISIX Dashboard: http://127.0.0.1:${APISIX_DASHBOARD_PORT:-9000}"
echo ""
echo "常用命令:"
echo "  查看日志:  docker compose --env-file .env logs -f apisix"
echo "  停止服务:  docker compose --env-file .env down"
echo ""
