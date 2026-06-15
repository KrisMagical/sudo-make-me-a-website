#!/usr/bin/env bash
set -euo pipefail

APP_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

info() { echo -e "${GREEN}$*${NC}"; }
warn() { echo -e "${YELLOW}$*${NC}"; }
fail() { echo -e "${RED}$*${NC}"; exit 1; }

ask() {
  local prompt="$1"
  local default="${2:-}"
  local value
  if [[ -n "$default" ]]; then
    read -r -p "$prompt [$default]: " value
    echo "${value:-$default}"
  else
    read -r -p "$prompt: " value
    echo "$value"
  fi
}

ask_secret() {
  local prompt="$1"
  local value
  read -r -s -p "$prompt: " value
  echo
  echo "$value"
}

require_command() {
  command -v "$1" >/dev/null 2>&1 || fail "Missing required command: $1"
}

write_kv_file() {
  local file="$1"
  shift
  umask 077
  : > "$file"
  for line in "$@"; do
    printf '%s\n' "$line" >> "$file"
  done
}

shell_quote() {
  printf '%q' "$1"
}

info "sudo-make-me-a-website configuration helper"
warn "This script does not create weak default administrator accounts."
warn "Production database migrations remain manual; see docs/database-migration.md."

PROFILE="$(ask "Profile to prepare (dev/prod)" "dev")"
case "$PROFILE" in
  dev|prod) ;;
  *) fail "Unsupported profile: $PROFILE" ;;
esac

DB_HOST="$(ask "Database host" "localhost")"
DB_PORT="$(ask "Database port" "3306")"
DB_NAME="$(ask "Database name" "blog")"
DB_USER="$(ask "Database username" "blog_user")"
if [[ "$DB_USER" == "root" && "$PROFILE" == "prod" ]]; then
  warn "Using root for production is not recommended. Prefer a dedicated database user."
fi

DB_PASSWORD="$(ask_secret "Database password (required)")"
if [[ -z "$DB_PASSWORD" ]]; then
  fail "Database password must not be empty."
fi

DB_URL="jdbc:mysql://${DB_HOST}:${DB_PORT}/${DB_NAME}"
export SPRING_DATASOURCE_URL="$DB_URL"
export SPRING_DATASOURCE_USERNAME="$DB_USER"
export SPRING_DATASOURCE_PASSWORD="$DB_PASSWORD"

write_kv_file "$APP_DIR/.env.database" \
  "export SPRING_DATASOURCE_URL=$(shell_quote "$DB_URL")" \
  "export SPRING_DATASOURCE_USERNAME=$(shell_quote "$DB_USER")" \
  "export SPRING_DATASOURCE_PASSWORD=$(shell_quote "$DB_PASSWORD")"
info "Wrote .env.database with restricted permissions."

if [[ "$PROFILE" == "prod" ]]; then
  warn "Production profile uses ddl-auto=validate and sql.init.mode=never."
  warn "Back up the database and run docs/migrations SQL before production start."
fi

read -r -p "Configure initial admin bootstrap variables now? (y/N): " SET_ADMIN
if [[ "$SET_ADMIN" =~ ^[Yy]$ ]]; then
  ADMIN_USERNAME="$(ask "Admin username")"
  ADMIN_PASSWORD="$(ask_secret "Admin password")"
  if [[ -z "$ADMIN_USERNAME" || -z "$ADMIN_PASSWORD" ]]; then
    fail "Admin username and password must not be empty."
  fi
  write_kv_file "$APP_DIR/.env.admin" \
    "export BLOG_ADMIN_USERNAME=$(shell_quote "$ADMIN_USERNAME")" \
    "export BLOG_ADMIN_PASSWORD=$(shell_quote "$ADMIN_PASSWORD")"
  info "Wrote .env.admin with restricted permissions."
  warn "Remove BLOG_ADMIN_USERNAME and BLOG_ADMIN_PASSWORD after the admin account exists."
else
  warn "Skipped admin bootstrap variables."
  warn "For first production deployment, set BLOG_ADMIN_USERNAME and BLOG_ADMIN_PASSWORD manually."
fi

read -r -p "Configure Aliyun OSS variables now? (y/N): " SET_OSS
if [[ "$SET_OSS" =~ ^[Yy]$ ]]; then
  OSS_ENDPOINT="$(ask "OSS endpoint" "oss-cn-guangzhou.aliyuncs.com")"
  OSS_BUCKET="$(ask "OSS bucket" "krismagic-images")"
  OSS_REGION="$(ask "OSS region" "cn-guangzhou")"
  OSS_CDN="$(ask "OSS CDN domain" "cdn.magiccodelab.com")"
  OSS_KEY="$(ask_secret "OSS access key id")"
  OSS_SECRET="$(ask_secret "OSS access key secret")"
  if [[ -z "$OSS_KEY" || -z "$OSS_SECRET" ]]; then
    fail "OSS access key id and secret must not be empty."
  fi
  write_kv_file "$APP_DIR/.env.oss" \
    "export OSS_ENDPOINT=$(shell_quote "$OSS_ENDPOINT")" \
    "export OSS_BUCKET_NAME=$(shell_quote "$OSS_BUCKET")" \
    "export OSS_REGION=$(shell_quote "$OSS_REGION")" \
    "export OSS_CDN_DOMAIN=$(shell_quote "$OSS_CDN")" \
    "export OSS_ACCESS_KEY_ID=$(shell_quote "$OSS_KEY")" \
    "export OSS_ACCESS_KEY_SECRET=$(shell_quote "$OSS_SECRET")"
  info "Wrote .env.oss with restricted permissions."
elif [[ "$PROFILE" == "prod" ]]; then
  warn "Production requires OSS credentials through .env.oss or environment variables."
fi

BACKEND_PORT="$(ask "Backend port" "8080")"
printf 'http://localhost:%s\n' "$BACKEND_PORT" > "$APP_DIR/.backend-port"
info "Wrote .backend-port."

FRONT_ENV="$APP_DIR/front/.env.production"
API_BASE="$(ask "Production API base URL (empty for same-origin proxy)" "")"
if [[ -n "$API_BASE" ]]; then
  printf 'VITE_API_BASE_URL=%s\n' "$API_BASE" > "$FRONT_ENV"
  info "Wrote front/.env.production."
fi

read -r -p "Install frontend dependencies and build now? (y/N): " BUILD_FRONT
if [[ "$BUILD_FRONT" =~ ^[Yy]$ ]]; then
  require_command npm
  (cd "$APP_DIR/front" && npm install && npm run build)
fi

read -r -p "Build backend jar now? (y/N): " BUILD_BACKEND
if [[ "$BUILD_BACKEND" =~ ^[Yy]$ ]]; then
  (cd "$APP_DIR/backend" && ./mvnw clean package -DskipTests)
fi

info "Configuration complete."
echo "Start locally with: ./start.sh dev"
echo "Start production with: source .env.database && ./start.sh prod"
echo "For production, run database migrations manually before starting."
