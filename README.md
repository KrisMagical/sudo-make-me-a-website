# sudo-make-me-a-website

**Just want a blog? Run a few commands. It automatically sets up everything, including the database.**  
Need to customize? It'll take you 5 minutes. Then you're done.

---

## What is this?

`sudo-make-me-a-website` is a zero‑configuration launcher that gets you a fully functional blog in minutes.  
It handles the entire stack for you:

- **Backend** – Spring Boot (Java 21) with a pre‑configured MySQL database
- **Frontend** – Vite + Vue (modern, fast, hot‑reload)
- **Database** – Automatically creates a MySQL database (via Docker or your local installation) and imports sample data
- **Simple setup** – Run the configuration script as `root`, then start the services as the `www-data` user. Everything is configured and ready.

No manual database creation, no environment file tweaks, no dependency hell.  
You just answer a few prompts and your blog is live.

---

## Prerequisites

- **Operating System** – Linux, macOS, or Windows (via WSL)
- **Java** – JDK **21** or later (required for Spring Boot)
- **Node.js** – version **20.19+** or **22.12+** (LTS recommended)
- **MySQL** or **Docker** (one of them is needed for the database)

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

3. **Run the configuration script as root** (this sets up the database, environment files, and optionally adjusts file permissions):
   ```bash
   sudo ./configure.sh
   ```

4. **Follow the on‑screen prompts** – it will ask you:
    - Whether you have an existing MySQL database or want a new one
    - Your blog title and footer text
    - Whether to change the default user password (default username: `gosling`)
    - Whether to change the project directory ownership to `www-data` (recommended for production)

5. **After configuration completes**, start the services as the `www-data` user:
   ```bash
   sudo -u www-data ./start.sh
   ```

6. **That's it!**  
   Your blog will be running at:
    - Frontend: http://localhost:5173 (or the port Vite chooses)
    - Backend API: http://localhost:8080

   Both services run in the background, with logs written to `backend.log` and `frontend.log`.

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

5. **Ownership Adjustment (Optional)**
    - If you choose to, the script will change the project directory owner to `www-data:www-data` – a crucial step for production security.

### `start.sh` (run as `www-data`)
This script **starts the backend and frontend services** in the background:

- Checks Node.js version and Maven wrapper.
- Starts the backend with `./mvnw spring-boot:run` (logs → `backend.log`).
- Installs frontend dependencies (`npm install`) and starts the Vite dev server (logs → `frontend.log`).
- Saves process PIDs to `backend.pid` and `frontend.pid` for easy management.

---

## Running the Services Manually (if you prefer)

After configuration, you can also start the services yourself without using `start.sh`:

### Backend (Spring Boot)
```bash
cd backend
./mvnw spring-boot:run
```
The backend will be available at http://localhost:8080

### Frontend (Vite + Vue)
```bash
cd front
npm install      # only needed once
npm run dev
```
The frontend will be available at http://localhost:5173 (or the port shown in the terminal)

---

## Stopping the Services (if started with `start.sh`)

If you used `start.sh`, the processes run in the background.  
To stop them:

```bash
kill $(cat backend.pid)
kill $(cat frontend.pid)
```

Or use the PIDs shown when the script finished.

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
Also ensure that the project files are owned by `www-data` (or at least readable). The configuration script can do this for you if you answered "yes" to the ownership question.

---

## What's Included

- **Backend** – Spring Boot with Spring Data JPA, Spring Web, MySQL driver, and a simple REST API for blog posts.
- **Frontend** – Vite + Vue 3, with a clean, Vim‑inspired design.
- **Sample data** – A few blog posts and one user (`gosling` with password `123456` unless you changed it).
- **Database** – Automatically configured with UTF‑8 support.

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