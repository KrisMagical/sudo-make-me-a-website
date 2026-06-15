# Contributing

Thanks for considering a contribution. This project is a self-hosted personal
blog system, so changes should keep it small, understandable, and easy to run.

## Before You Start

- Check existing issues and docs before opening a new issue.
- For larger changes, open an issue first and describe the intended behavior.
- Keep the Vim/terminal-inspired frontend style intact.
- Do not introduce a large UI framework or rewrite the application structure.
- Do not add external services for comment moderation or error tracking by
  default.

## Local Checks

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

## Pull Request Guidelines

- Keep changes focused.
- Update tests for behavior changes.
- Update docs when configuration, deployment, or API behavior changes.
- Do not commit `.env`, logs, backups, `node_modules`, `dist`, `target`, or IDE
  files.
- Do not add real tokens, passwords, database URLs, or OSS secrets.
- Keep production migrations manual; do not run database migrations from
  container startup.

## Commit Style

No strict commit convention is required, but concise prefixes help:

- `fix:` for bug fixes.
- `feat:` for small user-visible additions.
- `test:` for test-only changes.
- `docs:` for documentation.
- `ops:` for Docker, CI, deployment, and operations changes.

## Security

Do not open public issues for sensitive vulnerabilities. Follow
[SECURITY.md](SECURITY.md).
