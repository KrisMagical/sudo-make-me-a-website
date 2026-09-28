# Backup And Restore

Backups are required before every production migration and release.

## Backup

Create a local backup directory outside source control:

```bash
mkdir -p backups
```

Example Docker Compose backup:

```bash
docker compose exec -T mysql sh -c 'mysqldump -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" "$MYSQL_DATABASE"' > backups/blog_backup_YYYYMMDD_HHMMSS.sql
```

The command reads database credentials from the container environment instead of
hard-coding them in the command line.

Recommended naming:

```text
blog_backup_YYYYMMDD_HHMMSS.sql
```

## Restore

Stop application writes first:

```bash
docker compose stop backend frontend
```

Restore a dump:

```bash
docker compose exec -T mysql sh -c 'mysql -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" "$MYSQL_DATABASE"' < backups/blog_backup_YYYYMMDD_HHMMSS.sql
```

Start services again:

```bash
docker compose up -d
```

Then verify:

- `/actuator/health` returns `UP`
- Backend logs have no schema validation errors
- Admin login works
- Public posts, comments, reactions, media, search, and maintenance mode work

## Before Migrations

Always back up before running files from `docs/migrations`.

For a fresh database, review and run `bootstrap-schema.sql`. For an existing
database, run only the numbered migration files that have not already been
applied.

## Rollback Notes

Code rollback is not database rollback. Database migrations may be partially or
fully irreversible.

Before rolling back:

- Confirm whether migrations were executed.
- Confirm whether the old code can run against the new schema.
- Prepare the matching database backup if schema restore is required.

Do not drop new columns or constraints casually. Moderation and reaction data can
be lost.

## Security

Backup files may contain user content, comments, administrator password hashes,
JWT secret material, and media metadata.

- Do not commit backups.
- Do not upload backups to public storage.
- Encrypt backups when moving them off-host.
- Restrict filesystem permissions.

## Restore Drills

Periodically restore a backup in a non-production environment. A backup that has
never been restored is only a hope with a filename.
