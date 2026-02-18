# sudo-make-me-a-website

**Just want a blog? Run a few commands. It automatically sets up everything, including the database.**  
Need to customize? It'll take you 5 minutes. Then you're done.

---

## What is this?

`sudo-make-me-a-website` is a zero‑configuration launcher that gets you a fully functional blog in minutes.  
It handles the entire stack for you:

- **Backend** – Spring Boot (Java 21) with a pre‑configured MySQL database
- **Frontend** – Vite + Vue (modern, fast, hot‑reload in development, production‑ready static files)
- **Database** – Automatically creates a MySQL database (via Docker or your local installation) and imports sample data
- **Apache integration** – Generates a production Apache virtual host configuration with dynamic port detection, reverse proxy, and SPA routing.
- **Smart image handling** – Automatically compresses large images (>2MB) before upload to speed up page loads.
- **Simple setup** – Run the configuration script as `root`, then start the services as the `www-data` user. Everything is configured and ready.

No manual database creation, no environment file tweaks, no dependency hell.  
You just answer a few prompts and your blog is live.

---

## Prerequisites

- **Operating System** – Linux, macOS, or Windows (via WSL)
- **Java** – JDK **21** or later (required for Spring Boot)
- **Node.js** – version **20.19+** or **22.12+** (LTS recommended)
- **MySQL** or **Docker** (one of them is needed for the database)
- **Apache** (optional, but recommended for production) – the configuration script will generate a virtual host file for you.

> The configuration script will check Node.js and guide you if it's missing or outdated.  
> If you don't have MySQL locally, you can let the script spin up a Docker container automatically.

---

## Quick Start

1. **Clone the repository** (or download the zip):
   ```bash
   git clone https://github.com/yourname/sudo-make-me-a-website.git
   cd sudo-make-me-a-website
   ```

2. **Make the scripts executable**:
   ```bash
   chmod +x configure.sh start.sh
   chmod +x backend/mvnw
   ```

3. **Run the configuration script as root** (this sets up the database, environment files, frontend build, and optionally generates an Apache config):
   ```bash
   sudo ./configure.sh
   ```

4. **Follow the on‑screen prompts** – it will ask you:
    - Whether you have an existing MySQL database or want a new one
    - Your blog title and footer text
    - Whether to change the default user password (default username: `gosling`)
    - (Automatically) It will detect an available port for the backend (starting from 8080) and generate an Apache virtual host file.

5. **After configuration completes**, start the services as the `www-data` user:
   ```bash
   sudo -u www-data ./start.sh
   ```

6. **That's it!**  
   Your blog will be running at:
    - **Production mode (with Apache)**: http://your-server-ip (or your domain if configured)
    - **Backend API**: http://localhost:8080 (or the dynamically selected port)

   The backend runs in the background, with logs written to `backend.log`.

---

## Domain Name Resolution (Optional)

If you want to access your blog using a domain name (e.g., `http://magiccodelab.com`) **before** the domain's DNS records have propagated or for local testing, you need to edit your local machine's `hosts` file. This file maps domain names to IP addresses.

- **On Linux/macOS**: Edit `/etc/hosts` with superuser privileges:
  ```bash
  sudo nano /etc/hosts
  ```
- **On Windows**: Open `C:\Windows\System32\drivers\etc\hosts` as Administrator.

Add a line that maps your server's IP address to your desired domain, for example:
```
YOUR_SERVER_IP   magiccodelab.com
```

If you are testing on the same machine where the server runs (e.g., in a development environment), you can use `127.0.0.1` as the IP:
```
127.0.0.1   magiccodelab.com
```

After saving the file, you can open your browser and visit `http://magiccodelab.com` – it will resolve to the IP you set.

> **Note**: This change only affects your local machine. To make your domain accessible to everyone on the internet, you must configure DNS records with your domain registrar or DNS provider.

---

## What the Scripts Do

We now provide **two separate scripts** for a clean separation of concerns:

### `configure.sh` (run as `root`)
This script handles the **one‑time setup**:

1. **Database Magic**
    - Asks if you have an existing MySQL database.
    - If not, it offers to create one for you:
        - With **Docker** (if installed) → spins up a MySQL 8.0 container with UTF‑8 support.
        - Without Docker → tries to create a local database using your MySQL client.
    - Then it updates `backend/src/main/resources/application.properties` with your database credentials.

2. **Initial Data Injection**
    - If a file named `data.sql` exists in the project root, it is copied to the backend resources folder.  
      Spring Boot will automatically run it on startup, populating the database with sample content.

3. **Frontend Setup**
    - Creates a `.env` file in the `front` folder (based on `.env.example` if present).
    - Asks for your blog title and footer text, and writes them into the environment file.

4. **Default Password Change**
    - The default user is `gosling` (as in James Gosling, the father of Java).
    - You can optionally set a new password. The script generates a bcrypt hash and updates the database.

5. **Dependency Installation & Build**
    - Installs frontend dependencies and builds the static files (`front/dist`).
    - Downloads Maven dependencies for the backend.

6. **Ownership Adjustment**
    - Changes the project directory owner to `www-data:www-data` – a crucial step for production security.

7. **Apache Virtual Host Generation**
    - Detects an available port for the backend (starting from 8080).
    - Generates a complete Apache configuration file at `/etc/apache2/sites-available/magiccodelab.conf` with:
        - DocumentRoot pointing to `front/dist`
        - SPA routing (Rewrite rules)
        - Reverse proxy for `/api`, `/login`, and Swagger endpoints
        - Correct `ProxyPass` using the detected dynamic port
    - Enables the site and reloads Apache.

### `start.sh` (run as `www-data`)
This script **starts the backend service** in the background:

- Checks Node.js version (required for frontend, but in production the frontend is served by Apache).
- Starts the backend with `./mvnw spring-boot:run` (logs → `backend.log`).
- Saves the process PID to `backend.pid` for easy management.
- Verifies that the frontend static files (`front/dist`) exist (they were built by `configure.sh`).

---

## Running in Development Mode (without Apache)

If you prefer to use the Vite dev server during development, you can start the services manually:

### Backend (Spring Boot)
```bash
cd backend
./mvnw spring-boot:run
```
The backend will be available at http://localhost:8080 (or the next available port if 8080 is busy – the backend automatically adjusts).

### Frontend (Vite + Vue)
```bash
cd front
npm install      # only needed once
npm run dev
```
The frontend will be available at http://localhost:5173 (or the port shown in the terminal).  
API requests will be proxied to the backend automatically (see `vite.config.ts`).

---

## Stopping the Services (if started with `start.sh`)

If you used `start.sh`, the backend runs in the background.  
To stop it:

```bash
kill $(cat backend.pid)
```

Or use the PID shown when the script finished.

---

## Image Handling

When you upload images through the editor (in the admin panel), the frontend automatically:

- Creates a temporary blob URL for instant preview.
- If the image is larger than **2MB**, it is compressed to max dimensions 1200x1200 with quality 0.8 **before** uploading. This dramatically speeds up page loads and reduces storage.
- If you are creating a new post/page (no ID yet), images are queued and uploaded **after** the post/page is saved, with the blob URLs automatically replaced by the real image URLs.

No manual intervention needed – everything works seamlessly.

---

## Changing the Default User Password Later

You can change the password for user `gosling` at any time:

1. Connect to your MySQL database:
   ```bash
   mysql -h localhost -P 3306 -u root -p local_database
   ```
   (Use your actual host, port, database name and user.)

2. Generate a bcrypt hash of your new password.  
   One easy way is to use an online bcrypt generator, or run:
   ```bash
   htpasswd -bnBC 12 "" "your-new-password" | tr -d ':\n' | sed 's/^.*://'
   ```
   (You may need to install `apache2-utils` or `httpd-tools`.)

3. Update the password:
   ```sql
   UPDATE users SET password='the-generated-hash' WHERE username='gosling';
   ```

---

## Troubleshooting

### Node.js version not supported
The configuration script checks for Node.js 20.19+ or 22.12+. If yours is older, follow the printed instructions to upgrade or use `nvm`.  
The start script will also perform this check and refuse to run if Node.js is missing or unsupported.

### Docker container fails to start
Port 3306 might already be in use. Stop any local MySQL service or change the port mapping manually.

### Database connection errors
- Verify that MySQL is running.
- Check the credentials in `backend/src/main/resources/application.properties`.
- If you used Docker, ensure the container is up: `docker ps`.

### Frontend dependencies fail to install
Make sure you have a working internet connection. You can also try running `npm install` manually inside the `front` folder to see detailed errors.

### Permission denied when running `start.sh`
Make sure you run it with the correct user:
```bash
sudo -u www-data ./start.sh
```
Also ensure that the project files are owned by `www-data` (the configuration script does this automatically).

### Apache config not working
- Check that Apache is installed and the required modules are enabled:
  ```bash
  sudo a2enmod proxy proxy_http rewrite
  sudo systemctl restart apache2
  ```
- Verify the generated config file at `/etc/apache2/sites-available/magiccodelab.conf` and ensure the `ServerName` matches your domain.
- If you change the backend port manually, update the Apache config accordingly.

### Images show as broken after saving
This is usually because the temporary blob URLs were not replaced. In the browser console, check if the image uploads succeeded (look for network requests to `/api/posts/.../images`).  
If they failed, the images remain as blob URLs – you can re-upload them after the post/page is saved (the editor will now have a valid ID and upload immediately).

---

## What's Included

- **Backend** – Spring Boot with Spring Data JPA, Spring Web, MySQL driver, and a simple REST API for blog posts.
- **Frontend** – Vite + Vue 3, with a clean, Vim‑inspired design, and an editor with image/video embedding, LaTeX support, and auto‑compression of large images.
- **Sample data** – A few blog posts and one user (`gosling` with password `123456` unless you changed it).
- **Database** – Automatically configured with UTF‑8 support.
- **Apache integration** – Ready‑to‑use production virtual host configuration.

---

## License

This project is open source under the MIT license.  
Feel free to use it, modify it, and make it your own.

---

## Credits

Inspired by the idea that setting up a blog should be as easy as typing a few commands.  
And because typing `sudo make me a website` is way more fun than clicking through a CMS installer.

---

**Now go ahead, run `sudo ./configure.sh`, then `sudo -u www-data ./start.sh`, and start writing!**
