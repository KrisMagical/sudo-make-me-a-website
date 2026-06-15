# Testing

The backend test suite uses Spring Boot Test, MockMvc, the `test` profile, and
H2 in MySQL compatibility mode. It must not depend on a local MySQL instance,
real OSS credentials, production secrets, or Docker.

Run backend tests:

```bash
cd backend
mvn test
```

Run with Maven Wrapper:

```bash
cd backend
./mvnw test
# Windows:
mvnw.cmd test
```

Current backend coverage focuses on:

- Comment validation, moderation states, admin filters, stats, bulk actions, and
  spam rules.
- Like/dislike duplicate prevention and reaction switching.
- Public and admin post flows, including hidden drafts/unpublished posts,
  creation, update, deletion, slug conflicts, and 404 contracts.
- Authentication boundaries, token validation, and bootstrap safety.
- Media upload/list/delete boundaries with a mocked OSS client.
- Maintenance mode status and update boundaries.
- Unified error response fields, request id headers, Actuator, and OpenAPI
  profile behavior.

Frontend tests use Vitest:

```bash
cd front
npm run test:run
```

Before release, also run `npm audit`, `npm run build`, and
`docker compose --env-file .env.example config --quiet`.
