# sudo-make-me-a-website

A small personal blog stack with a Spring Boot backend and a Vue/Vite frontend. The project favors a minimal Vim/terminal-inspired interface and keeps the architecture intentionally simple.

## Stack

- Backend: Spring Boot, Spring Security, Spring Data JPA, MySQL
- Frontend: Vue 3, Vite, Pinia, UnoCSS
- Storage: Aliyun OSS for uploaded images
- Optional production proxy: Apache

## Security First

Production deployments must configure an administrator explicitly. The application no longer creates a public weak-password admin account during normal startup.

Set these environment variables before the first production start if you want the backend to create the initial administrator:

```bash
export BLOG_ADMIN_USERNAME="your-admin-name"
export BLOG_ADMIN_PASSWORD="use-a-strong-password"
```

If either variable is missing, no administrator is created automatically.

Do not use example passwords in production. Do not enable automatic sample data initialization in production. Do not use `ddl-auto=update` as a production migration strategy.

## Profiles

Common settings live in `backend/src/main/resources/application.properties`.

Development profile:

```properties
spring.jpa.hibernate.ddl-auto=update
spring.sql.init.mode=always
```

Run locally with:

```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

Production profile:

```properties
spring.jpa.hibernate.ddl-auto=validate
spring.sql.init.mode=never
```

The production start script runs the backend with `--spring.profiles.active=prod`.

This project does not currently include Flyway or Liquibase. For a maintained production blog, manage schema changes deliberately before deployment and avoid relying on Hibernate to mutate production tables.

## Local Development

Backend:

```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

Frontend:

```bash
cd front
npm install
npm run dev
```

The Vite dev server proxies `/api` to the backend target from `.backend-port`, or `http://localhost:8080` if that file is absent.

## Tests

Backend:

```bash
cd backend
mvn test
```

Frontend:

```bash
cd front
npm run test:run
```

Build frontend:

```bash
cd front
npm run build
```

## Comments

Visitor comments are moderated:

- New visitor comments are saved as `PENDING`.
- Public comment lists return only `APPROVED` comments.
- Admin users can approve, reject, or delete comments from the admin comment tools.
- Admin replies are published as `APPROVED`.

The frontend success message reflects the status returned by the backend.

## Reactions

Post like/dislike records are deduplicated by post and client identifier. Repeating the same reaction does not increase the count. Switching from like to dislike updates the existing record and recalculates counts.

## Production Deployment Checklist

- Set `SPRING_PROFILES_ACTIVE=prod` or use `start.sh`.
- Set strong `BLOG_ADMIN_USERNAME` and `BLOG_ADMIN_PASSWORD` for first admin creation.
- Remove admin bootstrap variables after the account exists if your deployment process allows it.
- Keep `spring.sql.init.mode=never` in production.
- Keep `spring.jpa.hibernate.ddl-auto=validate` or a stricter strategy in production.
- Configure real MySQL credentials outside source control.
- Configure OSS credentials through environment variables or a protected env file.
- Build frontend assets with `npm run build`.
- Run `mvn test` and `npm run test:run` before release.

## Scripts

`configure.sh` is the interactive setup helper for database, frontend build, backend build, Apache config, and OSS env creation.

`start.sh` starts the already-built backend JAR as `www-data` using the production profile. It expects:

- `backend/target/*.jar`
- `.backend-port`
- `.env.oss`
- `front/dist` for Apache/static serving

## Frontend Notes

The visual direction is intentionally minimal, monochrome, and terminal-like. Keep UI additions lightweight: inline errors, compact buttons, simple status labels, and existing toast feedback.

## License

MIT
