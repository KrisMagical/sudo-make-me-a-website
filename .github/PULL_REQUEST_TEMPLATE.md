## Summary

- 

## Type

- [ ] Bug fix
- [ ] Feature
- [ ] Tests
- [ ] Documentation
- [ ] Operations / deployment

## Checks

- [ ] `cd backend && mvn test`
- [ ] `cd backend && ./mvnw test` or `cmd /c mvnw.cmd test`
- [ ] `cd front && npm ci`
- [ ] `cd front && npm audit`
- [ ] `cd front && npm run test:run`
- [ ] `cd front && npm run build`
- [ ] `docker compose --env-file .env.example config --quiet`

## Notes

- [ ] No `.env`, logs, backups, `node_modules`, `dist`, `target`, or IDE files.
- [ ] No real passwords, tokens, database URLs, or OSS secrets.
- [ ] Production database migrations remain manual.
- [ ] Frontend visual style remains minimal and terminal-like.
