# sudo-make-me-a-website

[![CI](https://github.com/KrisMagical/sudo-make-me-a-website/actions/workflows/ci.yml/badge.svg)](https://github.com/KrisMagical/sudo-make-me-a-website/actions/workflows/ci.yml)
![Java 21](https://img.shields.io/badge/Java-21-blue)
![Spring Boot 3.5](https://img.shields.io/badge/Spring%20Boot-3.5-green)
![Vue 3](https://img.shields.io/badge/Vue-3-42b883)
![Vite 8](https://img.shields.io/badge/Vite-8-646cff)
![Docker](https://img.shields.io/badge/Docker-ready-2496ed)
![License MIT](https://img.shields.io/badge/License-MIT-yellow)

A self-hosted minimalist blog system with comment moderation, admin tools,
Docker deployment, OpenAPI docs, and production operations guides.

The frontend keeps a Vim/terminal-inspired visual style. The backend is a
Spring Boot REST API designed for deliberate, manual production operations
rather than automatic schema mutation.

Screenshots can be added under `docs/assets/`.

## Features

- Minimal terminal-like public blog UI.
- Post management with categories and collections.
- Comment moderation with `PENDING`, `APPROVED`, and `REJECTED` states.
- Admin comment filters, search, pagination, stats, and bulk actions.
- Local anti-spam rules without sending comments to external services.
- Like/dislike deduplication and reaction switching.
- Media upload and media library support.
- Maintenance mode controls.
- Admin authentication with bearer tokens.
- Unified API error response using `message` and `errors`.
- OpenAPI / Swagger UI in development.
- `X-Request-Id` tracing, Actuator health checks, and rolling backend logs.
- Docker Compose deployment with MySQL, backend, and Nginx frontend.
- Manual database migration, backup, restore, rollback, and release docs.

## Tech Stack

- Backend: Java 21, Spring Boot 3.5.5, Spring Security, Spring Data JPA,
  MySQL 8, H2 for tests.
- Frontend: Vue 3, Vite 8, TypeScript, Pinia, UnoCSS.
- Media: Aliyun OSS-compatible upload integration.
- Ops: Docker, Docker Compose, Nginx, Spring Boot Actuator, Logback.
- Docs: OpenAPI / Swagger UI, production runbook, migration notes.

## Quick Start

### Local Development

Backend:

```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

Or use the Maven Wrapper:

```bash
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

Frontend:

```bash
cd front
npm ci
npm run dev
```

Vite 8 requires Node.js `^20.19.0 || >=22.12.0`. The Vite dev server proxies
`/api` to the backend target from `.backend-port`, or `http://localhost:8080`
if that file is absent.

### Docker Compose

```bash
cp .env.example .env
# Edit .env and replace every placeholder with deployment-specific values.
docker compose --env-file .env config
docker compose up -d --build
```

Production database schema changes are manual. For a new production database,
review and execute `docs/migrations/bootstrap-schema.sql` first. For an existing
database, run the numbered migrations in `docs/migrations` before starting the
new backend version. Containers do not run migrations automatically.

Use [docs/production-runbook.md](docs/production-runbook.md) as the production
deployment entry point.

### Traditional Source Deployment

If the server already has MySQL, Java, and Apache/Nginx, build from source and
let systemd run the backend. The configuration script can also set permissions,
write the systemd service, and create an Apache site when run with sudo:

```bash
sudo ./configure.sh
```

See [docs/traditional-deployment.md](docs/traditional-deployment.md) for the
full flow.

## API Documentation

In the `dev` profile:

- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

In the `prod` profile, Swagger UI and OpenAPI JSON are disabled by default. Do
not expose admin API docs publicly in production. See [docs/api.md](docs/api.md)
for the API overview.

## Testing

Backend:

```bash
cd backend
mvn test
```

Maven Wrapper:

```bash
cd backend
./mvnw test
# Windows:
mvnw.cmd test
```

Frontend:

```bash
cd front
npm ci
npm audit
npm run test:run
npm run build
```

Docker config:

```bash
docker compose --env-file .env.example config --quiet
```

Backend test scope is summarized in [docs/testing.md](docs/testing.md).

## Documentation

- [docs/architecture.md](docs/architecture.md) - system structure and request
  flow.
- [docs/features.md](docs/features.md) - feature inventory.
- [docs/api.md](docs/api.md) - API contract overview.
- [docs/docker.md](docs/docker.md) - Docker usage.
- [docs/traditional-deployment.md](docs/traditional-deployment.md) - source
  deployment with systemd plus Nginx or Apache.
- [docs/production-runbook.md](docs/production-runbook.md) - production
  deployment, upgrade, rollback, and incident flow.
- [docs/database-migration.md](docs/database-migration.md) - schema migration
  strategy.
- [docs/backup-restore.md](docs/backup-restore.md) - backup and restore notes.
- [docs/operations.md](docs/operations.md) - health checks, request ids, and
  logs.
- [docs/testing.md](docs/testing.md) - test strategy.
- [docs/release-checklist.md](docs/release-checklist.md) - release checklist.
- [docs/security-audit-notes.md](docs/security-audit-notes.md) - dependency
  audit notes.
- [CHANGELOG.md](CHANGELOG.md) - release history.
- [ROADMAP.md](ROADMAP.md) - planned work.

## Security Notes

- No default admin password is created during normal startup.
- Initial admin creation is controlled by `BLOG_ADMIN_USERNAME` and
  `BLOG_ADMIN_PASSWORD`.
- Remove admin bootstrap variables after the first admin account exists.
- Do not commit `.env`, logs, backups, tokens, database passwords, or OSS
  secrets.
- Production keeps `spring.jpa.hibernate.ddl-auto=validate` and
  `spring.sql.init.mode=never`.
- Production Swagger UI and OpenAPI JSON are disabled by default.
- Production exposes only the Actuator health endpoint by default.
- Database migrations are reviewed and executed manually.

Report security issues using [SECURITY.md](SECURITY.md).

## Contributing

Contributions are welcome when they keep the project small, self-hostable, and
maintainable. Start with [CONTRIBUTING.md](CONTRIBUTING.md), and use the Issue
and Pull Request templates when opening work on GitHub.

## License

MIT. See [LICENSE](LICENSE).
