#!/bin/bash
set -euo pipefail

# ============================================================
# model-gateway-auth-front 前端服务启动脚本
# ============================================================

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
COMPOSE_FILE="$PROJECT_DIR/docker-compose.yml"
ENV_FILE="$PROJECT_DIR/.env"
DOCKERFILE="$PROJECT_DIR/Dockerfile"
NGINX_TEMPLATE="$PROJECT_DIR/nginx/default.conf.template"

echo "========================================"
echo "启动 model-gateway-auth-front 前端服务"
echo "========================================"
echo ""

cd "$PROJECT_DIR"

echo "[1/5] 检查必要文件..."

if [[ ! -f "$ENV_FILE" ]]; then
  echo "错误: 环境配置文件不存在: $ENV_FILE"
  echo "请在 model-gateway-auth-front 目录下创建 .env 文件"
  exit 1
fi

if [[ ! -f "$COMPOSE_FILE" ]]; then
  echo "错误: Compose 文件不存在: $COMPOSE_FILE"
  exit 1
fi

if [[ ! -f "$DOCKERFILE" ]]; then
  echo "错误: Dockerfile 不存在: $DOCKERFILE"
  exit 1
fi

if [[ ! -f "$NGINX_TEMPLATE" ]]; then
  echo "错误: Nginx 配置模板不存在: $NGINX_TEMPLATE"
  exit 1
fi

echo "  ✓ .env 已存在"
echo "  ✓ docker-compose.yml 已存在"
echo "  ✓ Dockerfile 已存在"
echo "  ✓ Nginx 配置模板已存在"

echo ""
echo "[2/5] 检查 Docker..."

if ! docker version > /dev/null 2>&1; then
  echo "  ✗ Docker 不可用，请先启动 Docker"
  exit 1
fi

echo "  ✓ Docker 可用"

echo ""
echo "[3/5] 检查 Docker 网络..."

NETWORK_NAME="new-api-network"

if ! docker network ls --format '{{.Name}}' | grep -qx "$NETWORK_NAME"; then
  echo "  ✗ 外部 Docker 网络不存在: $NETWORK_NAME"
  echo "    请先启动 new-api / APISIX 相关服务，并确认它们加入该网络"
  echo "    可用网络列表:"
  docker network ls --format '    - {{.Name}}' | grep -v '^    - bridge$' | grep -v '^    - host$' | grep -v '^    - none$' || true
  exit 1
fi

echo "  ✓ Docker 网络存在: $NETWORK_NAME"

echo ""
echo "[4/5] 检查 Compose 配置..."

docker compose --env-file "$ENV_FILE" config > /dev/null 2>&1 || {
  echo "  ✗ Compose 配置检查失败，请检查 .env 和 docker-compose.yml"
  docker compose --env-file "$ENV_FILE" config
  exit 1
}

echo "  ✓ Compose 配置检查通过"

echo ""
echo "[5/5] 构建并启动前端服务..."

docker compose --env-file "$ENV_FILE" up -d --build

FRONT_PORT=$(grep -E '^FRONT_PORT=' "$ENV_FILE" | cut -d= -f2- || true)
if [[ -z "$FRONT_PORT" ]]; then
  FRONT_PORT=8000
fi

echo ""
echo "========================================"
echo "启动完成"
echo "========================================"
echo ""
echo "访问地址:  http://localhost:$FRONT_PORT"
echo "查看日志:  docker compose --env-file .env logs -f model-gateway-auth-front"
echo "停止服务:  docker compose --env-file .env down"
echo ""
