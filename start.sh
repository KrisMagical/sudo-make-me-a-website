#!/usr/bin/env bash
# ============================================================
#  START SCRIPT (Production Mode) | Run as www-data
# ============================================================

GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
CYAN='\033[0;36m'
NC='\033[0m'

# Banner
clear
echo -e "${GREEN}"
echo "   _____                _          "
echo "  / ____|              | |         "
echo " | (___  _   _   __| | ___    "
echo "  \___ \| | | |/ _\` |/ _ \   "
echo "  ____) | |_| | (_| | (_) |  "
echo " |_____/ \__,_|\__,_|\___/    "
echo -e "${NC}"
echo -e "${BLUE}>> Production Startup Mode Initiated (Apache + Java)${NC}\n"

# -------------------------------------------------------------------
# 1. 用户检查
# -------------------------------------------------------------------
if [ "$(whoami)" != "www-data" ]; then
    echo -e "${RED}✘ This script must be run as www-data user.${NC}"
    echo -e "${YELLOW}Please run: sudo -u www-data ./start.sh${NC}"
    exit 1
fi

# -------------------------------------------------------------------
# 2. Maven 包装器检查
# -------------------------------------------------------------------
if [ ! -f "backend/mvnw" ]; then
    echo -e "${RED}✘ Maven wrapper (backend/mvnw) not found.${NC}"
    exit 1
fi
chmod +x backend/mvnw

# -------------------------------------------------------------------
# 3. 启动后端 (Spring Boot)
# -------------------------------------------------------------------
echo -e "${BLUE}...Starting Backend Service (Java API)...${NC}"

# 检查是否已有旧进程运行，如果有则先关闭（可选，但推荐）
if [ -f "backend.pid" ]; then
    OLD_PID=$(cat backend.pid)
    if ps -p $OLD_PID > /dev/null; then
        echo -e "${YELLOW}...Stopping previous backend instance (PID: $OLD_PID)...${NC}"
        kill $OLD_PID
        sleep 2
    fi
fi

# 设置 Maven 本地仓库路径（与 configure.sh 一致）
export MAVEN_OPTS="-Dmaven.repo.local=$(pwd)/.m2-repo"
mkdir -p "$(pwd)/.m2-repo"

cd backend || exit
# 使用 nohup 后台运行 Java 后端
nohup ./mvnw spring-boot:run > ../backend.log 2>&1 &
BACKEND_PID=$!
echo $BACKEND_PID > ../backend.pid
cd ..

if ps -p $BACKEND_PID > /dev/null; then
    echo -e "${GREEN}✔ Backend started with PID: $BACKEND_PID${NC}"
else
    echo -e "${RED}✘ Backend failed to start. Check backend.log for details.${NC}"
    exit 1
fi

# -------------------------------------------------------------------
# 4. 前端状态确认 (Apache Mode)
# -------------------------------------------------------------------
echo -e "\n${BLUE}...Frontend Status...${NC}"
if [ -d "front/dist" ]; then
    echo -e "${GREEN}✔ Frontend static files detected in front/dist/${NC}"
    echo -e "${CYAN}ℹ Frontend is being served by Apache (Port 80/443).${NC}"
else
    echo -e "${RED}✘ Frontend 'dist' folder not found!${NC}"
    echo -e "${YELLOW}Please run './configure.sh' or 'cd front && npm run build' first.${NC}"
fi

# -------------------------------------------------------------------
# 5. 完成提示
# -------------------------------------------------------------------
echo -e "\n${CYAN}==============================================${NC}"
echo -e "  Backend PID:  ${YELLOW}$BACKEND_PID${NC} (Log: backend.log)"
echo -e "  Site URL:     ${GREEN}http://magiccodelab.com${NC}"
echo -e "${CYAN}==============================================${NC}"
echo -e "${BLUE}To monitor backend logs: tail -f backend.log${NC}"
