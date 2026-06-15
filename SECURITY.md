# Security Policy

## Supported Versions

The `main` branch is the maintained development line. Tagged release support can
be defined once the project starts publishing versioned releases.

## Reporting a Vulnerability

Please do not open a public issue for a vulnerability that could expose admin
access, tokens, database data, uploads, or deployment secrets.

Use a private GitHub security advisory if available for this repository. If that
is not available, contact the maintainer through a private channel listed on the
GitHub profile.

When reporting, include:

- A short description of the issue.
- Affected endpoint, component, or configuration.
- Reproduction steps.
- Expected impact.
- Suggested mitigation if known.

Do not include real production passwords, tokens, database dumps, OSS secrets,
or private user data in the report.

## Security Defaults

- No default public administrator password.
- First admin bootstrap is environment-driven.
- Production Swagger UI and OpenAPI JSON are disabled by default.
- Production exposes only Actuator health by default.
- Production database migrations are manual.
- `.env`, logs, and backups are ignored by Git.
