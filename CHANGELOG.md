# Changelog

This project follows a simple human-readable changelog. Dates use ISO format.

## Unreleased

- Added comment moderation states and admin moderation workflows.
- Added backend validation for comment creation and unified API error responses.
- Added like/dislike deduplication and reaction switching.
- Removed default weak administrator initialization.
- Added environment-driven first admin bootstrap.
- Split development, production, and test profiles.
- Added manual production database migrations and bootstrap schema.
- Added Maven Wrapper, Dockerfiles, Docker Compose, and `.env.example`.
- Added OpenAPI / Swagger documentation.
- Added Actuator health checks, request id tracing, and rolling logs.
- Added production runbook, backup/restore docs, operations docs, testing docs,
  release checklist, Docker docs, and database migration docs.
- Upgraded frontend build tooling to Vite 8 and cleared npm audit findings.
- Added backend tests for comments, likes, posts, auth, media, maintenance,
  error contracts, request ids, Actuator, and OpenAPI.
- Added frontend tests for comments, auth storage behavior, date formatting,
  API errors, and search cancellation behavior.

## 0.1.0

- Initial maintained version baseline for the self-hosted blog system.
