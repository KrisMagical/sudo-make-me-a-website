# Frontend

Vue 3 + Vite 8 frontend for the personal blog.

## Requirements

- Node.js `^20.19.0 || >=22.12.0`
- npm
- Backend API running locally or reachable through `VITE_API_BASE_URL`

## Setup

```bash
npm install
```

Use `npm ci` in CI or fresh release verification when `package-lock.json` is present.

## Development

```bash
npm run dev
```

The dev server proxies `/api` to the backend target configured in `.backend-port`, falling back to `http://localhost:8080`.

## Build

```bash
npm run build
```

## Tests

```bash
npm run test:run
```

Vitest covers core frontend behavior such as comment submission feedback, auth logout storage handling, API cancellation detection, and stable date formatting.

## Dependency Audit

```bash
npm audit
npm audit --audit-level=high
```

Use `npm audit fix` for safe patch/minor updates. Avoid `npm audit fix --force`
unless the suggested major upgrade has been reviewed and `npm run test:run` plus
`npm run build` both pass. CI treats high or critical audit findings as blocking
when `npm audit` is clean.

## Bundle Notes

Routes are lazy-loaded, and Vite splits CodeMirror, editor, markdown, math, HTTP,
and Vue vendor code into separate chunks. Large lazy-loaded editor chunks are
less urgent than a large first-load application chunk, but they should still be
reviewed during release checks.

## API Configuration

For production builds, set:

```env
VITE_API_BASE_URL=https://your-domain.example
```

Leave it empty when the frontend is served behind the same origin as the backend reverse proxy.

For local development, `.backend-port` can point the Vite proxy at the backend, for example `http://localhost:8080`.

## API Types

Shared frontend API contracts live in `src/types/api.ts`. Keep those types in
sync with the backend OpenAPI schemas and DTO field names. The backend OpenAPI
docs are available in the dev profile at `/swagger-ui/index.html` and
`/v3/api-docs`.

## Notes

- The interface intentionally keeps a minimal Vim/terminal-inspired style.
- Use the shared error helpers in `src/utils/apiError.ts` for API failures.
- Use `src/utils/date.ts` for stable date formatting instead of browser locale defaults.
