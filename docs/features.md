# Features

This document lists the current project capabilities. It is intentionally
descriptive rather than aspirational.

## Public Blog

- Minimal terminal-like visual style.
- Home page, post pages, category pages, collection pages, search, and sidebar
  content.
- Public post lists exclude draft and unpublished posts.
- Post detail includes approved comments, media, and reaction counts.
- Search supports cancellation handling on the frontend.

## Admin

- Admin login with bearer token authentication.
- Token validation endpoint for frontend route guards.
- Post create, update, and delete.
- Category, collection, sidebar, social link, and maintenance management.
- Media upload and media library operations.

## Comments

- Visitor comments default to `PENDING`.
- Public lists show only `APPROVED` comments.
- `REJECTED` comments remain hidden from public pages.
- Admin comment list supports status filters, keyword search, pagination,
  statistics, and bulk approve/reject/delete actions.
- Admin-only moderation reason is available for rejected spam-like comments.

## Moderation

- Local link-count rule.
- Local blocked keyword rule.
- No external anti-spam service.
- No comment content is sent to third-party moderation APIs.

## Reactions

- Like/dislike records are deduplicated by post and client identifier.
- Repeating the same reaction does not increase counts.
- Switching between like and dislike updates the existing reaction record.

## API and Error Contracts

- OpenAPI / Swagger UI is available in development.
- Production disables OpenAPI docs by default.
- API errors use `message` and `errors`.
- Validation errors return field-level entries under `errors`.

## Operations

- Spring Boot Actuator health check.
- `X-Request-Id` response header and MDC logging.
- Production rolling log file.
- Docker and Docker Compose deployment files.
- Manual database migration strategy.
- Backup, restore, rollback, release, and production runbook docs.

## Testing and CI

- Backend tests use Spring Boot Test, MockMvc, H2, and test profile.
- Frontend tests use Vitest.
- CI runs backend tests, frontend install/audit/tests/build, static guards, and
  Docker build/config checks.
