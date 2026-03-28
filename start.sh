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
# 2. 后端 JAR 文件检查
# -------------------------------------------------------------------
JAR_FILE=$(find backend/target -name "*.jar" -not -name "*-sources.jar" -not -name "*-javadoc.jar" | head -1)
if [ -z "$JAR_FILE" ]; then
    echo -e "${RED}✘ Backend JAR file not found. Please run ./configure.sh first.${NC}"
    exit 1
fi
echo -e "${GREEN}✔ Found backend JAR: $JAR_FILE${NC}"

# -------------------------------------------------------------------
# 3. 加载 OSS 环境变量
# -------------------------------------------------------------------
OSS_ENV_FILE=".env.oss"

if [ -f "$OSS_ENV_FILE" ]; then
    echo -e "${BLUE}Loading OSS environment variables...${NC}"
    source "$OSS_ENV_FILE"
    
    # 验证关键变量是否已加载
    if [ -z "$OSS_ACCESS_KEY_ID" ] || [ -z "$OSS_ACCESS_KEY_SECRET" ]; then
        echo -e "${RED}✘ OSS variables not properly loaded from $OSS_ENV_FILE.${NC}"
        echo -e "${YELLOW}Please check file permissions (should be readable by www-data).${NC}"
        exit 1
    fi
    echo -e "${GREEN}✔ OSS variables loaded successfully.${NC}"
else
    echo -e "${RED}✘ Missing $OSS_ENV_FILE. Please run ./configure.sh first.${NC}"
    exit 1
fi

# -------------------------------------------------------------------
# 4. 读取后端端口（由 configure.sh 生成）
# -------------------------------------------------------------------
BACKEND_PORT_FILE=".backend-port"
if [ ! -f "$BACKEND_PORT_FILE" ]; then
    echo -e "${RED}✘ Backend port file ($BACKEND_PORT_FILE) not found. Please run ./configure.sh first.${NC}"
    exit 1
fi
BACKEND_PORT=$(cat "$BACKEND_PORT_FILE" | grep -oP '(?<=:)\d+' || echo "8080")
echo -e "${GREEN}✔ Using backend port: $BACKEND_PORT${NC}"

# -------------------------------------------------------------------
# 5. 启动后端 (Spring Boot) 使用指定端口
# -------------------------------------------------------------------
echo -e "${BLUE}...Starting Backend Service (Java API) on port $BACKEND_PORT...${NC}"

# 检查是否已有旧进程运行，如果有则先关闭
if [ -f "backend.pid" ]; then
    OLD_PID=$(cat backend.pid)
    if ps -p $OLD_PID > /dev/null; then
        echo -e "${YELLOW}...Stopping previous backend instance (PID: $OLD_PID)...${NC}"
        kill $OLD_PID
        sleep 2
    fi
fi

# 确保没有其他进程占用该端口
if command -v lsof &>/dev/null; then
    if lsof -Pi :$BACKEND_PORT -sTCP:LISTEN -t >/dev/null 2>&1; then
        echo -e "${YELLOW}...Port $BACKEND_PORT is still occupied, attempting to kill the occupying process...${NC}"
        kill -9 $(lsof -ti:$BACKEND_PORT) 2>/dev/null
        sleep 2
    fi
fi

# 运行 JAR 文件，记录 Java 进程 PID
nohup java -jar "$JAR_FILE" --server.port="$BACKEND_PORT" > backend.log 2>&1 &
BACKEND_PID=$!
echo $BACKEND_PID > backend.pid

if ps -p $BACKEND_PID > /dev/null; then
    echo -e "${GREEN}✔ Backend started with PID: $BACKEND_PID${NC}"
else
    echo -e "${RED}✘ Backend failed to start. Check backend.log for details.${NC}"
    exit 1
fi

# -------------------------------------------------------------------
# 6. 前端状态确认 (Apache Mode)
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
# 7. 完成提示
# -------------------------------------------------------------------
echo -e "\n${CYAN}==============================================${NC}"
echo -e "  Backend PID:  ${YELLOW}$BACKEND_PID${NC} (Log: backend.log)"
echo -e "  Backend Port: ${YELLOW}$BACKEND_PORT${NC}"
echo -e "  Site URL:     ${GREEN}http://magiccodelab.com${NC}"
echo -e "${CYAN}==============================================${NC}"
echo -e "${BLUE}To monitor backend logs: tail -f backend.log${NC}"
