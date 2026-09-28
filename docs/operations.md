# Operations Notes

Use `docs/production-runbook.md` for the full production deployment, upgrade,
rollback, and incident response flow. Use `docs/backup-restore.md` for database
backup and restore commands. Use `docs/traditional-deployment.md` when running
the backend as a systemd service and serving the frontend with Nginx or Apache.

## Health Checks

The backend includes Spring Boot Actuator for lightweight operational checks.

- `dev` and `test` profiles expose `/actuator/health` and `/actuator/info`.
- `prod` exposes `/actuator/health` only.
- Sensitive endpoints such as `/actuator/env`, `/actuator/beans`,
  `/actuator/configprops`, `/actuator/heapdump`, `/actuator/threaddump`, and
  broad metrics are not exposed by default in production.

Use `/actuator/health` for load balancer or uptime checks. A healthy app should
return `{"status":"UP"}`.

## Request Id

Every HTTP response includes `X-Request-Id`.

- If the client sends `X-Request-Id`, the backend reuses it.
- Otherwise the backend generates one.
- The same value is written to backend logs as `requestId`.

When investigating an issue, ask for the request id shown by the admin UI or
captured from the response headers.

## Logs

Production logs are written to:

- Console
- `logs/app.log`

The file log rotates by size and day, keeping a short history. The log format
includes timestamp, level, logger name, request id, and message.

Do not write secrets to logs or issues:

- No passwords
- No tokens or Authorization headers
- No database passwords
- No OSS secrets
- No full comment bodies or uploaded file contents

## Troubleshooting Flow

1. Get the user-visible error and `X-Request-Id` if available.
2. Search backend logs for that request id.
3. Check `/actuator/health`.
4. Check database connectivity and schema validation errors.
5. Check recent admin actions such as comment moderation, media operations, or
   maintenance mode changes.

Production Swagger UI and OpenAPI JSON are disabled by default. If API docs must
be inspected in production, enable them only behind a trusted network or
administrator-only access, then disable them again.
