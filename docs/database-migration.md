# Database Migration Guide

Production uses `spring.jpa.hibernate.ddl-auto=validate` and
`spring.sql.init.mode=never`. Hibernate validates that the schema matches the
entities, but it must not mutate production tables automatically.

## Why Not ddl-auto=update In Production

`ddl-auto=update` can make unexpected table changes during application startup.
That is convenient during local development, but risky for a maintained blog
with real content. Production schema changes should be reviewed, backed up, and
executed deliberately.

## Current Strategy

- `dev`: `ddl-auto=update`, optional development seed data from `data.sql`.
- `test`: H2 in-memory database from `backend/src/test/resources`.
- `prod`: `ddl-auto=validate`, `spring.sql.init.mode=never`, manual SQL
  migrations from `docs/migrations`.

For a brand-new production database, start from
`docs/migrations/bootstrap-schema.sql`. For an existing production database,
apply only the numbered migration files that have not already been applied.

Flyway or Liquibase is not currently part of this project. Until a migration
tool is introduced, treat `docs/migrations` as the ordered source of production
DDL changes.

## Standard Production Upgrade Flow

1. Back up the production database.
2. Stop the application or enter a maintenance window.
3. Review and execute the required SQL files in `docs/migrations` in order.
4. Start the application with the `prod` profile.
5. Confirm logs contain no schema validation errors.
6. Verify login, public post access, comment moderation, and like/dislike
   behavior.

Do not rely on Hibernate to alter production tables during startup.

## Fresh Production Database

For the first deployment to an empty MySQL 8 database:

1. Create the database and user outside the application.
2. Review `docs/migrations/bootstrap-schema.sql`.
3. Execute the bootstrap SQL manually.
4. Start the backend with the `prod` profile.
5. Set `BLOG_ADMIN_USERNAME` and `BLOG_ADMIN_PASSWORD` only for the initial
   administrator bootstrap, then remove them after creation.

The bootstrap schema creates tables and constraints only. It does not insert a
default administrator or sample production data.

## Migration 001

`docs/migrations/001-comment-status-and-like-log-unique.sql` covers the recent
moderation and reaction changes:

- Adds `comments.status`.
- Marks legacy comments as `APPROVED` so existing public comments remain
  visible.
- Sets the column default to `PENDING` for new rows inserted outside the
  application.
- Adds `uk_like_logs_post_identifier` on `like_logs(post_id, identifier)`.

The unique constraint matches the service rule: one reaction row per post and
client identifier. Repeating the same reaction does not increase counts, and
switching between like/dislike updates the existing row.

## Duplicate like_logs Data

The unique constraint will fail if historical duplicate rows already exist.
Check first:

```sql
SELECT post_id, identifier, COUNT(*) AS duplicate_count
FROM like_logs
GROUP BY post_id, identifier
HAVING COUNT(*) > 1;
```

If duplicates exist, inspect them and decide which record should remain. The
migration file includes a commented cleanup example that keeps the newest row,
but do not run cleanup SQL blindly on production data.

After cleanup, recount post reactions if needed by comparing `like_logs` with
the `posts.like_count` and `posts.dislike_count` columns.

## Migration 002

`docs/migrations/002-comment-moderation-reason.sql` adds
`comments.moderation_reason` for lightweight local anti-spam explanations in
the admin moderation view.

The column is nullable. Existing comments do not need backfill data.

## Rollback Notes

Rolling back application code after a schema migration requires compatibility
checking. The new `comments.status` and `comments.moderation_reason` columns are
generally safe to keep. Dropping them can lose moderation state or review
context. Removing the like log unique constraint can reintroduce duplicate
reaction data, so only do that after confirming the old code path and data risk.
