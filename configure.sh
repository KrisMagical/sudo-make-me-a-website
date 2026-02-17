#!/usr/bin/env bash
# ============================================================
#  CONFIGURE SCRIPT | Run as root to set up the environment
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
echo -e "${BLUE}>> Configuration Mode Initiated (Run as root)${NC}\n"

# ============================================================
# 1. 数据库配置
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
# 2. 数据初始化
# ============================================================
echo -e "\n${YELLOW}[2/5] Handling Initial Data...${NC}"
if [ -f "$DATA_SQL_SOURCE" ]; then
    # 先复制文件到目标位置，后续在第4步修改目标文件
    cp "$DATA_SQL_SOURCE" "$DATA_SQL_DEST"
    echo -e "${GREEN}✔ data.sql detected and moved to resources for auto-import.${NC}"
else
    echo -e "${CYAN}ℹ No data.sql found in root, skipping default data import.${NC}"
fi

# ============================================================
# 3. 前端配置
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
# 4. 修改默认用户凭证 (通过修改 data.sql 实现)
# ============================================================
echo -e "\n${YELLOW}[4/6] Default User Credentials Configuration...${NC}"
echo -e "Default user in data.sql: ${CYAN}gosling${NC}"

# 检查目标文件是否存在
if [ ! -f "$DATA_SQL_DEST" ]; then
    echo -e "${RED}✘ Target data.sql not found at $DATA_SQL_DEST. Skipping credential update.${NC}"
else
    read -p "Do you want to change the default user's username or password? (y/n): " CHANGE_CRED

    if [[ "$CHANGE_CRED" == "y" || "$CHANGE_CRED" == "Y" ]]; then
        # 1. 获取新用户名
        read -p "Enter new username (press Enter to keep 'gosling'): " NEW_USERNAME
        NEW_USERNAME=${NEW_USERNAME:-gosling}
        
        # 2. 获取新密码并生成 Hash
        read -p "Do you want to change the password? (y/n): " CHANGE_PASS
        if [[ "$CHANGE_PASS" == "y" || "$CHANGE_PASS" == "Y" ]]; then
            while true; do
                read -s -p "Enter new password: " NEW_PASS1
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

            HASH=""
            if command -v htpasswd &>/dev/null; then
                HASH=$(htpasswd -bnBC 12 "" "$NEW_PASS1" | tr -d ':\n' | sed 's/^.*://')
            fi

            if [[ -z "$HASH" ]]; then
                if command -v python3 &>/dev/null; then
                    HASH=$(python3 -c "import bcrypt; print(bcrypt.hashpw('$NEW_PASS1'.encode(), bcrypt.gensalt(12)).decode())")
                elif command -v python &>/dev/null; then
                    HASH=$(python -c "import bcrypt; print(bcrypt.hashpw('$NEW_PASS1'.encode(), bcrypt.gensalt(12)).decode())")
                else
                    echo -e "${RED}✘ No BCrypt tool found (htpasswd/python). Skipping password update.${NC}"
                fi
            fi
        fi

        # 3. 修改 data.sql 文件
        echo -e "${YELLOW}...Updating data.sql with new credentials...${NC}"
        
        # 默认值 (来自原始 data.sql)
        DEFAULT_USER_VAL="gosling"
        DEFAULT_HASH_VAL='\$2a\$12\$zBfG6tE.mgR28EON4eKQqeLJVwLn.aL5e213vvar8tA4fLcVFcJ1q'

        # 替换用户名
        if [ "$NEW_USERNAME" != "$DEFAULT_USER_VAL" ]; then
            run_sed "s|\"${DEFAULT_USER_VAL}\"|\"${NEW_USERNAME}\"|g" "$DATA_SQL_DEST"
            echo -e "${GREEN}✔ Username updated to '${NEW_USERNAME}' in data.sql.${NC}"
        fi

        # 替换密码 Hash
        if [ -n "$HASH" ]; then
            # 转义新 Hash 中的 $ 符号，防止 sed 解析错误
            ESCAPED_NEW_HASH=$(echo "$HASH" | sed 's/\$/\\$/g')
            run_sed "s|${DEFAULT_HASH_VAL}|${ESCAPED_NEW_HASH}|g" "$DATA_SQL_DEST"
            echo -e "${GREEN}✔ Password hash updated in data.sql.${NC}"
        fi
        
    else
        echo -e "${CYAN}Using default credentials (username: gosling).${NC}"
    fi
fi

# ============================================================
# 5. 安装依赖
# ============================================================
echo -e "\n${YELLOW}[5/6] Installing dependencies as root...${NC}"

# 前端依赖
echo -e "${YELLOW}...Installing frontend dependencies...${NC}"
cd front || exit
# 显式指定缓存位置，确保 root 也有地方写缓存
export npm_config_cache="$(pwd)/../.npm-cache"
npm install > ../frontend-install.log 2>&1
if [ $? -ne 0 ]; then
    echo -e "${RED}✘ npm install failed. Check frontend-install.log${NC}"
    exit 1
fi
echo -e "${GREEN}✔ Frontend dependencies installed.${NC}"
cd ..

# 后端 Maven 依赖
echo -e "${YELLOW}...Downloading backend dependencies (Maven)...${NC}"
cd backend || exit
# 显式指定 Maven 本地仓库路径
export MAVEN_OPTS="-Dmaven.repo.local=$(pwd)/../.m2-repo"
./mvnw dependency:go-offline > ../backend-deps.log 2>&1
if [ $? -ne 0 ]; then
    echo -e "${RED}✘ Maven dependency download failed. Check backend-deps.log${NC}"
    exit 1
fi
echo -e "${GREEN}✔ Backend dependencies downloaded.${NC}"
cd ..

# ============================================================
# 6. 权限
# ============================================================
echo -e "\n${YELLOW}[6/6] FINAL STEP: Adjusting all permissions for www-data...${NC}"

if id "www-data" &>/dev/null; then
    chown -R www-data:www-data .
    chmod -R 775 .
    echo -e "${GREEN}✔ Project ownership and permissions set to www-data:775.${NC}"

    if [ -d "/var/www" ]; then
        mkdir -p /var/www/.m2
        chown -R www-data:www-data /var/www/.m2
        echo -e "${GREEN}✔ System Fix: /var/www/.m2 is ready.${NC}"
    fi
else
    echo -e "${RED}✘ User www-data not found. You may need to manual fix permissions.${NC}"
fi

# ============================================================
# 完成
# ============================================================
echo -e "\n${BLUE}==============================================${NC}"
echo -e "${GREEN}   CONFIGURATION COMPLETE.${NC}"
echo -e "${BLUE}==============================================${NC}"
echo -e "Now start services as www-data:"
echo -e "  ${YELLOW}sudo -u www-data ./start.sh${NC}"
