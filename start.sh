#!/usr/bin/env bash
set -euo pipefail

APP_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROFILE="${1:-${SPRING_PROFILES_ACTIVE:-dev}}"

fail() {
  echo "$*"
  exit 1
}

require_command() {
  if command -v "$1" >/dev/null 2>&1; then
    return 0
  fi

  echo "Missing required command: $1"
  case "$1" in
    java)
      echo "Install Java 21 before starting the backend."
      echo "Ubuntu example: sudo apt install -y openjdk-21-jre"
      echo "Check with: java -version"
      ;;
    find)
      echo "find is required to locate the backend jar."
      ;;
  esac
  exit 1
}

check_env_file_single_line_exports() {
  local file="$1"
  [[ -f "$file" ]] || return 0

  if grep -n "^export .*='[[:space:]]*$" "$file" >/tmp/sudo-blog-bad-env-lines 2>/dev/null; then
    echo "Invalid multiline value detected in $file"
    echo "The following export starts a quoted value but does not contain it on the same line:"
    cat /tmp/sudo-blog-bad-env-lines
    echo "Fix it so each value is a single line, for example:"
    echo "  export SPRING_DATASOURCE_PASSWORD='your_password_here'"
    exit 1
  fi
}

case "$PROFILE" in
  dev|prod|test) ;;
  *)
    echo "Unsupported profile: $PROFILE"
    echo "Usage: $0 [dev|prod|test]"
    exit 1
    ;;
esac

echo "Starting sudo-make-me-a-website"
echo "Profile: $PROFILE"

require_command java
require_command find

if [[ "$PROFILE" == "prod" ]]; then
  echo "Production mode: confirm database backup and migrations are complete."
  echo "Production mode: confirm a strong admin password is configured for first bootstrap."
  echo "Production mode: this script will not run schema migrations."

  if [[ "$(id -un)" != "www-data" ]]; then
    echo "Production start should run as www-data."
    echo "Example: sudo -u www-data ./start.sh prod"
    exit 1
  fi
fi

if [[ -f "$APP_DIR/.env.oss" ]]; then
  check_env_file_single_line_exports "$APP_DIR/.env.oss"
  # shellcheck disable=SC1091
  source "$APP_DIR/.env.oss"
elif [[ "$PROFILE" == "prod" ]]; then
  echo "Missing .env.oss."
  echo "Run sudo ./configure.sh and configure OSS, or let it create a placeholder if media upload is not used yet."
  exit 1
else
  export OSS_ACCESS_KEY_ID="${OSS_ACCESS_KEY_ID:-dev-oss-key}"
  export OSS_ACCESS_KEY_SECRET="${OSS_ACCESS_KEY_SECRET:-dev-oss-secret}"
  echo "No .env.oss found; using placeholder OSS credentials for local development."
fi

if [[ -f "$APP_DIR/.env.database" ]]; then
  check_env_file_single_line_exports "$APP_DIR/.env.database"
  # shellcheck disable=SC1091
  source "$APP_DIR/.env.database"
elif [[ "$PROFILE" == "prod" ]]; then
  echo "Missing .env.database."
  echo "Run sudo ./configure.sh to create database configuration."
  exit 1
fi

if [[ -f "$APP_DIR/.env.admin" ]]; then
  check_env_file_single_line_exports "$APP_DIR/.env.admin"
  # shellcheck disable=SC1091
  source "$APP_DIR/.env.admin"
fi

if [[ "$PROFILE" == "prod" ]]; then
  if [[ -z "${BLOG_ADMIN_USERNAME:-}" || -z "${BLOG_ADMIN_PASSWORD:-}" ]]; then
    echo "BLOG_ADMIN_USERNAME/BLOG_ADMIN_PASSWORD are not set."
    echo "If the first administrator already exists, this is expected."
    echo "For first deployment, set strong values before starting."
  else
    echo "Admin bootstrap variables detected. Remove them after the admin account exists."
  fi
fi

BACKEND_PORT="${BACKEND_PORT:-8080}"
if [[ -f "$APP_DIR/.backend-port" ]]; then
  BACKEND_URL="$(tr -d '[:space:]' < "$APP_DIR/.backend-port")"
  BACKEND_PORT="${BACKEND_URL##*:}"
fi

JAR_FILE="$(find "$APP_DIR/backend/target" -maxdepth 1 -type f -name '*.jar' ! -name '*sources.jar' ! -name '*javadoc.jar' | head -n 1 || true)"
if [[ -z "$JAR_FILE" ]]; then
  echo "Backend jar not found. Build first:"
  echo "  cd backend && ./mvnw clean package -DskipTests"
  echo "If running as www-data, also check permissions:"
  echo "  sudo chown -R www-data:www-data $APP_DIR"
  echo "  sudo -u www-data find $APP_DIR/backend/target -maxdepth 1 -type f -name '*.jar'"
  exit 1
fi

if [[ ! -r "$JAR_FILE" ]]; then
  echo "Backend jar is not readable by user $(id -un): $JAR_FILE"
  echo "Fix permissions, for example:"
  echo "  sudo chown -R www-data:www-data $APP_DIR"
  echo "  sudo chmod -R u+rwX,go+rX $APP_DIR"
  exit 1
fi

if command -v ss >/dev/null 2>&1 && ss -ltn "( sport = :$BACKEND_PORT )" | grep -q ":$BACKEND_PORT"; then
  echo "Port $BACKEND_PORT is already in use."
  echo "Find the process with:"
  echo "  sudo ss -ltnp '( sport = :$BACKEND_PORT )'"
  echo "Or choose another port in configure.sh."
  exit 1
fi

echo "Backend port: $BACKEND_PORT"
exec java -jar "$JAR_FILE" \
  --server.port="$BACKEND_PORT" \
  --spring.profiles.active="$PROFILE"
