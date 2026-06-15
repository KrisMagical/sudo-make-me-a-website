# Release Checklist

## Before Release

1. Review `docs/production-runbook.md`.
2. Confirm `.env` was created from `.env.example`.
3. Confirm all placeholder passwords and secrets were replaced.
4. Confirm `.env` is not tracked by Git.
5. Confirm a rollback plan and database backup location are ready.
6. Run backend tests: `cd backend && mvn test`.
   The backend suite should cover posts, auth, media, maintenance mode,
   comments, likes, error contracts, request ids, Actuator, and OpenAPI.
7. Run Maven Wrapper tests: `cd backend && ./mvnw test` on Unix or
   `cd backend && mvnw.cmd test` on Windows. If the Unix executable bit is
   unavailable, use `bash ./mvnw test`.
8. Run frontend tests: `cd front && npm run test:run`.
9. Build frontend assets: `cd front && npm run build`.
10. Run frontend dependency audit: `cd front && npm audit`.
11. Confirm frontend Node.js satisfies `^20.19.0 || >=22.12.0` for Vite 8.
12. Review `docs/security-audit-notes.md` if audit reports new or deferred
   findings.
13. Check frontend build output for chunk warnings and confirm any large chunks
   are lazy-loaded or documented.
14. Build Docker images: `docker build -t blog-backend:test ./backend` and
   `docker build -t blog-frontend:test ./front`.
15. Validate compose config: `docker compose --env-file .env config`.
16. Confirm Docker healthchecks are defined for MySQL, backend, and frontend.
17. Confirm `.env` contains strong deployment secrets and is not committed.
18. Confirm CI passes for backend tests, frontend audit, frontend tests, build,
   and static guards.
19. Confirm OpenAPI docs work in dev/test and are disabled in the prod profile.
20. Confirm `/actuator/health` returns `UP`.
21. Confirm the prod profile does not expose sensitive actuator endpoints such
    as `/actuator/env`, `/actuator/beans`, or `/actuator/heapdump`.
22. Confirm the production log directory exists and is writable.
23. Confirm responses include `X-Request-Id`.
24. Confirm login failure logs do not print passwords, tokens, or Authorization
    headers.
25. Confirm `alert(` is not used for primary user feedback.
26. Confirm `localStorage.clear()` is not used for logout.
27. Confirm frontend errors do not rely on `response.data.error` as the primary
   field.
28. Confirm README files do not guide production users toward weak default
   passwords.
29. Confirm the prod profile keeps `ddl-auto=validate`.
30. Confirm the prod profile keeps `spring.sql.init.mode=never`.
31. Back up the production database.
32. For a fresh database, execute `docs/migrations/bootstrap-schema.sql`.
33. For an existing database, execute the required numbered SQL migrations from
    `docs/migrations`.
34. Set `BLOG_ADMIN_USERNAME` and `BLOG_ADMIN_PASSWORD` for first admin
    creation when needed.
35. Remove the admin bootstrap environment variables after the account exists
    if your deployment process allows it.
36. Run `docker compose up -d` and confirm services are healthy.
37. Manually verify login.
38. Manually verify visitor comments are created as `PENDING`.
39. Manually verify approved comments appear publicly.
40. Manually verify rejected comments stay hidden.
41. Manually verify admin comment status filters, search, stats, and bulk
    approve/reject/delete.
42. Manually verify obvious spam comments are rejected or flagged with a
    moderation reason.
43. Manually verify repeated likes do not increase counts.
44. Manually verify like/dislike switching updates counts correctly.
45. Manually verify post load failures show a user-visible error.
46. Manually verify canceled search requests do not show a failure state.
47. Manually verify media upload and media library access.
48. Manually verify maintenance mode can be enabled and disabled.
49. Manually verify frontend route refresh does not return 404.

## After Release

1. Observe backend and frontend logs for 10 to 15 minutes.
2. Check backend logs for schema validation errors.
3. Check backend logs for unexpected admin initialization errors.
4. Check backend logs for unexpected authentication or validation spikes.
5. Confirm request ids appear in responses and logs.
6. Verify login failure logs do not print passwords.
7. Verify `/actuator/health` is reachable and does not reveal sensitive detail.
8. Verify public post access.
9. Verify admin login.
10. Verify comment moderation.
11. Verify like/dislike behavior.
12. Verify media upload and media library operations.
13. Verify search behavior.
14. Verify maintenance mode.
15. Verify frontend route refresh does not return 404.

## Rollback Notes

1. Confirm whether database migrations were executed.
2. Confirm whether the previous version is compatible with the current schema.
3. Prepare the relevant database backup.
4. Prepare the previous image or previous code checkout.
5. Before rolling back code, confirm whether the database migration is
   compatible with the older code.
6. Do not drop new columns casually; moderation state may be lost.
7. If removing the like log unique constraint, first confirm duplicate reaction
   rows will not corrupt counts.
8. Run the smoke test after rollback.
