# Docker Deployment

This document explains the Docker files and common Compose commands. For the
full production process, use `docs/production-runbook.md` as the main entry.

This project ships Docker files for a simple single-host deployment:

- MySQL 8
- Spring Boot backend with the `prod` profile
- Nginx serving the Vue/Vite frontend and proxying `/api`

Production containers do not run database migrations automatically.

## Requirements

- Docker
- Docker Compose v2

## Quick Start

```bash
cp .env.example .env
```

Edit `.env` and replace every placeholder with strong deployment values. Do not
commit `.env`.

```bash
docker compose build
docker compose up -d
docker compose ps
```

You can also use the helper script:

```bash
chmod +x deploy-docker.sh
./deploy-docker.sh --frontend-port 127.0.0.1:8088
```

Use `127.0.0.1:8088` when a host-level Nginx or Apache process terminates HTTPS
and proxies traffic to the frontend container. Use `80` only when the frontend
container should bind directly to the public HTTP port.

For a fresh database, add `--init-db` and confirm the prompt:

```bash
./deploy-docker.sh --frontend-port 127.0.0.1:8088 --init-db
```

The helper still does not run existing-database migrations automatically.
It checks Docker, Compose, `.env`, MySQL health, and common port settings. If a
prerequisite is missing, it prints the next command to run instead of failing
silently.

## Database Initialization

For a fresh production database, review and run
`docs/migrations/bootstrap-schema.sql`.

For an existing production database, apply the numbered migrations in
`docs/migrations` in order. Always back up the database first.

The backend runs with `spring.jpa.hibernate.ddl-auto=validate` and
`spring.sql.init.mode=never`. Do not rely on Hibernate to create or alter
production tables.

## Administrator Bootstrap

To create the first administrator, set these in `.env` before the first backend
start:

```env
BLOG_ADMIN_USERNAME=
BLOG_ADMIN_PASSWORD=
```

Use a strong password. After the account exists, remove these variables from the
deployment environment if your process allows it, then restart the backend.

## Health Checks

- Frontend: `http://localhost:${FRONTEND_PORT:-80}/`
- Backend through Nginx: `http://localhost:${FRONTEND_PORT:-80}/actuator/health`
- Backend container internal health: `/actuator/health`

The production backend exposes only the health actuator endpoint by default.

## Logs

```bash
docker compose logs backend
docker compose logs frontend
docker compose logs mysql
```

The backend also writes rolling logs under `/app/logs`, backed by the
`backend-logs` Docker volume.

## Common Issues

### Database connection failed

Check `.env` values for `MYSQL_DATABASE`, `MYSQL_USER`, and `MYSQL_PASSWORD`.
Confirm the `mysql` service is healthy with `docker compose ps`.

### Schema validation failed

The production backend does not change schema automatically. Run
`bootstrap-schema.sql` for a new database, or apply the numbered migration files
for an existing database.

### Administrator was not created

Confirm `BLOG_ADMIN_USERNAME` and `BLOG_ADMIN_PASSWORD` were present before the
first backend start. Do not leave bootstrap credentials in the environment after
the admin account exists.

### Frontend cannot call the API

The frontend image uses Nginx to proxy `/api/` to the `backend` service. Check
`docker compose logs frontend` and `docker compose logs backend`.

### Healthcheck is unhealthy

Check backend logs, database health, and `/actuator/health`. If schema
validation fails, the backend will not become healthy.

### Docker Hub pull failed

Base image pulls can fail because of registry or network instability. Retry the
pull, configure a trusted registry mirror, pre-pull images from a stable network,
or rely on CI if CI can build images successfully. Do not commit image binaries
or Docker cache artifacts to Git.

## Security Notes

- Do not commit `.env`.
- Do not use an empty MySQL root password.
- Do not publish the MySQL port to the public internet.
- Do not expose Swagger UI in production.
- Do not expose sensitive actuator endpoints in production.
- Do not keep administrator bootstrap variables longer than needed.

Related docs:

- `docs/production-runbook.md`
- `docs/database-migration.md`
- `docs/backup-restore.md`
- `docs/operations.md`
