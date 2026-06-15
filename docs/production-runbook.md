# Production Runbook

## Scope

This runbook covers a single-host Docker Compose deployment:

- Spring Boot backend with the `prod` profile
- MySQL 8
- Vue/Vite frontend served by Nginx
- Manual database bootstrap and migrations

It does not cover Kubernetes, managed cloud platforms, or provider-specific
networking. HTTPS, external reverse proxy, DNS, and firewall policy are owned by
the deployer.

## Prerequisites

- Docker and Docker Compose v2
- Persistent MySQL volume
- A reachable domain or internal access address
- Reverse proxy and HTTPS plan, if public traffic is expected
- Backup directory outside the Git repository
- Writable backend log volume or host log path
- A secure `.env` copied from `.env.example`

Do not commit `.env`, database dumps, logs, or local IDE files.

## Environment

Create the deployment environment file:

```bash
cp .env.example .env
```

Edit `.env` and replace all placeholders. Required values include:

- `MYSQL_DATABASE`
- `MYSQL_USER`
- `MYSQL_PASSWORD`
- `MYSQL_ROOT_PASSWORD`
- `BLOG_ADMIN_USERNAME`
- `BLOG_ADMIN_PASSWORD`
- `FRONTEND_PORT`
- OSS variables such as `OSS_ENDPOINT`, `OSS_ACCESS_KEY_ID`,
  `OSS_ACCESS_KEY_SECRET`, `OSS_BUCKET_NAME`, `OSS_CDN_DOMAIN`, and
  `OSS_REGION` if media uploads are used

`docker-compose.yml` builds `SPRING_DATASOURCE_URL`,
`SPRING_DATASOURCE_USERNAME`, and `SPRING_DATASOURCE_PASSWORD` from the MySQL
variables. If you run the backend outside Compose, set the Spring datasource
variables explicitly.

Rules:

- Do not use placeholder passwords.
- Do not use an empty MySQL root password.
- Do not commit `.env`.
- Remove `BLOG_ADMIN_USERNAME` and `BLOG_ADMIN_PASSWORD` after the first admin
  account exists if your deployment process allows it.

## Fresh Database Deployment

Use this flow for a new production database.

1. Prepare `.env`.
2. Start MySQL, or enter the MySQL container after Compose creates it.
3. Confirm the database and user exist.
4. Review and execute `docs/migrations/bootstrap-schema.sql`.
5. Start backend and frontend containers.
6. Check `/actuator/health`.
7. Log in to the admin area.
8. Confirm the administrator account was created.
9. Remove admin bootstrap variables from `.env`.
10. Restart the backend.
11. Verify posts, comments, reactions, media, and maintenance mode.

`bootstrap-schema.sql` creates schema only. It does not insert a default
administrator, weak password, or production data. Production must not use
`ddl-auto=update`.

## Existing Database Upgrade

Use this flow when upgrading an existing deployment.

1. Stop writes or enter a maintenance window.
2. Back up the database.
3. Apply numbered migrations in order, for example:
   - `001-comment-status-and-like-log-unique.sql`
   - `002-comment-moderation-reason.sql`
   - Later migration files as they are added
4. Build or pull the new application images.
5. Start the new containers.
6. Watch backend logs for schema validation errors.
7. Verify core flows.

Do not run migrations from container startup hooks. Apply them deliberately.

## Standard Release

1. Pull or check out the release code.
2. Review `docs/release-checklist.md`.
3. Run backend tests:

   ```bash
   cd backend
   mvn test
   ```

4. Run frontend checks:

   ```bash
   cd front
   npm ci
   npm audit
   npm run test:run
   npm run build
   ```

5. Validate Compose:

   ```bash
   docker compose --env-file .env config
   ```

6. Build images:

   ```bash
   docker compose build
   ```

7. Start or update services:

   ```bash
   docker compose up -d
   docker compose ps
   ```

8. Check health.
9. Run smoke tests.
10. Observe logs for at least 10 to 15 minutes.

## Smoke Test

- Frontend home page loads.
- Backend `/actuator/health` returns `UP`.
- Admin login works.
- Visitor comment submission creates `PENDING`.
- Admin approval makes the comment public.
- Rejected comments stay hidden.
- Repeated like/dislike does not increase counts.
- Switching like/dislike updates counts correctly.
- Search works.
- Media library is accessible.
- Maintenance mode can be enabled and disabled.
- Swagger UI and `/v3/api-docs` are not exposed in the `prod` profile.
- Refreshing frontend routes does not return 404.

## Rollback

Rollback can involve code, images, and database state. Treat them separately.

1. Identify whether any database migration was executed.
2. Confirm the old code is compatible with the current schema.
3. Prepare the previous image tag or previous code checkout.
4. If the old code is compatible, roll back the app image or code and restart.
5. If the old code is not compatible, restore the database from a backup before
   starting the old app.
6. Do not casually drop columns or constraints added by migrations.
7. Run the smoke test after rollback.

Code rollback does not automatically undo database changes.

## Backup Strategy

Create backups before every production migration and release.

Example with Docker Compose:

```bash
mkdir -p backups
docker compose exec -T mysql sh -c 'mysqldump -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" "$MYSQL_DATABASE"' > backups/blog_backup_YYYYMMDD_HHMMSS.sql
```

Use timestamped names such as:

```text
blog_backup_YYYYMMDD_HHMMSS.sql
```

Store backups outside Git. Production backups may contain user content,
administrator password hashes, comments, and media metadata. Encrypt backups if
they leave the host.

Regularly restore a backup in a non-production environment to prove it works.

## Restore

1. Stop application writes or stop services:

   ```bash
   docker compose stop backend frontend
   ```

2. Restore the dump:

   ```bash
   docker compose exec -T mysql sh -c 'mysql -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" "$MYSQL_DATABASE"' < backups/blog_backup_YYYYMMDD_HHMMSS.sql
   ```

3. Start services:

   ```bash
   docker compose up -d
   ```

4. Check `/actuator/health`.
5. Check backend logs.
6. Run the smoke test.

## Logs And Troubleshooting

Useful commands:

```bash
docker compose logs backend
docker compose logs frontend
docker compose logs mysql
```

Backend rolling logs are written under `/app/logs` in the backend container and
the `backend-logs` volume.

Every response includes `X-Request-Id`. To investigate an issue:

1. Capture the user-visible error and request id.
2. Search backend logs for that request id.
3. Check `/actuator/health`.
4. Check database connection and schema validation logs.
5. Check recent admin operations such as moderation, media changes, and
   maintenance mode updates.

Common issues:

- `schema validate failed`: execute the required bootstrap or migration SQL.
- `database connection refused`: check MySQL health, credentials, and network.
- `admin not created`: confirm bootstrap env existed before the first backend
  start.
- Frontend `502`: check backend health and Nginx proxy logs.
- Docker Hub pull failed: see the next section.
- MySQL unhealthy: check credentials, volume state, and MySQL logs.
- Nginx proxy failed: confirm the `backend` service is healthy.

## Docker Hub Pull Failures

This project has seen Docker Hub pulls for base images time out or close the
connection while building locally. That usually indicates a network or registry
access problem, not a Dockerfile problem.

Options:

- Retry `docker pull` for the base image.
- Configure a Docker registry mirror approved by your environment.
- Build from a network with stable Docker Hub access.
- Pre-pull base images before release work.
- Let CI be the source of truth if CI can pull and build images reliably.

Do not commit image binaries, Docker cache directories, or downloaded base image
archives to Git.

## Security Checklist

- `.env` is not committed.
- Production Swagger UI and OpenAPI JSON are disabled by default.
- Sensitive actuator endpoints are not exposed.
- MySQL is not published to the public internet.
- MySQL root password is not empty.
- No default weak administrator exists.
- Logs do not include passwords, tokens, Authorization headers, database
  passwords, OSS secrets, or full comment bodies.
- Admin bootstrap variables are removed after account creation.
- Database is backed up before migrations.
