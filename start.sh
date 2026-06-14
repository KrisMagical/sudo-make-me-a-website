#!/usr/bin/env bash
set -euo pipefail

GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
CYAN='\033[0;36m'
NC='\033[0m'

echo -e "${BLUE}>> Starting blog backend in production mode${NC}"

if [ "$(whoami)" != "www-data" ]; then
  echo -e "${RED}This script must be run as www-data.${NC}"
  echo -e "${YELLOW}Use: sudo -u www-data ./start.sh${NC}"
  exit 1
fi

JAR_FILE=$(find backend/target -name "*.jar" -not -name "*-sources.jar" -not -name "*-javadoc.jar" | head -1)
if [ -z "$JAR_FILE" ]; then
  echo -e "${RED}Backend JAR not found. Run ./configure.sh first.${NC}"
  exit 1
fi
echo -e "${GREEN}Found backend JAR: $JAR_FILE${NC}"

OSS_ENV_FILE=".env.oss"
if [ -f "$OSS_ENV_FILE" ]; then
  echo -e "${BLUE}Loading OSS environment variables...${NC}"
  # shellcheck disable=SC1090
  source "$OSS_ENV_FILE"
else
  echo -e "${RED}Missing $OSS_ENV_FILE. Run ./configure.sh first.${NC}"
  exit 1
fi

if [ -z "${OSS_ACCESS_KEY_ID:-}" ] || [ -z "${OSS_ACCESS_KEY_SECRET:-}" ]; then
  echo -e "${RED}OSS credentials are missing from $OSS_ENV_FILE.${NC}"
  exit 1
fi

BACKEND_PORT_FILE=".backend-port"
if [ ! -f "$BACKEND_PORT_FILE" ]; then
  echo -e "${RED}Missing $BACKEND_PORT_FILE. Run ./configure.sh first.${NC}"
  exit 1
fi
BACKEND_PORT=$(grep -oP '(?<=:)\d+' "$BACKEND_PORT_FILE" || echo "8080")
echo -e "${GREEN}Using backend port: $BACKEND_PORT${NC}"

if [ -f "backend.pid" ]; then
  OLD_PID=$(cat backend.pid)
  if ps -p "$OLD_PID" > /dev/null; then
    echo -e "${YELLOW}Stopping previous backend instance: $OLD_PID${NC}"
    kill "$OLD_PID"
    sleep 2
  fi
fi

if command -v lsof >/dev/null 2>&1 && lsof -Pi :"$BACKEND_PORT" -sTCP:LISTEN -t >/dev/null 2>&1; then
  echo -e "${YELLOW}Port $BACKEND_PORT is occupied. Stop that process before starting.${NC}"
  exit 1
fi

nohup java -jar "$JAR_FILE" \
  --server.port="$BACKEND_PORT" \
  --spring.profiles.active=prod \
  > backend.log 2>&1 &

BACKEND_PID=$!
echo "$BACKEND_PID" > backend.pid

if ps -p "$BACKEND_PID" > /dev/null; then
  echo -e "${GREEN}Backend started with PID: $BACKEND_PID${NC}"
else
  echo -e "${RED}Backend failed to start. Check backend.log.${NC}"
  exit 1
fi

if [ -d "front/dist" ]; then
  echo -e "${GREEN}Frontend static files detected in front/dist.${NC}"
else
  echo -e "${YELLOW}front/dist not found. Run cd front && npm run build.${NC}"
fi

echo -e "${CYAN}Backend port: $BACKEND_PORT${NC}"
echo -e "${CYAN}Log: backend.log${NC}"
