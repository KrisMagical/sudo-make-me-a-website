# Traditional Source Deployment

This guide explains how to deploy the project without Docker:

- Frontend: built Vue/Vite files served by Nginx or Apache.
- Backend: Spring Boot jar managed by systemd.
- Database: MySQL 8.
- Profile: `prod`.

Use this path when you want to manage Java, MySQL, and the web server directly
on the host. If you prefer containerized deployment, use `docs/docker.md` and
`docs/production-runbook.md`.

Production still uses manual database schema initialization and migrations. The
startup script does not run database migrations.

## 1. Install Runtime Dependencies

Ubuntu / Debian example:

```bash
sudo apt update
sudo apt install -y git curl mysql-server nginx
```

Install Java 21 and Node.js 22 or another version supported by Vite 8:

```bash
java -version
node -v
npm -v
```

Vite 8 requires Node.js `^20.19.0 || >=22.12.0`.

## 2. Clone the Project

```bash
cd /var/www
sudo git clone https://github.com/KrisMagical/sudo-make-me-a-website.git
sudo chown -R "$USER:$USER" sudo-make-me-a-website
cd sudo-make-me-a-website
chmod +x configure.sh start.sh backend/mvnw
```

## 3. Create the Production Database

Create a dedicated database and user. Do not use an empty root password and do
not use a shared administrator account for the application.

```bash
sudo mysql
```

```sql
CREATE DATABASE blog CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'blog_user'@'localhost' IDENTIFIED BY 'replace_with_a_strong_password';
GRANT ALL PRIVILEGES ON blog.* TO 'blog_user'@'localhost';
FLUSH PRIVILEGES;
```

## 4. Initialize or Migrate the Schema

For a fresh production database:

```bash
mysql -u blog_user -p blog < docs/migrations/bootstrap-schema.sql
```

For an existing production database, back up first and run the numbered SQL
migrations in `docs/migrations` in order.

Do not rely on Hibernate to create or update production tables. The prod
profile keeps `ddl-auto=validate` and `spring.sql.init.mode=never`.

## 5. Generate Runtime Configuration

Run the helper script:

```bash
./configure.sh
```

Recommended answers for production:

```text
Profile to prepare (dev/prod): prod
Database host: localhost
Database port: 3306
Database name: blog
Database username: blog_user
Database password: <strong database password>
Configure initial admin bootstrap variables now? y
Admin username: <admin username>
Admin password: <strong admin password>
Configure Aliyun OSS variables now? y or n, depending on your deployment
Backend port: 8080
Production API base URL: <empty when using same-origin reverse proxy>
Install frontend dependencies and build now? y
Build backend jar now? y
```

The script may create these local files:

- `.env.database`
- `.env.admin`
- `.env.oss`
- `.backend-port`
- `front/.env.production`

These files can contain secrets and must not be committed.

After the first admin account exists, remove `.env.admin` or remove
`BLOG_ADMIN_USERNAME` and `BLOG_ADMIN_PASSWORD` from the deployment
environment, then restart the backend.

## 6. Build Manually If Needed

If you skipped build steps in `configure.sh`, build manually.

Backend:

```bash
cd backend
./mvnw clean package -DskipTests
cd ..
```

Frontend:

```bash
cd front
npm ci
npm run build
cd ..
```

The frontend output is `front/dist`.

## 7. Run Backend with systemd

`start.sh prod` starts the built Spring Boot jar, but it should be managed by
systemd in production.

When `configure.sh` is run with sudo in the `prod` profile, it can automate
permissions, systemd setup, Apache setup, and service restart in the same
interactive flow:

```bash
sudo ./configure.sh
```

The script still does not create the database, initialize schema, or run
migrations. Run `bootstrap-schema.sql` or the numbered migrations before the
final production start.

If you prefer to perform the setup manually, use the steps below.

Create a service user if needed:

```bash
sudo useradd --system --home /var/www/sudo-make-me-a-website --shell /usr/sbin/nologin sudo-blog || true
sudo chown -R sudo-blog:sudo-blog /var/www/sudo-make-me-a-website
```

Create `/etc/systemd/system/sudo-blog.service`:

```ini
[Unit]
Description=sudo-make-me-a-website backend
After=network.target mysql.service

[Service]
Type=simple
User=sudo-blog
WorkingDirectory=/var/www/sudo-make-me-a-website
ExecStart=/var/www/sudo-make-me-a-website/start.sh prod
Restart=always
RestartSec=5

[Install]
WantedBy=multi-user.target
```

Enable and start:

```bash
sudo systemctl daemon-reload
sudo systemctl enable sudo-blog
sudo systemctl start sudo-blog
sudo systemctl status sudo-blog
```

Logs:

```bash
journalctl -u sudo-blog -f
```

Backend health check:

```bash
curl http://127.0.0.1:8080/actuator/health
```

## 8. Serve Frontend with Nginx

Create `/etc/nginx/sites-available/sudo-blog`:

```nginx
server {
    listen 80;
    server_name example.com;

    root /var/www/sudo-make-me-a-website/front/dist;
    index index.html;

    location / {
        try_files $uri $uri/ /index.html;
    }

    location /api/ {
        proxy_pass http://127.0.0.1:8080/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_set_header X-Request-Id $request_id;
    }

    location = /actuator/health {
        proxy_pass http://127.0.0.1:8080/actuator/health;
        proxy_set_header Host $host;
        proxy_set_header X-Request-Id $request_id;
    }

    access_log /var/log/nginx/sudo-blog-access.log;
    error_log /var/log/nginx/sudo-blog-error.log;
}
```

Enable it:

```bash
sudo ln -s /etc/nginx/sites-available/sudo-blog /etc/nginx/sites-enabled/sudo-blog
sudo nginx -t
sudo systemctl reload nginx
```

Replace `example.com` with your domain. If you do not have a domain yet, use the
server IP for testing and update the config later.

## 9. Serve Frontend with Apache

If you prefer Apache, install and enable required modules:

```bash
sudo apt install -y apache2
sudo a2enmod rewrite proxy proxy_http headers
sudo systemctl restart apache2
```

Create `/etc/apache2/sites-available/sudo-blog.conf`:

```apache
<VirtualHost *:80>
    ServerName example.com

    DocumentRoot /var/www/sudo-make-me-a-website/front/dist

    <Directory /var/www/sudo-make-me-a-website/front/dist>
        Options -Indexes +FollowSymLinks
        AllowOverride None
        Require all granted

        RewriteEngine On
        RewriteBase /
        RewriteRule ^index\.html$ - [L]
        RewriteCond %{REQUEST_FILENAME} !-f
        RewriteCond %{REQUEST_FILENAME} !-d
        RewriteRule . /index.html [L]
    </Directory>

    ProxyPreserveHost On
    ProxyPass /api/ http://127.0.0.1:8080/api/
    ProxyPassReverse /api/ http://127.0.0.1:8080/api/

    ProxyPass /actuator/health http://127.0.0.1:8080/actuator/health
    ProxyPassReverse /actuator/health http://127.0.0.1:8080/actuator/health

    RequestHeader set X-Forwarded-Proto expr=%{REQUEST_SCHEME}
    RequestHeader set X-Forwarded-Host expr=%{HTTP_HOST}

    ErrorLog ${APACHE_LOG_DIR}/sudo-blog-error.log
    CustomLog ${APACHE_LOG_DIR}/sudo-blog-access.log combined
</VirtualHost>
```

Enable it:

```bash
sudo a2dissite 000-default.conf
sudo a2ensite sudo-blog.conf
sudo apache2ctl configtest
sudo systemctl reload apache2
```

Use either Nginx or Apache on port 80, not both.

## 10. Add HTTPS

Nginx:

```bash
sudo apt install -y certbot python3-certbot-nginx
sudo certbot --nginx -d example.com
```

Apache:

```bash
sudo apt install -y certbot python3-certbot-apache
sudo certbot --apache -d example.com
```

## 11. Smoke Test

```bash
curl http://example.com/
curl http://example.com/actuator/health
curl http://example.com/api/posts
```

Also verify in the browser:

- Public home page loads.
- Refreshing a frontend route does not return 404.
- Admin login works.
- Visitor comments enter `PENDING`.
- Approved comments appear publicly.
- Repeated likes do not increase counts.
- Search works.
- Media library works.
- Maintenance mode can be enabled and disabled.
- Swagger UI is not exposed in production.

## 12. Updating the Deployment

```bash
cd /var/www/sudo-make-me-a-website
git pull

cd front
npm ci
npm run build
cd ..

cd backend
./mvnw clean package -DskipTests
cd ..

sudo systemctl restart sudo-blog
sudo systemctl reload nginx
```

If a release includes database migrations, back up the database first and run
the required SQL files before restarting the backend.

## 13. Security Notes

- Do not commit `.env.database`, `.env.admin`, `.env.oss`, logs, or backups.
- Do not use weak or shared database credentials.
- Do not keep admin bootstrap variables after the admin account exists.
- Do not expose MySQL to the public internet.
- Do not expose Swagger UI or sensitive Actuator endpoints in production.
- Do not log passwords, tokens, Authorization headers, or OSS secrets.
- Back up before every schema migration.
