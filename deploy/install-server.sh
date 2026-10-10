#!/usr/bin/env bash
# =============================================================================
#  一键部署脚本（在全新的 Ubuntu 22.04 云服务器上以 root 执行）
#
#  用法：
#    1) 把项目放到服务器： git clone <你的仓库地址> ~/aiuse
#    2) cd ~/aiuse && sudo bash deploy/install-server.sh
#    3) 按提示填写数据库密码、百度密钥，脚本自动构建并启动
#
#  脚本做了什么：安装 Docker → 生成 .env（密钥不进仓库）→ 构建并启动应用与 MySQL
#               → 放行本机防火墙 → 自检并打印访问地址
# =============================================================================
set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
APP_PORT="${APP_PORT:-8081}"

log() { echo -e "\033[32m[deploy]\033[0m $*"; }
warn() { echo -e "\033[33m[warn]\033[0m $*"; }
err() { echo -e "\033[31m[error]\033[0m $*" >&2; }

if [[ "$(id -u)" -ne 0 ]]; then
  err "请用 root 运行：sudo bash deploy/install-server.sh"
  exit 1
fi

# ---------- 1. 安装依赖 ----------
log "检查并安装 Docker ..."
if ! command -v docker >/dev/null 2>&1; then
  apt-get update -y
  apt-get install -y ca-certificates curl gnupg git
  install -m 0755 -d /etc/apt/keyrings
  curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
  chmod a+r /etc/apt/keyrings/docker.asc
  echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] https://download.docker.com/linux/ubuntu $(. /etc/os-release && echo "$VERSION_CODENAME") stable" \
    > /etc/apt/sources.list.d/docker.list
  apt-get update -y
  apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
else
  log "Docker 已安装，跳过"
fi

if [[ ! -f /etc/docker/daemon.json ]]; then
  mkdir -p /etc/docker
  cat > /etc/docker/daemon.json <<'JSON'
{
  "registry-mirrors": ["https://docker.m.daocloud.io"]
}
JSON
  systemctl restart docker || warn "docker 重启失败，忽略"
fi

# ---------- 2. 收集配置 ----------
ENV_FILE="${PROJECT_DIR}/.env"
if [[ -f "$ENV_FILE" ]]; then
  log "发现已有 .env，沿用其中配置（要修改请编辑后重新运行）"
else
  echo
  log "请填写配置（直接回车使用括号里的默认值）"
  read -rp "MySQL root 密码 (medreminder): " DB_PASSWORD
  DB_PASSWORD="${DB_PASSWORD:-medreminder}"
  read -rp "百度 OCR API Key (可留空，影响拍照识药): " BAIDU_AK
  read -rp "百度 OCR Secret Key (可留空): " BAIDU_SK
  read -rp "是否允许自助注册账号 (true/false) [true]: " ALLOW_REGISTER
  ALLOW_REGISTER="${ALLOW_REGISTER:-true}"

  cat > "$ENV_FILE" <<EOF
# 由 deploy/install-server.sh 生成，不要提交到 Git
MYSQL_ROOT_PASSWORD=${DB_PASSWORD}
BAIDU_OCR_API_KEY=${BAIDU_AK}
BAIDU_OCR_SECRET_KEY=${BAIDU_SK}
ALLOW_REGISTER=${ALLOW_REGISTER}
APP_PORT=${APP_PORT}
EOF
  chmod 600 "$ENV_FILE"
  log "配置已写入 .env（权限 600，仅 root 可读）"
fi

# ---------- 3. 构建并启动 ----------
cd "$PROJECT_DIR"
log "开始构建镜像并启动（第一次约 5~10 分钟，请耐心等待）..."
docker compose --env-file "$ENV_FILE" up -d --build

# ---------- 4. 放行防火墙 ----------
if command -v ufw >/dev/null 2>&1 && ufw status | grep -q "Status: active"; then
  ufw allow "${APP_PORT}/tcp" >/dev/null 2>&1 || true
  log "已放行 ufw 端口 ${APP_PORT}"
fi

# ---------- 5. 自检并输出结果 ----------
log "等待服务启动 ..."
for _ in $(seq 1 60); do
  if curl -fsS "http://127.0.0.1:${APP_PORT}/api/health" >/dev/null 2>&1; then
    break
  fi
  sleep 3
done

PUBLIC_IP="$(curl -fsS --max-time 5 https://api.ipify.org 2>/dev/null || hostname -I | awk '{print $1}')"
echo
if curl -fsS "http://127.0.0.1:${APP_PORT}/api/health" >/dev/null 2>&1; then
  log "部署成功！"
  echo "    访问地址：http://${PUBLIC_IP}:${APP_PORT}/"
  echo "    健康检查：http://${PUBLIC_IP}:${APP_PORT}/api/health"
  echo
  echo "  如果打不开，请到云厂商控制台的【安全组 / 防火墙】放行 TCP ${APP_PORT} 端口。"
else
  err "服务似乎没起来，查看日志： docker compose logs -f app"
  exit 1
fi

echo
echo "  常用命令："
echo "    查看日志： docker compose logs -f app"
echo "    重启服务： docker compose restart app"
echo "    更新代码： git pull && docker compose up -d --build"
echo "    停止全部： docker compose down      （数据保留在数据卷里）"

