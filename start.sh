#!/usr/bin/env bash
set -euo pipefail

APP_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROFILE="${1:-${SPRING_PROFILES_ACTIVE:-dev}}"

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
  # shellcheck disable=SC1091
  source "$APP_DIR/.env.oss"
elif [[ "$PROFILE" == "prod" ]]; then
  echo "Missing .env.oss. Configure OSS credentials before production start."
  exit 1
else
  export OSS_ACCESS_KEY_ID="${OSS_ACCESS_KEY_ID:-dev-oss-key}"
  export OSS_ACCESS_KEY_SECRET="${OSS_ACCESS_KEY_SECRET:-dev-oss-secret}"
  echo "No .env.oss found; using placeholder OSS credentials for local development."
fi

if [[ -f "$APP_DIR/.env.database" ]]; then
  # shellcheck disable=SC1091
  source "$APP_DIR/.env.database"
fi

if [[ -f "$APP_DIR/.env.admin" ]]; then
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
  exit 1
fi

if command -v ss >/dev/null 2>&1 && ss -ltn "( sport = :$BACKEND_PORT )" | grep -q ":$BACKEND_PORT"; then
  echo "Port $BACKEND_PORT is already in use."
  exit 1
fi

echo "Backend port: $BACKEND_PORT"
exec java -jar "$JAR_FILE" \
  --server.port="$BACKEND_PORT" \
  --spring.profiles.active="$PROFILE"
