# Frontend

Vue 3 + Vite frontend for the personal blog.

## Requirements

- Node.js 20.19+ or 22.12+
- npm
- Backend API running locally or reachable through `VITE_API_BASE_URL`

## Setup

```bash
npm install
```

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

## API Configuration

For production builds, set:

```env
VITE_API_BASE_URL=https://your-domain.example
```

Leave it empty when the frontend is served behind the same origin as the backend reverse proxy.

## Notes

- The interface intentionally keeps a minimal Vim/terminal-inspired style.
- Use the shared error helpers in `src/utils/apiError.ts` for API failures.
- Use `src/utils/date.ts` for stable date formatting instead of browser locale defaults.
