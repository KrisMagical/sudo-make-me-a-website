#!/usr/bin/env bash
set -euo pipefail

APP_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SERVICE_NAME="${SERVICE_NAME:-sudo-blog}"
SERVICE_USER="${SERVICE_USER:-www-data}"
APACHE_SITE_NAME="${APACHE_SITE_NAME:-sudo-blog}"

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

ask_yes_no() {
  local prompt="$1"
  local default="${2:-N}"
  local value
  local suffix="y/N"
  if [[ "$default" =~ ^[Yy]$ ]]; then
    suffix="Y/n"
  fi
  read -r -p "$prompt ($suffix): " value
  value="${value:-$default}"
  [[ "$value" =~ ^[Yy]$ ]]
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
  printf "'%s'" "$(printf '%s' "$1" | sed "s/'/'\\\\''/g")"
}

run_frontend_audit() {
  local front_dir="$1"
  if (cd "$front_dir" && npm audit --audit-level=high >/tmp/sudo-blog-npm-audit.log 2>&1); then
    info "Frontend npm audit passed for high severity issues."
    return 0
  fi

  warn "Frontend npm audit reported high severity issues."
  warn "Review with: cd front && npm audit"
  if ask_yes_no "Run safe npm audit fix now? This will not use --force" "N"; then
    (cd "$front_dir" && npm audit fix)
    (cd "$front_dir" && npm audit --audit-level=high) || \
      warn "npm audit still reports high severity issues. Review manually before release."
  else
    warn "Skipped npm audit fix. Review audit output before production release."
  fi
}

fix_frontend_executables() {
  local front_dir="$1"
  local vite_bin="$front_dir/node_modules/vite/bin/vite.js"
  local vite_link="$front_dir/node_modules/.bin/vite"

  if [[ -f "$vite_bin" && ! -x "$vite_bin" ]]; then
    warn "Fixing Vite executable permission."
    chmod +x "$vite_bin" || warn "Could not chmod $vite_bin"
  fi

  if [[ -e "$vite_link" && ! -x "$vite_link" ]]; then
    chmod +x "$vite_link" 2>/dev/null || true
  fi
}

build_frontend() {
  local front_dir="$APP_DIR/front"

  require_command npm
  require_command node

  if [[ -d "$front_dir/node_modules" ]]; then
    if ask_yes_no "Clean existing frontend node_modules and dist before install" "Y"; then
      rm -rf "$front_dir/node_modules" "$front_dir/dist"
      info "Cleaned frontend node_modules and dist."
    fi
  fi

  if [[ -f "$front_dir/package-lock.json" ]]; then
    info "Installing frontend dependencies with npm ci."
    (cd "$front_dir" && npm ci)
  else
    warn "package-lock.json not found; falling back to npm install."
    (cd "$front_dir" && npm install)
  fi

  run_frontend_audit "$front_dir"
  fix_frontend_executables "$front_dir"

  info "Building frontend."
  if ! (cd "$front_dir" && npm run build); then
    warn "npm run build failed. Trying direct Vite entrypoint fallback."
    fix_frontend_executables "$front_dir"
    (cd "$front_dir" && node ./node_modules/vite/bin/vite.js build)
  fi
}

is_root() {
  [[ "${EUID:-$(id -u)}" -eq 0 ]]
}

check_built_artifacts() {
  [[ -d "$APP_DIR/front/dist" ]] || fail "Frontend dist not found. Build frontend first."
  find "$APP_DIR/backend/target" -maxdepth 1 -type f -name '*.jar' ! -name '*sources.jar' ! -name '*javadoc.jar' | grep -q . || \
    fail "Backend jar not found. Build backend first."
}

ensure_prod_oss_file() {
  if [[ -f "$APP_DIR/.env.oss" ]]; then
    return 0
  fi

  warn "Missing .env.oss. Production start currently expects OSS environment variables."
  if ask_yes_no "Create placeholder .env.oss so the site can start without media upload" "N"; then
    write_kv_file "$APP_DIR/.env.oss" \
      "export OSS_ENDPOINT='placeholder'" \
      "export OSS_BUCKET_NAME='placeholder'" \
      "export OSS_REGION='placeholder'" \
      "export OSS_CDN_DOMAIN='placeholder'" \
      "export OSS_ACCESS_KEY_ID='placeholder'" \
      "export OSS_ACCESS_KEY_SECRET='placeholder'"
    warn "Created placeholder .env.oss. Replace it before using media upload."
  else
    fail "Create .env.oss with real OSS values before production start."
  fi
}

fix_deployment_permissions() {
  info "Fixing deployment permissions for $SERVICE_USER."
  chown -R "$SERVICE_USER:$SERVICE_USER" "$APP_DIR"

  find "$APP_DIR" -type d -exec chmod 755 {} \;
  find "$APP_DIR" -type f -exec chmod 644 {} \;

  chmod +x "$APP_DIR/configure.sh" 2>/dev/null || true
  chmod +x "$APP_DIR/config.sh" 2>/dev/null || true
  chmod +x "$APP_DIR/start.sh"
  chmod +x "$APP_DIR/backend/mvnw" 2>/dev/null || true

  chmod 600 "$APP_DIR/.env.database"
  chmod 600 "$APP_DIR/.env.admin" 2>/dev/null || true
  chmod 600 "$APP_DIR/.env.oss" 2>/dev/null || true
}

write_systemd_service() {
  local unit="/etc/systemd/system/${SERVICE_NAME}.service"
  info "Writing systemd service: $unit"
  cat > "$unit" <<EOF
[Unit]
Description=sudo-make-me-a-website backend
After=network.target mysql.service

[Service]
Type=simple
User=${SERVICE_USER}
WorkingDirectory=${APP_DIR}
ExecStart=${APP_DIR}/start.sh prod
Restart=always
RestartSec=5

[Install]
WantedBy=multi-user.target
EOF

  systemctl daemon-reload
  systemctl enable "$SERVICE_NAME"
}

write_apache_site() {
  local domain="$1"
  local alias_value="$2"
  local backend_port="$3"
  local site_file="/etc/apache2/sites-available/${APACHE_SITE_NAME}.conf"
  local alias_line=""

  if [[ -n "$alias_value" ]]; then
    alias_line="    ServerAlias ${alias_value}"
  fi

  info "Writing Apache site: $site_file"
  cat > "$site_file" <<EOF
<VirtualHost *:80>
    ServerName ${domain}
${alias_line}

    DocumentRoot ${APP_DIR}/front/dist

    <Directory ${APP_DIR}/front/dist>
        Options -Indexes +FollowSymLinks
        AllowOverride None
        Require all granted

        RewriteEngine On
        RewriteBase /
        RewriteRule ^index\\.html$ - [L]
        RewriteCond \${REQUEST_FILENAME} !-f
        RewriteCond \${REQUEST_FILENAME} !-d
        RewriteRule . /index.html [L]
    </Directory>

    ProxyPreserveHost On

    ProxyPass /api/ http://127.0.0.1:${backend_port}/api/
    ProxyPassReverse /api/ http://127.0.0.1:${backend_port}/api/

    ProxyPass /actuator/health http://127.0.0.1:${backend_port}/actuator/health
    ProxyPassReverse /actuator/health http://127.0.0.1:${backend_port}/actuator/health

    RequestHeader set X-Forwarded-Proto expr=%{REQUEST_SCHEME}
    RequestHeader set X-Forwarded-Host expr=%{HTTP_HOST}

    ErrorLog \${APACHE_LOG_DIR}/${APACHE_SITE_NAME}-error.log
    CustomLog \${APACHE_LOG_DIR}/${APACHE_SITE_NAME}-access.log combined
</VirtualHost>
EOF

  a2enmod rewrite proxy proxy_http headers >/dev/null
  a2dissite 000-default.conf >/dev/null 2>&1 || true
  a2ensite "${APACHE_SITE_NAME}.conf" >/dev/null
  apache2ctl configtest
}

configure_traditional_runtime() {
  local backend_port="$1"

  if ! is_root; then
    warn "Skipping systemd/Apache setup because this script is not running as root."
    warn "Run again with sudo if you want configure.sh to finish server setup automatically."
    return 0
  fi

  if ! ask_yes_no "Configure permissions, systemd backend service, and Apache site now" "Y"; then
    warn "Skipped server setup. You can still start manually with: sudo -u $SERVICE_USER ./start.sh prod"
    return 0
  fi

  local domain
  local alias_value
  domain="$(ask "Apache ServerName / domain" "$(hostname -f 2>/dev/null || echo localhost)")"
  alias_value="$(ask "Apache ServerAlias (empty to skip)" "")"

  check_built_artifacts
  ensure_prod_oss_file
  fix_deployment_permissions
  write_systemd_service

  if command -v apache2ctl >/dev/null 2>&1; then
    write_apache_site "$domain" "$alias_value" "$backend_port"
  else
    warn "Apache command apache2ctl not found; skipped Apache site setup."
  fi

  if ask_yes_no "Start/restart backend and reload Apache now" "Y"; then
    systemctl restart "$SERVICE_NAME"
    if command -v apache2ctl >/dev/null 2>&1; then
      systemctl reload apache2
    fi
    sleep 5
    if command -v curl >/dev/null 2>&1; then
      curl -i "http://127.0.0.1:${backend_port}/actuator/health" || true
    fi
  fi
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

DB_URL="jdbc:mysql://${DB_HOST}:${DB_PORT}/${DB_NAME}?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai"
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
  build_frontend
fi

read -r -p "Build backend jar now? (y/N): " BUILD_BACKEND
if [[ "$BUILD_BACKEND" =~ ^[Yy]$ ]]; then
  (cd "$APP_DIR/backend" && ./mvnw clean package -DskipTests)
fi

if [[ "$PROFILE" == "prod" ]]; then
  configure_traditional_runtime "$BACKEND_PORT"
fi

info "Configuration complete."
echo "Start locally with: ./start.sh dev"
echo "Start production backend with: ./start.sh prod"
echo "Or use systemd with: sudo systemctl restart $SERVICE_NAME"
echo "For production, run database migrations manually before starting."
