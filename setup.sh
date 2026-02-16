#!/usr/bin/env bash

# ============================================================
#  SUDO-MAKE-ME-A-WEBSITE | ZERO-CONFIG LAUNCHER (OPTIMIZED)
# ============================================================

GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
CYAN='\033[0;36m'
NC='\033[0m'

# Paths
BACKEND_PROP="backend/src/main/resources/application.properties"
BACKEND_PROP_BAK="${BACKEND_PROP}.bak"
FRONT_ENV="front/.env"
FRONT_ENV_EXAMPLE="front/.env.example"
DATA_SQL_SOURCE="data.sql"
DATA_SQL_DEST="backend/src/main/resources/data.sql"

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
echo -e "${BLUE}>> System Auto-Configuration Protocol Initiated...${NC}\n"

# Helper: sed compatible with macOS/Linux
run_sed() {
    if [[ "$(uname)" == "Darwin" ]]; then
        sed -i '' "$1" "$2"
    else
        sed -i "$1" "$2"
    fi
}

# Helper: wait for MySQL container to be ready
wait_for_mysql() {
    local container=$1
    local password=$2
    echo -e "${YELLOW}...Waiting for MySQL to be ready...${NC}"
    until docker exec "$container" mysqladmin ping -h localhost -uroot -p"$password" --silent &>/dev/null; do
        sleep 1
    done
    echo -e "${GREEN}✔ MySQL is ready.${NC}"
}

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
#  Maven 包装器检查 (使用 ./mvnw)
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

# ============================================================
# 1. 数据库魔法 (Database Magic)
# ============================================================
echo -e "${YELLOW}[1/5] Database Configuration...${NC}"

DEFAULT_DB_NAME="local_database"
DEFAULT_DB_USER="root"
DEFAULT_DB_PASS="root"
DEFAULT_DB_PORT="3306"
DEFAULT_DB_HOST="localhost"

DB_NAME="$DEFAULT_DB_NAME"
DB_USER="$DEFAULT_DB_USER"
DB_PASS="$DEFAULT_DB_PASS"
DB_PORT="$DEFAULT_DB_PORT"
DB_HOST="$DEFAULT_DB_HOST"

echo -e "Do you have an existing MySQL database? (y/n)"
read -p "> " HAS_DB

if [[ "$HAS_DB" == "n" || "$HAS_DB" == "N" ]]; then
    if command -v docker &>/dev/null; then
        echo -e "${CYAN}★ Docker detected! The easiest way.${NC}"
        while true; do
            read -p "Spin up a MySQL container for you? (y/n): " USE_DOCKER
            case "$USE_DOCKER" in
                [Yy]*) USE_DOCKER="y"; break ;;
                [Nn]*) USE_DOCKER="n"; break ;;
                *) echo "Please answer y or n." ;;
            esac
        done

        if [[ "$USE_DOCKER" == "y" ]]; then
            echo -e "${BLUE}...Pulling and starting MySQL 8.0 container (utf8mb4)...${NC}"

            docker stop blog-mysql &>/dev/null
            docker rm blog-mysql &>/dev/null

            echo -e "${YELLOW}We'll create a MySQL container with:${NC}"
            echo -e "  Database: ${CYAN}$DB_NAME${NC}"
            echo -e "  Username: ${CYAN}root${NC}"
            echo -e "  Password: ${CYAN}root${NC} (you can change it later if needed)"
            echo ""

            if docker run --name blog-mysql \
                -e MYSQL_ROOT_PASSWORD=root \
                -e MYSQL_DATABASE="$DB_NAME" \
                -p 3306:3306 \
                --character-set-server=utf8mb4 \
                --collation-server=utf8mb4_unicode_ci \
                -d mysql:8.0; then
                echo -e "${GREEN}✔ MySQL Container 'blog-mysql' is running on port 3306.${NC}"
                echo -e "${GREEN}✔ Database '$DB_NAME' created automatically (utf8mb4).${NC}"
                DB_PASS="root"
                wait_for_mysql "blog-mysql" "$DB_PASS"
            else
                echo -e "${RED}✘ Failed to start MySQL container. Please check if port 3306 is already in use.${NC}"
                exit 1
            fi
        else
            echo -e "${YELLOW}Docker skipped. Falling back to local MySQL...${NC}"
            FALLBACK_TO_LOCAL=1
        fi
    fi

    if ! command -v docker &>/dev/null || [[ "$USE_DOCKER" == "n" ]] || [[ -n "$FALLBACK_TO_LOCAL" ]]; then
        echo -e "${YELLOW}Trying local MySQL client...${NC}"
        if command -v mysql &>/dev/null; then
            read -p "Enter your local MySQL root password to create DB: " -s LOCAL_ROOT_PASS
            echo ""
            echo -e "${BLUE}...Creating database '$DB_NAME' with utf8mb4...${NC}"

            if mysql -u root -p"$LOCAL_ROOT_PASS" -e "CREATE DATABASE IF NOT EXISTS \`$DB_NAME\` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"; then
                echo -e "${GREEN}✔ Local database '$DB_NAME' created (utf8mb4).${NC}"
                DB_PASS="$LOCAL_ROOT_PASS"
                DB_USER="root"
                DB_HOST="localhost"
                DB_PORT="3306"
            else
                echo -e "${RED}✘ Failed to create database. Please check your password.${NC}"
                exit 1
            fi
        else
            echo -e "${RED}✘ No Docker and no MySQL client found.${NC}"
            echo -e "${YELLOW}Please install MySQL to continue.${NC}"
            echo ""
            echo -e "${CYAN}Detected OS: $(uname)${NC}"
            if [[ "$(uname)" == "Darwin" ]]; then
                echo -e "macOS: Install MySQL with Homebrew:"
                echo -e "  ${YELLOW}brew install mysql${NC}"
                echo -e "  ${YELLOW}brew services start mysql${NC}"
                echo -e "  ${YELLOW}mysql_secure_installation${NC}  (set root password)"
            elif [[ -f /etc/debian_version ]]; then
                echo -e "Debian/Ubuntu:"
                echo -e "  ${YELLOW}sudo apt update${NC}"
                echo -e "  ${YELLOW}sudo apt install mysql-server${NC}"
                echo -e "  ${YELLOW}sudo systemctl start mysql${NC}"
            elif [[ -f /etc/redhat-release ]]; then
                echo -e "RHEL/CentOS:"
                echo -e "  ${YELLOW}sudo yum install mysql-server${NC}"
                echo -e "  ${YELLOW}sudo systemctl start mysqld${NC}"
            else
                echo -e "Please install MySQL manually for your operating system."
            fi
            echo ""
            echo -e "After installing MySQL, run this script again."
            exit 1
        fi
    fi
else
    echo -e "${CYAN}Please provide your existing database credentials:${NC}"
    read -p "Enter Database Host (default: localhost): " DB_HOST
    DB_HOST=${DB_HOST:-localhost}
    read -p "Enter Database Port (default: 3306): " DB_PORT
    DB_PORT=${DB_PORT:-3306}
    read -p "Enter Database Name (default: local_database): " DB_NAME
    DB_NAME=${DB_NAME:-local_database}
    read -p "Enter Database User (default: root): " DB_USER
    DB_USER=${DB_USER:-root}
    read -s -p "Enter Database Password: " DB_PASS
    echo ""
fi

echo -e "\n${YELLOW}=== Database Credentials Summary ===${NC}"
echo -e "  Host:     ${CYAN}$DB_HOST${NC}"
echo -e "  Port:     ${CYAN}$DB_PORT${NC}"
echo -e "  Database: ${CYAN}$DB_NAME${NC}"
echo -e "  Username: ${CYAN}$DB_USER${NC}"
echo -e "  Password: ${CYAN}$DB_PASS${NC}"
echo -e "${YELLOW}These credentials will be written to $BACKEND_PROP${NC}"
read -p "Press Enter to continue or Ctrl+C to abort..."

if [ -f "$BACKEND_PROP" ]; then
    cp "$BACKEND_PROP" "$BACKEND_PROP_BAK"
    echo -e "${CYAN}ℹ Backend config backed up to ${BACKEND_PROP_BAK}${NC}"
fi

NEW_JDBC_URL="jdbc:mysql://${DB_HOST}:${DB_PORT}/${DB_NAME}?useUnicode=true&characterEncoding=utf8mb4&useSSL=false&serverTimezone=UTC"
run_sed "s|^spring.datasource.url=.*|spring.datasource.url=${NEW_JDBC_URL}|" "$BACKEND_PROP"
run_sed "s|^spring.datasource.username=.*|spring.datasource.username=${DB_USER}|" "$BACKEND_PROP"
run_sed "s|^spring.datasource.password=.*|spring.datasource.password=${DB_PASS}|" "$BACKEND_PROP"
echo -e "${GREEN}✔ Database configuration updated.${NC}"

# ============================================================
# 2. 数据初始化 (Data Injection)
# ============================================================
echo -e "\n${YELLOW}[2/5] Handling Initial Data...${NC}"
if [ -f "$DATA_SQL_SOURCE" ]; then
    cp "$DATA_SQL_SOURCE" "$DATA_SQL_DEST"
    echo -e "${GREEN}✔ data.sql detected and moved to resources for auto-import.${NC}"
else
    echo -e "${CYAN}ℹ No data.sql found in root, skipping default data import.${NC}"
fi

# ============================================================
# 3. 前端配置 (Frontend)
# ============================================================
echo -e "\n${YELLOW}[3/5] Frontend Setup...${NC}"

if [ ! -f "$FRONT_ENV" ]; then
    if [ -f "$FRONT_ENV_EXAMPLE" ]; then
        cp "$FRONT_ENV_EXAMPLE" "$FRONT_ENV"
        echo -e "${CYAN}ℹ Created .env from .env.example${NC}"
    else
        echo "VITE_APP_TITLE=Vim-Style Blog" > "$FRONT_ENV"
        echo -e "${CYAN}ℹ Created default .env file${NC}"
    fi
fi

read -p "Enter Blog Title (default: Vim-Style Blog): " APP_TITLE
APP_TITLE=${APP_TITLE:-Vim-Style Blog}
if grep -q "^VITE_APP_TITLE=" "$FRONT_ENV"; then
    run_sed "s|^VITE_APP_TITLE=.*|VITE_APP_TITLE=${APP_TITLE}|" "$FRONT_ENV"
else
    echo "VITE_APP_TITLE=${APP_TITLE}" >> "$FRONT_ENV"
fi

read -p "Enter Footer Text (default: '© 2026 • Built with Vite + Vue'): " FOOTER_TEXT
FOOTER_TEXT=${FOOTER_TEXT:-© 2026 • Built with Vite + Vue}

if grep -q "^VITE_FOOTER_TEXT=" "$FRONT_ENV"; then
    run_sed "/^VITE_FOOTER_TEXT=/d" "$FRONT_ENV"
fi

FOOTER_TEXT_ESCAPED=$(printf "%s" "$FOOTER_TEXT" | sed 's/"/\\"/g')
echo "VITE_FOOTER_TEXT=\"$FOOTER_TEXT_ESCAPED\"" >> "$FRONT_ENV"
echo -e "${GREEN}✔ Footer text configured.${NC}"

echo "http://localhost:8080" > .backend-port
echo -e "${GREEN}✔ Frontend configured.${NC}"

# ============================================================
# 4. 自动启动 (Auto Launch) - 适用于无窗口服务器
# ============================================================
echo -e "\n${YELLOW}[4/5] Auto Launch (Optional)${NC}"
echo -e "Do you want to automatically start the backend and frontend in the background?"
read -p "This will run services with nohup and save logs to files. (y/n): " AUTO_START

if [[ "$AUTO_START" == "y" || "$AUTO_START" == "Y" ]]; then
    # --- Node.js 环境检查 ---
    echo -e "${YELLOW}...Checking Node.js environment...${NC}"
    if ! check_node_version; then
        echo -e "${RED}✘ Cannot start frontend due to Node.js issues.${NC}"
        echo -e "${CYAN}ℹ You can still start backend manually.${NC}"
        exit 1
    fi

    # --- 检查 Maven 包装器 ---
    echo -e "${YELLOW}...Checking Maven wrapper...${NC}"
    if ! _check_mvnw; then
        echo -e "${RED}✘ Cannot start backend due to missing mvnw.${NC}"
        exit 1
    fi

    echo -e "${BLUE}...Starting backend service with ./mvnw...${NC}"
    cd backend || exit
    # 确保 mvnw 可执行
    chmod +x mvnw
    nohup ./mvnw spring-boot:run > ../backend.log 2>&1 &
    BACKEND_PID=$!
    echo $BACKEND_PID > ../backend.pid
    echo -e "${GREEN}✔ Backend started with PID: $BACKEND_PID (logs: backend.log)${NC}"
    cd ..

    echo -e "${BLUE}...Starting frontend service...${NC}"
    cd front || exit
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

    echo -e "\n${CYAN}Services are running in the background.${NC}"
    echo -e "  Backend PID:  ${YELLOW}$BACKEND_PID${NC}  (stop with: kill $BACKEND_PID)"
    echo -e "  Frontend PID: ${YELLOW}$FRONTEND_PID${NC}  (stop with: kill $FRONTEND_PID)"
    echo -e "  Log files:    ${CYAN}backend.log, frontend.log${NC}"
    echo -e "\n${BLUE}To monitor output: tail -f backend.log${NC}"
else
    echo -e "${CYAN}ℹ Auto-launch skipped. You can manually start services using the commands below.${NC}"
fi

# ============================================================
# 5. 修改默认用户密码 (Change Default Password)
# ============================================================
echo -e "\n${YELLOW}[5/5] Default User Password Configuration...${NC}"
echo -e "Default user: ${CYAN}gosling${NC}"
read -p "Do you want to change the default user password? (y/n): " CHANGE_PASS

if [[ "$CHANGE_PASS" == "y" || "$CHANGE_PASS" == "Y" ]]; then
    while true; do
        read -s -p "Enter new password for user 'gosling': " NEW_PASS1
        echo ""
        read -s -p "Confirm new password: " NEW_PASS2
        echo ""

        if [[ "$NEW_PASS1" != "$NEW_PASS2" ]]; then
            echo -e "${RED}✘ Passwords do not match. Please try again.${NC}"
        elif [[ -z "$NEW_PASS1" ]]; then
            echo -e "${RED}✘ Password cannot be empty. Please try again.${NC}"
        else
            break
        fi
    done

    echo -e "${YELLOW}...Generating password hash...${NC}"

    if command -v htpasswd &>/dev/null; then
        HASH=$(htpasswd -bnBC 12 "" "$NEW_PASS1" | tr -d ':\n' | sed 's/^.*://')
        if [[ -z "$HASH" ]]; then
            echo -e "${YELLOW}htpasswd BCrypt failed, falling back to Python...${NC}"
            FALLBACK_HASH=1
        fi
    fi

    if [[ -n "$FALLBACK_HASH" ]] || ! command -v htpasswd &>/dev/null; then
        if command -v python3 &>/dev/null; then
            HASH=$(python3 -c "import bcrypt; print(bcrypt.hashpw('$NEW_PASS1'.encode(), bcrypt.gensalt(12)).decode())")
        elif command -v python &>/dev/null; then
            HASH=$(python -c "import bcrypt; print(bcrypt.hashpw('$NEW_PASS1'.encode(), bcrypt.gensalt(12)).decode())")
        else
            echo -e "${RED}✘ No BCrypt tool found. Please install:${NC}"
            echo -e "  ${YELLOW}sudo apt install apache2-utils   # for htpasswd${NC}"
            echo -e "  ${YELLOW}pip install bcrypt              # for Python${NC}"
            echo -e "${CYAN}ℹ You can manually update password later using SQL:${NC}"
            echo "UPDATE users SET password='bcrypt_hash' WHERE username='gosling';"
        fi
    fi

    if [[ -n "$HASH" ]]; then
        echo -e "${GREEN}✔ Password hash generated.${NC}"
        TMP_SQL="update-password.sql"
        cat > "$TMP_SQL" << EOF
UPDATE users SET password='$HASH' WHERE username='gosling';
EOF
        echo -e "${YELLOW}...Applying password update...${NC}"
        if [[ -n "$DB_PASS" ]]; then
            mysql -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" -p"$DB_PASS" "$DB_NAME" < "$TMP_SQL"
            if [ $? -eq 0 ]; then
                echo -e "${GREEN}✔ Password updated successfully for user 'gosling'.${NC}"
            else
                echo -e "${RED}✘ Failed to update password. Please check database connection.${NC}"
            fi
        else
            echo -e "${RED}✘ Database password not available. Cannot update password.${NC}"
        fi
        rm -f "$TMP_SQL"
    else
        echo -e "${YELLOW}Password not updated. You can manually update it later.${NC}"
    fi
else
    echo -e "${CYAN}ℹ Using default password for user 'gosling'.${NC}"
fi

# ============================================================
# 启动引导
# ============================================================
echo -e "\n${BLUE}==============================================${NC}"
echo -e "${GREEN}   SETUP COMPLETE. READY FOR TAKEOFF.      ${NC}"
echo -e "${BLUE}==============================================${NC}"
echo -e "Use these commands in separate terminals:"
echo -e "1. Backend: ${YELLOW}cd backend && ./mvnw spring-boot:run${NC}"
echo -e "2. Frontend: ${YELLOW}cd front && npm install && npm run dev${NC}"
echo -e "${YELLOW}Note: Frontend requires Node.js 20.19+ or 22.12+. Please verify your Node.js version before running manually.${NC}"
echo -e "\n(If you used Docker, the database is already running in the background!)"
