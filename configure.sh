#!/usr/bin/env bash
set -euo pipefail

APP_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SERVICE_NAME="${SERVICE_NAME:-sudo-blog}"
SERVICE_USER="${SERVICE_USER:-www-data}"
APACHE_SITE_NAME="${APACHE_SITE_NAME:-sudo-blog}"
NGINX_SITE_NAME="${NGINX_SITE_NAME:-sudo-blog}"

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
    value="${value:-$default}"
  else
    read -r -p "$prompt: " value
  fi
  value="${value//$'\r'/}"
  value="${value//$'\n'/}"
  echo "$value"
}

ask_secret() {
  local prompt="$1"
  local value
  read -r -s -p "$prompt: " value
  printf '\n' >&2
  value="${value//$'\r'/}"
  value="${value//$'\n'/}"
  printf '%s' "$value"
}

ask_required_secret() {
  local prompt="$1"
  local label="$2"
  local value
  while true; do
    value="$(ask_secret "$prompt")"
    if [[ -z "$value" ]]; then
      echo -e "${YELLOW}$label must not be empty. Enter the actual password, without wrapping quotes.${NC}" >&2
      continue
    fi
    if [[ "$value" == *$'\r'* || "$value" == *$'\n'* ]]; then
      echo -e "${YELLOW}$label must be a single line. Paste it again without line breaks.${NC}" >&2
      continue
    fi
    printf '%s' "$value"
    return 0
  done
}

ask_confirmed_secret() {
  local prompt="$1"
  local label="$2"
  local first
  local second
  while true; do
    first="$(ask_required_secret "$prompt" "$label")"
    second="$(ask_required_secret "Confirm $label" "$label confirmation")"
    if [[ "$first" == "$second" ]]; then
      printf '%s' "$first"
      return 0
    fi
    echo -e "${YELLOW}$label values did not match. Please enter them again.${NC}" >&2
  done
}

validate_single_line_value() {
  local label="$1"
  local value="$2"
  if [[ "$value" == *$'\r'* || "$value" == *$'\n'* ]]; then
    fail "$label must be a single line. Paste it again without line breaks."
  fi
}

require_command() {
  if command -v "$1" >/dev/null 2>&1; then
    return 0
  fi

  warn "Missing required command: $1"
  case "$1" in
    node|npm)
      warn "Install Node.js first. Vite 8 requires Node.js ^20.19.0 or >=22.12.0."
      warn "Ubuntu example: install Node.js 22 from NodeSource or your trusted package source, then rerun this script."
      warn "Check with: node -v && npm -v"
      ;;
    java)
      warn "Install Java 21 before building or running the backend."
      warn "Ubuntu example: sudo apt install -y openjdk-21-jdk"
      warn "Check with: java -version"
      ;;
    apache2ctl)
      warn "Apache is not installed or not in PATH."
      warn "Ubuntu example: sudo apt install -y apache2"
      ;;
    nginx)
      warn "Nginx is not installed or not in PATH."
      warn "Ubuntu example: sudo apt install -y nginx"
      ;;
    systemctl)
      warn "systemd is required for automatic backend service setup."
      warn "If this server does not use systemd, choose to skip server setup and run start.sh manually."
      ;;
  esac
  fail "Install the missing prerequisite and rerun configure.sh."
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

sql_quote() {
  printf "'%s'" "$(printf '%s' "$1" | sed "s/'/''/g")"
}

hash_bcrypt_password() {
  local password="$1"
  local hash
  if ! command -v htpasswd >/dev/null 2>&1; then
    echo -e "${RED}Cannot update an existing admin password because htpasswd is missing. Install it with: sudo apt install -y apache2-utils${NC}" >&2
    return 1
  fi
  if ! hash="$(printf '%s\n' "$password" | htpasswd -nBi -C 12 "" 2>/dev/null | sed 's/^://')"; then
    echo -e "${RED}Could not generate BCrypt password hash with htpasswd.${NC}" >&2
    return 1
  fi
  hash="${hash//$'\r'/}"
  hash="${hash//$'\n'/}"
  if [[ -z "$hash" || "$hash" != \$2* ]]; then
    echo -e "${RED}Generated BCrypt password hash was invalid.${NC}" >&2
    return 1
  fi
  printf '%s' "$hash"
}

write_frontend_env() {
  local file="$APP_DIR/front/.env.production"
  local api_base="$1"
  : > "$file"
  if [[ -n "$api_base" ]]; then
    printf 'VITE_API_BASE_URL=%s\n' "$api_base" >> "$file"
  fi
  printf 'VITE_FOOTER_TEXT=%s\n' "$SITE_FOOTER_TEXT" >> "$file"
  info "Wrote front/.env.production."
}

update_site_config_database() {
  if ! command -v mysql >/dev/null 2>&1; then
    warn "mysql client not found; skipped database site config update."
    warn "Install mysql client or update site config from the admin panel later."
    return 0
  fi

  if ! ask_yes_no "Write site name/footer into database site config now" "Y"; then
    warn "Skipped database site config update. You can update it from the admin panel later."
    return 0
  fi

  local sql
  sql="
UPDATE site_configs SET is_active = b'0' WHERE is_active = b'1';
INSERT INTO site_configs (
  site_name,
  author_name,
  footer_text,
  meta_description,
  meta_keywords,
  copyright_text,
  is_active
) VALUES (
  $(sql_quote "$SITE_NAME"),
  $(sql_quote "$SITE_AUTHOR"),
  $(sql_quote "$SITE_FOOTER_TEXT"),
  '',
  '',
  $(sql_quote "$SITE_FOOTER_TEXT"),
  b'1'
);
"

  if mysql -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" -p"$DB_PASSWORD" "$DB_NAME" -e "$sql"; then
    info "Updated database site config."
  else
    warn "Could not update site config in database."
    warn "Make sure the database schema exists first:"
    warn "  mysql -u $DB_USER -p $DB_NAME < docs/migrations/bootstrap-schema.sql"
    warn "You can also update site config from the admin panel after startup."
  fi
}

update_admin_password_database() {
  local username="$1"
  local password="$2"

  if ! command -v mysql >/dev/null 2>&1; then
    warn "mysql client not found; skipped existing admin password update."
    return 0
  fi

  local exists
  exists="$(mysql -N -B -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" -p"$DB_PASSWORD" "$DB_NAME" -e "SELECT COUNT(*) FROM users WHERE username = $(sql_quote "$username");" 2>/dev/null || echo 0)"
  if [[ "$exists" == "0" ]]; then
    warn "Admin user '$username' does not exist yet; bootstrap variables will create it on first successful backend start."
    return 0
  fi

  local password_hash
  if ! password_hash="$(hash_bcrypt_password "$password")"; then
    fail "Existing admin password was not changed."
  fi
  mysql -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" -p"$DB_PASSWORD" "$DB_NAME" \
    -e "UPDATE users SET password = $(sql_quote "$password_hash") WHERE username = $(sql_quote "$username");"
  info "Updated password for existing admin user '$username'."
}

verify_database_connection() {
  if ! command -v mysql >/dev/null 2>&1; then
    warn "mysql client not found; skipped database connection preflight."
    warn "Install mysql client so configure.sh can catch database credential errors before startup."
    return 0
  fi

  if mysql -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" -p"$DB_PASSWORD" "$DB_NAME" -e "SELECT 1;" >/dev/null 2>&1; then
    info "Database connection preflight passed."
  else
    fail "Cannot connect to MySQL database '$DB_NAME' as '$DB_USER'. Create the database/user first and verify the password."
  fi
}

verify_production_schema() {
  [[ "$PROFILE" == "prod" ]] || return 0
  if ! command -v mysql >/dev/null 2>&1; then
    warn "mysql client not found; cannot verify production schema."
    return 0
  fi

  local users_table
  users_table="$(mysql -N -B -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" -p"$DB_PASSWORD" "$DB_NAME" -e "SHOW TABLES LIKE 'users';" 2>/dev/null || true)"
  if [[ "$users_table" != "users" ]]; then
    fail "Production schema is not initialized. Run: mysql -u $DB_USER -p $DB_NAME < docs/migrations/bootstrap-schema.sql"
  fi

  local missing_items=()
  local comments_status
  local comments_moderation_reason
  local like_logs_unique
  comments_status="$(mysql -N -B -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" -p"$DB_PASSWORD" "$DB_NAME" -e "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'comments' AND COLUMN_NAME = 'status';" 2>/dev/null || echo 0)"
  comments_moderation_reason="$(mysql -N -B -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" -p"$DB_PASSWORD" "$DB_NAME" -e "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'comments' AND COLUMN_NAME = 'moderation_reason';" 2>/dev/null || echo 0)"
  like_logs_unique="$(mysql -N -B -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" -p"$DB_PASSWORD" "$DB_NAME" -e "SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'like_logs' AND INDEX_NAME = 'uk_like_logs_post_identifier';" 2>/dev/null || echo 0)"

  [[ "$comments_status" == "1" ]] || missing_items+=("comments.status")
  [[ "$comments_moderation_reason" == "1" ]] || missing_items+=("comments.moderation_reason")
  [[ "$like_logs_unique" == "1" ]] || missing_items+=("like_logs.uk_like_logs_post_identifier")

  if (( ${#missing_items[@]} > 0 )); then
    warn "Production schema is missing required migration artifacts:"
    printf '  - %s\n' "${missing_items[@]}"
    fail "Run pending migrations in order: docs/migrations/001-comment-status-and-like-log-unique.sql then docs/migrations/002-comment-moderation-reason.sql"
  fi
  info "Production schema preflight passed."
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

check_backend_build_prerequisites() {
  require_command java
  if [[ ! -x "$APP_DIR/backend/mvnw" ]]; then
    warn "Maven Wrapper is not executable. Fixing permission."
    chmod +x "$APP_DIR/backend/mvnw" 2>/dev/null || \
      fail "Could not chmod backend/mvnw. Run: chmod +x backend/mvnw"
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
  require_command systemctl
  local unit="/etc/systemd/system/${SERVICE_NAME}.service"
  info "Writing systemd service: $unit"
  cat > "$unit" <<EOF
[Unit]
Description=sudo-make-me-a-website backend
After=network.target mysql.service

[Service]
Type=simple
User=${SERVICE_USER}
Environment=SERVICE_USER=${SERVICE_USER}
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
  local enable_https="$4"
  local site_file="/etc/apache2/sites-available/${APACHE_SITE_NAME}.conf"
  local alias_line=""
  local cert_file=""
  local chain_file=""
  local key_file=""

  if [[ -n "$alias_value" ]]; then
    alias_line="    ServerAlias ${alias_value}"
  fi

  info "Writing Apache site: $site_file"
  if [[ "$enable_https" == "true" ]]; then
    cert_file="$(ask "Apache SSL certificate file" "/etc/ssl/certs/${domain}_public.crt")"
    chain_file="$(ask "Apache SSL certificate chain file" "/etc/ssl/certs/${domain}_chain.crt")"
    key_file="$(ask "Apache SSL certificate key file" "/etc/ssl/private/${domain}.key")"

    local missing_cert="false"
    if [[ ! -f "$cert_file" ]]; then
      warn "Certificate file not found yet: $cert_file"
      missing_cert="true"
    fi
    if [[ ! -f "$chain_file" ]]; then
      warn "Certificate chain file not found yet: $chain_file"
      missing_cert="true"
    fi
    if [[ ! -f "$key_file" ]]; then
      warn "Certificate key file not found yet: $key_file"
      missing_cert="true"
    fi
    info "Apache HTTPS certificate paths:"
    info "  certificate: $cert_file"
    info "  chain:       $chain_file"
    info "  key:         $key_file"

    if [[ "$missing_cert" == "true" ]]; then
      warn "Apache cannot load an HTTPS virtual host until all certificate files exist."
      if ask_yes_no "Generate an HTTP-only Apache site for now" "Y"; then
        enable_https="false"
      else
        fail "Place the certificate files at the paths above, then rerun configure.sh."
      fi
    fi
  fi

  if [[ "$enable_https" == "true" ]]; then
    cat > "$site_file" <<EOF
<VirtualHost *:443>
    ServerName ${domain}
${alias_line}
    ServerAdmin webmaster@${domain}

    DocumentRoot ${APP_DIR}/front/dist

    SSLEngine on
    SSLCertificateFile ${cert_file}
    SSLCertificateChainFile ${chain_file}
    SSLCertificateKeyFile ${key_file}

    <Directory ${APP_DIR}/front/dist>
        Options -Indexes +FollowSymLinks
        AllowOverride None
        Require all granted

        RewriteEngine On
        RewriteBase /
        RewriteRule ^index\\.html$ - [L]
        RewriteCond %{REQUEST_FILENAME} !-f
        RewriteCond %{REQUEST_FILENAME} !-d
        RewriteRule . /index.html [L]
    </Directory>

    ProxyRequests Off
    ProxyPreserveHost On
    ProxyTimeout 300

    RequestHeader set X-Forwarded-Proto "https"
    RequestHeader set X-Forwarded-Host expr=%{HTTP_HOST}

    ProxyPass /api/ http://127.0.0.1:${backend_port}/api/
    ProxyPassReverse /api/ http://127.0.0.1:${backend_port}/api/

    ProxyPass /actuator/health http://127.0.0.1:${backend_port}/actuator/health
    ProxyPassReverse /actuator/health http://127.0.0.1:${backend_port}/actuator/health

    ErrorLog \${APACHE_LOG_DIR}/${APACHE_SITE_NAME}-ssl-error.log
    CustomLog \${APACHE_LOG_DIR}/${APACHE_SITE_NAME}-ssl-access.log combined
</VirtualHost>

<VirtualHost *:80>
    ServerName ${domain}
${alias_line}
    Redirect permanent / https://${domain}/
</VirtualHost>
EOF
  fi

  if [[ "$enable_https" != "true" ]]; then
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
        RewriteCond %{REQUEST_FILENAME} !-f
        RewriteCond %{REQUEST_FILENAME} !-d
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
  fi

  if grep -q '\${REQUEST_FILENAME}' "$site_file"; then
    warn "Repairing invalid Apache RewriteCond variable syntax in $site_file."
    sed -i 's/${REQUEST_FILENAME}/%{REQUEST_FILENAME}/g' "$site_file"
  fi
  if grep -q '\${REQUEST_FILENAME}' "$site_file"; then
    fail "Apache site still contains invalid RewriteCond variable syntax: \${REQUEST_FILENAME}. Use %{REQUEST_FILENAME}."
  fi
  if ! grep -q '%{REQUEST_FILENAME}' "$site_file"; then
    fail "Apache site is missing the SPA rewrite checks that use %{REQUEST_FILENAME}."
  fi
  info "Apache rewrite checks:"
  grep -n 'REQUEST_FILENAME' "$site_file"

  a2enmod rewrite proxy proxy_http headers >/dev/null
  if [[ "$enable_https" == "true" ]]; then
    a2enmod ssl >/dev/null
  fi
  a2dissite 000-default.conf >/dev/null 2>&1 || true
  a2ensite "${APACHE_SITE_NAME}.conf" >/dev/null
  apache2ctl configtest
}

write_nginx_site() {
  local domain="$1"
  local alias_value="$2"
  local backend_port="$3"
  local site_file="/etc/nginx/sites-available/${NGINX_SITE_NAME}"
  local server_names="$domain"

  if [[ -n "$alias_value" ]]; then
    server_names="$server_names $alias_value"
  fi

  info "Writing Nginx site: $site_file"
  cat > "$site_file" <<EOF
server {
    listen 80;
    server_name ${server_names};

    root ${APP_DIR}/front/dist;
    index index.html;

    location / {
        try_files \$uri \$uri/ /index.html;
    }

    location /api/ {
        proxy_pass http://127.0.0.1:${backend_port}/api/;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
        proxy_set_header X-Request-Id \$request_id;
    }

    location = /actuator/health {
        proxy_pass http://127.0.0.1:${backend_port}/actuator/health;
        proxy_set_header Host \$host;
        proxy_set_header X-Request-Id \$request_id;
    }

    access_log /var/log/nginx/${NGINX_SITE_NAME}-access.log;
    error_log /var/log/nginx/${NGINX_SITE_NAME}-error.log;
}
EOF

  ln -sfn "$site_file" "/etc/nginx/sites-enabled/${NGINX_SITE_NAME}"
  if [[ -e /etc/nginx/sites-enabled/default ]]; then
    rm -f /etc/nginx/sites-enabled/default
  fi
  nginx -t
}

configure_traditional_runtime() {
  local backend_port="$1"

  if ! is_root; then
    warn "Skipping systemd/web server setup because this script is not running as root."
    warn "Run again with sudo if you want configure.sh to finish server setup automatically."
    return 0
  fi

  if ! ask_yes_no "Configure permissions, systemd backend service, and web server site now" "Y"; then
    warn "Skipped server setup. You can still start manually with: sudo -u $SERVICE_USER ./start.sh prod"
    return 0
  fi

  local domain
  local alias_value
  local web_server
  domain="$(ask "Site domain / server name" "$SITE_DOMAIN")"
  alias_value="$(ask "Site alias (empty to skip)" "")"
  web_server="$(ask "Web server to configure (apache/nginx/none)" "apache")"
  case "$web_server" in
    apache|nginx|none) ;;
    *) fail "Unsupported web server: $web_server" ;;
  esac

  check_built_artifacts
  ensure_prod_oss_file
  fix_deployment_permissions
  write_systemd_service

  case "$web_server" in
    apache)
      if command -v apache2ctl >/dev/null 2>&1; then
        local enable_apache_https="false"
        if ask_yes_no "Enable HTTPS in generated Apache site" "Y"; then
          enable_apache_https="true"
          warn "Place certificate files at the prompted paths before Apache reload succeeds."
        fi
        write_apache_site "$domain" "$alias_value" "$backend_port" "$enable_apache_https"
      else
        warn "Apache command apache2ctl not found; skipped Apache site setup."
        warn "Install it with: sudo apt install -y apache2"
        warn "Then rerun configure.sh and choose apache, or choose nginx/none."
      fi
      ;;
    nginx)
      if command -v nginx >/dev/null 2>&1; then
        write_nginx_site "$domain" "$alias_value" "$backend_port"
      else
        warn "Nginx command not found; skipped Nginx site setup."
        warn "Install it with: sudo apt install -y nginx"
        warn "Then rerun configure.sh and choose nginx, or choose apache/none."
      fi
      ;;
    none)
      warn "Skipped web server site setup."
      ;;
  esac

  if ask_yes_no "Start/restart backend and reload selected web server now" "Y"; then
    systemctl restart "$SERVICE_NAME"
    case "$web_server" in
      apache)
        if command -v apache2ctl >/dev/null 2>&1; then
          systemctl reload apache2
        fi
        ;;
      nginx)
        if command -v nginx >/dev/null 2>&1; then
          systemctl reload nginx
        fi
        ;;
    esac
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

SITE_NAME="$(ask "Website name" "KrisMagic")"
SITE_DOMAIN="$(ask "Website domain" "magiccodelab.com")"
SITE_AUTHOR="$(ask "Website author name" "KrisMagic")"
SITE_FOOTER_TEXT="$(ask "Website footer/copyright text" "(c) 2026 KrisMagic. All rights reserved.")"
validate_single_line_value "Website name" "$SITE_NAME"
validate_single_line_value "Website domain" "$SITE_DOMAIN"
validate_single_line_value "Website author name" "$SITE_AUTHOR"
validate_single_line_value "Website footer/copyright text" "$SITE_FOOTER_TEXT"

DB_HOST="$(ask "Database host" "localhost")"
DB_PORT="$(ask "Database port" "3306")"
DB_NAME="$(ask "Database name" "blog")"
DB_USER="$(ask "Database username" "blog_user")"
if [[ "$DB_USER" == "root" && "$PROFILE" == "prod" ]]; then
  warn "Using root for production is not recommended. Prefer a dedicated database user."
fi

DB_PASSWORD="$(ask_required_secret "Database password (required)" "Database password")"

DB_URL="jdbc:mysql://${DB_HOST}:${DB_PORT}/${DB_NAME}?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false"
export SPRING_DATASOURCE_URL="$DB_URL"
export SPRING_DATASOURCE_USERNAME="$DB_USER"
export SPRING_DATASOURCE_PASSWORD="$DB_PASSWORD"

write_kv_file "$APP_DIR/.env.database" \
  "export SPRING_DATASOURCE_URL=$(shell_quote "$DB_URL")" \
  "export SPRING_DATASOURCE_USERNAME=$(shell_quote "$DB_USER")" \
  "export SPRING_DATASOURCE_PASSWORD=$(shell_quote "$DB_PASSWORD")"
info "Wrote .env.database with restricted permissions."

verify_database_connection
verify_production_schema

if [[ "$PROFILE" == "prod" ]]; then
  update_site_config_database
fi

if [[ "$PROFILE" == "prod" ]]; then
  warn "Production profile uses ddl-auto=validate and sql.init.mode=never."
  warn "Back up the database and run docs/migrations SQL before production start."
fi

read -r -p "Configure initial admin bootstrap variables now? (y/N): " SET_ADMIN
if [[ "$SET_ADMIN" =~ ^[Yy]$ ]]; then
  ADMIN_USERNAME="$(ask "Admin username")"
  if [[ -z "$ADMIN_USERNAME" ]]; then
    fail "Admin username must not be empty."
  fi
  ADMIN_PASSWORD="$(ask_confirmed_secret "Admin password" "Admin password")"
  validate_single_line_value "Admin username" "$ADMIN_USERNAME"
  write_kv_file "$APP_DIR/.env.admin" \
    "export BLOG_ADMIN_USERNAME=$(shell_quote "$ADMIN_USERNAME")" \
    "export BLOG_ADMIN_PASSWORD=$(shell_quote "$ADMIN_PASSWORD")"
  info "Wrote .env.admin with restricted permissions."
  if [[ "$PROFILE" == "prod" ]] && ask_yes_no "Update this admin password in the existing database now" "Y"; then
    update_admin_password_database "$ADMIN_USERNAME" "$ADMIN_PASSWORD"
  fi
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
  OSS_KEY="$(ask_required_secret "OSS access key id" "OSS access key id")"
  OSS_SECRET="$(ask_required_secret "OSS access key secret" "OSS access key secret")"
  validate_single_line_value "OSS endpoint" "$OSS_ENDPOINT"
  validate_single_line_value "OSS bucket" "$OSS_BUCKET"
  validate_single_line_value "OSS region" "$OSS_REGION"
  validate_single_line_value "OSS CDN domain" "$OSS_CDN"
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
write_frontend_env "$API_BASE"

read -r -p "Install frontend dependencies and build now? (y/N): " BUILD_FRONT
if [[ "$BUILD_FRONT" =~ ^[Yy]$ ]]; then
  build_frontend
fi

read -r -p "Build backend jar now? (y/N): " BUILD_BACKEND
if [[ "$BUILD_BACKEND" =~ ^[Yy]$ ]]; then
  check_backend_build_prerequisites
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
