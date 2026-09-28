# Security Audit Notes

## Frontend npm audit

Run from `front`:

```bash
npm audit
npm audit --audit-level=high
```

Use `npm audit fix` for patch, minor, and safe transitive dependency updates.
Do not use `npm audit fix --force` during release hardening unless the major
upgrade has been reviewed and the full test/build suite passes.

## Current Status

The previous high findings in the frontend build toolchain have been resolved.

- Previous package path: `vite` -> `esbuild`
- Previous severity: high
- Resolution: reviewed and upgraded Vite-related tooling to Vite 8
- Current `npm audit` result: 0 vulnerabilities

The upgrade was performed manually instead of using `npm audit fix --force`:

- `vite` upgraded to `8.0.16`
- `@vitejs/plugin-vue` upgraded to `6.0.7`
- `vitest` upgraded to `4.1.8`
- `esbuild` resolved to a patched version

Vite 8 requires Node `^20.19.0 || >=22.12.0`. CI and frontend docs should stay
aligned with that requirement.

## Ongoing Practice

1. Run `npm audit` before release.
2. Treat high or critical frontend dependency findings as blocking unless a
   documented compatibility issue prevents an immediate fix.
3. Avoid `npm audit fix --force` without a focused compatibility review.
4. Run `npm run test:run` and `npm run build` after dependency changes.
