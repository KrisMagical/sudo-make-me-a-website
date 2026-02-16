#!/usr/bin/env bash
# ============================================================
#  START SCRIPT | Run as www-data to launch backend & frontend
# ============================================================

GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
CYAN='\033[0;36m'
NC='\033[0m'

# -------------------------------------------------------------------
#  Node.js 版本检查与安装指导
# -------------------------------------------------------------------
_print_node_install_instructions() {
    echo -e "${CYAN}Recommended installation methods for your OS:${NC}"
    if [[ "$(uname)" == "Darwin" ]]; then
        echo -e "  ${YELLOW}brew install node@22${NC}"
        echo -e "  ${YELLOW}brew link --overwrite node@22${NC}"
        echo -e "${CYAN}Or download installer from https://nodejs.org${NC}"
    elif [[ -f /etc/debian_version ]]; then
        echo -e "  ${YELLOW}curl -fsSL https://deb.nodesource.com/setup_22.x | sudo -E bash -${NC}"
        echo -e "  ${YELLOW}sudo apt install -y nodejs${NC}"
    elif [[ -f /etc/redhat-release ]]; then
        echo -e "  ${YELLOW}curl -fsSL https://rpm.nodesource.com/setup_22.x | sudo bash -${NC}"
        echo -e "  ${YELLOW}sudo yum install -y nodejs${NC}"
    else
        echo -e "  Please install Node.js 20.19+ or 22.12+ manually from https://nodejs.org"
    fi
    echo -e "${CYAN}Alternatively, you can use nvm (Node Version Manager):${NC}"
    echo -e "  ${YELLOW}curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.40.1/install.sh | bash${NC}"
    echo -e "  ${YELLOW}# After installation, restart your terminal or run: source ~/.bashrc${NC}"
    echo -e "  ${YELLOW}nvm install 20.19.0${NC}"
    echo -e "  ${YELLOW}nvm use 20.19.0${NC}"
    echo ""
}

_print_node_upgrade_instructions() {
    if command -v nvm &>/dev/null; then
        echo -e "${CYAN}You have nvm installed. Upgrade Node.js with:${NC}"
        echo -e "  ${YELLOW}nvm install 20.19.0${NC}"
        echo -e "  ${YELLOW}nvm alias default 20.19.0${NC}"
        echo -e "  ${YELLOW}nvm use 20.19.0${NC}"
        echo -e "${CYAN}Or install latest 22.x:${NC}"
        echo -e "  ${YELLOW}nvm install 22${NC}"
        echo -e "  ${YELLOW}nvm use 22${NC}"
    else
        echo -e "${CYAN}Recommended upgrade methods for your OS:${NC}"
        _print_node_install_instructions
    fi
}

check_node_version() {
    if ! command -v node &>/dev/null; then
        echo -e "${RED}✘ Node.js is not installed.${NC}"
        echo -e "${YELLOW}Please install Node.js 20.19 or higher (LTS recommended) to run the frontend.${NC}"
        echo ""
        _print_node_install_instructions
        return 1
    fi

    local node_version=$(node -v | sed 's/v//')
    local major_version=$(echo "$node_version" | cut -d. -f1)
    local minor_version=$(echo "$node_version" | cut -d. -f2)

    local valid=0
    if [[ $major_version -eq 20 && $minor_version -ge 19 ]]; then
        valid=1
    elif [[ $major_version -eq 22 && $minor_version -ge 12 ]]; then
        valid=1
    elif [[ $major_version -gt 22 ]]; then
        valid=1
    fi

    if [[ $valid -eq 0 ]]; then
        echo -e "${RED}✘ Node.js version $node_version is not supported.${NC}"
        echo -e "${YELLOW}This project requires Node.js 20.19+ or 22.12+ (LTS recommended).${NC}"
        echo ""
        _print_node_upgrade_instructions
        return 1
    fi

    echo -e "${GREEN}✔ Node.js version $node_version detected (OK).${NC}"
    return 0
}

# -------------------------------------------------------------------
#  Maven 包装器检查
# -------------------------------------------------------------------
_check_mvnw() {
    if [ ! -f "backend/mvnw" ]; then
        echo -e "${RED}✘ Maven wrapper (backend/mvnw) not found.${NC}"
        echo -e "${YELLOW}Please ensure the project structure is correct.${NC}"
        return 1
    fi
    chmod +x backend/mvnw
    echo -e "${GREEN}✔ Maven wrapper found and executable.${NC}"
    return 0
}

# Banner
clear
echo -e "${GREEN}"
echo "  _____           _         "
echo " / ____|         | |        "
echo "| (___  _   _  __| | ___    "
echo " \___ \| | | |/ _\` |/ _ \   "
echo " ____) | |_| | (_| | (_) |  "
echo "|_____/ \__,_|\__,_|\___/   "
echo -e "${NC}"
echo -e "${BLUE}>> Service Startup Mode Initiated (Run as www-data)${NC}\n"

# -------------------------------------------------------------------
# 用户检查
# -------------------------------------------------------------------
if [ "$(whoami)" != "www-data" ]; then
    echo -e "${RED}✘ This script must be run as www-data user.${NC}"
    echo -e "${YELLOW}Please run: sudo -u www-data ./start.sh${NC}"
    exit 1
fi

# -------------------------------------------------------------------
# 环境检查
# -------------------------------------------------------------------
echo -e "${YELLOW}...Checking Node.js environment...${NC}"
if ! check_node_version; then
    echo -e "${RED}✘ Cannot start frontend due to Node.js issues.${NC}"
    exit 1
fi

echo -e "${YELLOW}...Checking Maven wrapper...${NC}"
if ! _check_mvnw; then
    echo -e "${RED}✘ Cannot start backend due to missing mvnw.${NC}"
    exit 1
fi

# -------------------------------------------------------------------
# 启动后端
# -------------------------------------------------------------------
echo -e "${BLUE}...Starting backend service with ./mvnw...${NC}"

export MAVEN_OPTS="-Dmaven.repo.local=$(pwd)/.m2-repo"
mkdir -p "$(pwd)/.m2-repo"

cd backend || exit
chmod +x mvnw
nohup ./mvnw spring-boot:run > ../backend.log 2>&1 &
BACKEND_PID=$!
echo $BACKEND_PID > ../backend.pid
echo -e "${GREEN}✔ Backend started with PID: $BACKEND_PID (logs: backend.log)${NC}"
cd ..

# -------------------------------------------------------------------
# 启动前端
# -------------------------------------------------------------------
echo -e "${BLUE}...Starting frontend service...${NC}"
cd front || exit

export npm_config_cache="$(pwd)/../.npm-cache"
mkdir -p "$npm_config_cache"

if [ ! -d "node_modules" ]; then
    echo -e "${YELLOW}Installing frontend dependencies...${NC}"
    npm install > ../frontend-install.log 2>&1
    if [ $? -ne 0 ]; then
        echo -e "${RED}✘ npm install failed. Check frontend-install.log${NC}"
        exit 1
    fi
    echo -e "${GREEN}✔ Dependencies installed.${NC}"
fi
nohup npm run dev > ../frontend.log 2>&1 &
FRONTEND_PID=$!
echo $FRONTEND_PID > ../frontend.pid
echo -e "${GREEN}✔ Frontend started with PID: $FRONTEND_PID (logs: frontend.log)${NC}"

# -------------------------------------------------------------------
# 完成提示
# -------------------------------------------------------------------
echo -e "\n${CYAN}Services are running in the background.${NC}"
echo -e "  Backend PID:  ${YELLOW}$BACKEND_PID${NC}  (stop with: kill $BACKEND_PID)"
echo -e "  Frontend PID: ${YELLOW}$FRONTEND_PID${NC}  (stop with: kill $FRONTEND_PID)"
echo -e "  Log files:    ${CYAN}backend.log, frontend.log${NC}"
echo -e "\n${BLUE}To monitor output: tail -f backend.log${NC}"