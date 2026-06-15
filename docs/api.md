# API Documentation

## OpenAPI

In the `dev` and `test` profiles, the backend exposes generated OpenAPI docs:

- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

The `prod` profile disables Swagger UI and OpenAPI JSON by default:

```properties
springdoc.swagger-ui.enabled=false
springdoc.api-docs.enabled=false
```

Do not expose admin API documentation publicly in production. If production
documentation must be enabled temporarily, put it behind a trusted network or
administrator-only protection and disable it again after use.

## Authentication

Admin users log in through:

- `POST /login`

The response includes a JWT token. Admin endpoints use:

```http
Authorization: Bearer <token>
```

Do not put real tokens, passwords, database URLs, or secret values in examples,
logs, or screenshots.

## Public API Overview

- Posts: list recent posts, list by category, get post detail, get reaction
  counts, react with like/dislike.
- Comments: submit a visitor comment and list approved comments for a post.
- Search: search posts, categories, and collections through the public search
  endpoints.
- Social and sidebar: read social links, home profile, sidebar config, browser
  icon config, and public media metadata.
- Maintenance: read the current maintenance mode status.

## Admin API Overview

Admin endpoints require bearer authentication unless noted otherwise:

- Auth: validate the current token with `GET /api/admin/auth/me`.
- Posts: create, update, delete posts; manage categories and collections.
- Comments: create admin comments, list comments by status, search comments,
  view moderation stats, bulk approve/reject/delete, and delete individual
  comments.
- Media: upload and delete post, home, avatar, favicon, and icon images.
- Maintenance: update maintenance mode.
- Config: update home profile, sidebar, browser icon, and site configuration.

## Comment Moderation

Visitor comments are created with status `PENDING`.

- `PENDING`: waiting for review.
- `APPROVED`: visible in public comment lists.
- `REJECTED`: hidden from public comment lists.

Public comment list endpoints return only `APPROVED` comments.

Admins can filter moderation lists by status (`PENDING`, `APPROVED`,
`REJECTED`, `ALL`), keyword, post id, page, size, and creation sort. The admin
stats endpoint returns pending, approved, rejected, and total counts.

Bulk moderation accepts `APPROVE`, `REJECT`, and `DELETE`. If no comments are
selected, clients should not send the request.

## Local Anti-Spam Rules

The backend applies lightweight local checks before storing visitor comments.
No external anti-spam service is used, and comment content is not sent to third
parties.

Configuration:

```properties
blog.comment.moderation.max-links=2
blog.comment.moderation.blocked-keywords=
blog.comment.moderation.auto-reject-blocked-keywords=true
```

Rules currently reject comments with too many links, very short link-only
content, abnormal repeated characters, or configured blocked keywords. Rejected
comments are hidden publicly. Admin moderation lists may show a short
`moderationReason`, such as `too many links`.

## Error Responses

Errors use the same top-level fields across the API:

```json
{
  "message": "Validation failed",
  "errors": {
    "email": "must be a well-formed email address"
  }
}
```

Common status codes:

- `400`: invalid request or validation failure.
- `401`: missing or invalid authentication.
- `403`: authenticated but not allowed.
- `404`: resource not found or disabled endpoint.
- `409`: business conflict, such as duplicate reaction.
- `500`: unexpected server error.

Before release, run the checks in [release-checklist.md](release-checklist.md).
