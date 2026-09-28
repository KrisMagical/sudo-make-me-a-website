#!/usr/bin/env bash
set -euo pipefail

APP_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ENV_FILE="$APP_DIR/.env"
DEFAULT_FRONTEND_PORT="127.0.0.1:8088"

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

info() { echo -e "${GREEN}$*${NC}"; }
warn() { echo -e "${YELLOW}$*${NC}"; }
fail() { echo -e "${RED}$*${NC}"; exit 1; }

usage() {
  cat <<EOF
Usage: ./deploy-docker.sh [options]

Options:
  --env-file FILE        Env file to use. Default: .env
  --frontend-port PORT   Frontend host binding, e.g. 80 or 127.0.0.1:8088
  --init-db              Initialize a fresh database with bootstrap-schema.sql
  --skip-build           Start existing images without rebuilding
  --pull                 Pull service images before starting
  --no-edit              Do not open the env file editor
  -h, --help             Show this help

Recommended with host HTTPS reverse proxy:
  ./deploy-docker.sh --frontend-port 127.0.0.1:8088 --init-db

This script does not run existing-database migrations automatically.
EOF
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

require_command() {
  if command -v "$1" >/dev/null 2>&1; then
    return 0
  fi

  warn "Missing required command: $1"
  case "$1" in
    docker)
      warn "Install Docker Engine and Docker Compose plugin first."
      warn "Ubuntu example:"
      warn "  sudo apt update"
      warn "  sudo apt install -y ca-certificates curl git"
      warn "  Follow Docker's official install guide or use a trusted mirror."
      warn "After install, verify with:"
      warn "  docker --version"
      warn "  docker compose version"
      ;;
    curl)
      warn "curl is optional but useful for health checks."
      warn "Ubuntu example: sudo apt install -y curl"
      ;;
  esac
  fail "Install the missing prerequisite and rerun deploy-docker.sh."
}

check_docker_ready() {
  require_command docker

  if ! docker info >/dev/null 2>&1; then
    warn "Docker is installed, but the daemon is not reachable."
    warn "Try:"
    warn "  sudo systemctl enable docker"
    warn "  sudo systemctl start docker"
    warn "If you run Docker as a non-root user, add yourself to the docker group and log in again:"
    warn "  sudo usermod -aG docker \$USER"
    fail "Docker daemon is not ready."
  fi

  if ! docker compose version >/dev/null 2>&1; then
    warn "Docker Compose v2 plugin is missing."
    warn "Ubuntu example: sudo apt install -y docker-compose-plugin"
    fail "Install Docker Compose plugin and rerun deploy-docker.sh."
  fi
}

compose() {
  docker compose --env-file "$ENV_FILE" "$@"
}

set_env_value() {
  local key="$1"
  local value="$2"
  local escaped
  escaped="$(printf '%s' "$value" | sed 's/[\/&]/\\&/g')"

  if grep -q "^${key}=" "$ENV_FILE"; then
    sed -i "s/^${key}=.*/${key}=${escaped}/" "$ENV_FILE"
  else
    printf '%s=%s\n' "$key" "$value" >> "$ENV_FILE"
  fi
}

get_env_value() {
  local key="$1"
  grep -E "^${key}=" "$ENV_FILE" | tail -n 1 | cut -d '=' -f 2-
}

prepare_env_file() {
  if [[ ! -f "$ENV_FILE" ]]; then
    info "Creating env file from .env.example."
    cp "$APP_DIR/.env.example" "$ENV_FILE"
    chmod 600 "$ENV_FILE" 2>/dev/null || true
  fi

  if [[ -n "$FRONTEND_PORT_OVERRIDE" ]]; then
    set_env_value "FRONTEND_PORT" "$FRONTEND_PORT_OVERRIDE"
  fi

  if [[ "$NO_EDIT" != "true" ]]; then
    warn "Review $ENV_FILE and replace all placeholder secrets before continuing."
    warn "Required: MYSQL_DATABASE, MYSQL_USER, MYSQL_PASSWORD, MYSQL_ROOT_PASSWORD."
    warn "For host HTTPS reverse proxy, keep FRONTEND_PORT like 127.0.0.1:8088."
    if ask_yes_no "Open env file in editor now" "Y"; then
      "${EDITOR:-nano}" "$ENV_FILE"
    fi
  fi
}

validate_env_file() {
  [[ -f "$ENV_FILE" ]] || fail "Env file not found: $ENV_FILE"

  local required_keys=(
    MYSQL_DATABASE
    MYSQL_USER
    MYSQL_PASSWORD
    MYSQL_ROOT_PASSWORD
  )

  for key in "${required_keys[@]}"; do
    local value
    value="$(get_env_value "$key")"
    if [[ -z "$value" ]]; then
      fail "$key is empty in $ENV_FILE"
    fi
    if [[ "$value" == *replace_with* || "$value" == *strong_* || "$value" == "password" ]]; then
      fail "$key still looks like a placeholder. Edit $ENV_FILE first."
    fi
  done

  local frontend_port
  frontend_port="$(get_env_value FRONTEND_PORT)"
  if [[ -z "$frontend_port" ]]; then
    set_env_value "FRONTEND_PORT" "$DEFAULT_FRONTEND_PORT"
    frontend_port="$DEFAULT_FRONTEND_PORT"
  fi

  info "Frontend host binding: $frontend_port"
  if [[ "$frontend_port" != 127.0.0.1:* && "$frontend_port" != localhost:* ]]; then
    warn "FRONTEND_PORT is not bound to localhost. If using host HTTPS reverse proxy, prefer 127.0.0.1:8088."
  fi
}

wait_for_mysql() {
  info "Waiting for mysql service to become healthy."
  local i
  for i in {1..30}; do
    if compose ps mysql | grep -qi "healthy"; then
      info "MySQL is healthy."
      return 0
    fi
    sleep 3
  done
  compose ps
  warn "MySQL did not become healthy. Useful diagnostics:"
  warn "  docker compose --env-file $ENV_FILE logs mysql"
  warn "Common causes: weak/empty password, reused broken volume, port/storage issues."
  warn "If this is a failed first attempt, inspect logs before deleting any volume."
  fail "MySQL did not become healthy in time."
}

init_database() {
  info "Initializing fresh database with docs/migrations/bootstrap-schema.sql."
  compose exec -T mysql sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE"' < "$APP_DIR/docs/migrations/bootstrap-schema.sql"
  info "Database bootstrap completed."
}

frontend_url() {
  local port
  port="$(get_env_value FRONTEND_PORT)"
  case "$port" in
    127.0.0.1:*|localhost:*)
      printf 'http://%s\n' "$port"
      ;;
    *:*)
      printf 'http://%s\n' "$port"
      ;;
    *)
      printf 'http://127.0.0.1:%s\n' "${port:-80}"
      ;;
  esac
}

health_check() {
  local url
  url="$(frontend_url)"
  info "Checking frontend and backend health through frontend container: $url"
  if command -v curl >/dev/null 2>&1; then
    curl -i "$url/" || true
    curl -i "$url/actuator/health" || true
  else
    warn "curl not installed; skip HTTP health checks."
  fi
}

INIT_DB="false"
SKIP_BUILD="false"
PULL_IMAGES="false"
NO_EDIT="false"
FRONTEND_PORT_OVERRIDE=""

while [[ $# -gt 0 ]]; do
  case "$1" in
    --env-file)
      case "${2:-}" in
        /*) ENV_FILE="${2:-}" ;;
        *) ENV_FILE="$APP_DIR/${2:-}" ;;
      esac
      shift 2
      ;;
    --frontend-port)
      FRONTEND_PORT_OVERRIDE="${2:-}"
      shift 2
      ;;
    --init-db)
      INIT_DB="true"
      shift
      ;;
    --skip-build)
      SKIP_BUILD="true"
      shift
      ;;
    --pull)
      PULL_IMAGES="true"
      shift
      ;;
    --no-edit)
      NO_EDIT="true"
      shift
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      fail "Unknown option: $1"
      ;;
  esac
done

cd "$APP_DIR"
check_docker_ready

info "sudo-make-me-a-website Docker deployment helper"
warn "This script does not run existing-database migrations automatically."
warn "Back up existing production data before applying migrations."

prepare_env_file
validate_env_file

info "Validating Docker Compose config."
if ! compose config --quiet; then
  warn "Docker Compose config is invalid."
  warn "Check $ENV_FILE for missing values or invalid FRONTEND_PORT syntax."
  warn "Examples:"
  warn "  FRONTEND_PORT=80"
  warn "  FRONTEND_PORT=127.0.0.1:8088"
  fail "Fix compose configuration and rerun deploy-docker.sh."
fi

if [[ "$PULL_IMAGES" == "true" ]]; then
  info "Pulling service images."
  if ! compose pull; then
    warn "Pull failed. This is often a Docker Hub or network issue."
    warn "You can retry, configure a trusted registry mirror, or build when the network is stable."
    warn "Continuing because local build may still work if base images are cached."
  fi
fi

info "Starting MySQL first."
compose up -d mysql
wait_for_mysql

if [[ "$INIT_DB" != "true" ]]; then
  if ask_yes_no "Is this a fresh empty database and should bootstrap-schema.sql be executed now" "N"; then
    INIT_DB="true"
  fi
fi

if [[ "$INIT_DB" == "true" ]]; then
  warn "bootstrap-schema.sql is for fresh databases only."
  if ask_yes_no "Confirm this database is fresh or safe to initialize" "N"; then
    init_database
  else
    warn "Skipped database bootstrap."
  fi
fi

if [[ "$SKIP_BUILD" == "true" ]]; then
  info "Starting backend and frontend without rebuilding."
  compose up -d backend frontend
else
  info "Building and starting backend and frontend."
  compose up -d --build backend frontend
fi

compose ps
health_check

warn "After the first admin account exists, remove BLOG_ADMIN_USERNAME and BLOG_ADMIN_PASSWORD from $ENV_FILE, then run:"
echo "  docker compose --env-file $ENV_FILE up -d backend"

info "Docker deployment complete."
