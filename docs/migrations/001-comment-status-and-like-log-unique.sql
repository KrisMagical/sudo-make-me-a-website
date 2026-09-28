-- Migration 001: comment moderation status and like log deduplication
--
-- Target database: MySQL / MariaDB-compatible deployments.
-- Execute manually after backing up production data and before starting the
-- application with the prod profile.
--
-- This migration is intentionally not wired into application startup.

-- 1) Add comments.status if it does not exist.
SET @comments_status_exists := (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'comments'
      AND COLUMN_NAME = 'status'
);

SET @comments_status_sql := IF(
    @comments_status_exists = 0,
    'ALTER TABLE comments ADD COLUMN status VARCHAR(20) NULL',
    'SELECT ''comments.status already exists'' AS message'
);

PREPARE stmt FROM @comments_status_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Legacy comments existed before moderation, so keep them visible after the
-- upgrade. New comments are created as PENDING by the application.
UPDATE comments
SET status = 'APPROVED'
WHERE status IS NULL OR status = '';

ALTER TABLE comments
    MODIFY COLUMN status VARCHAR(20) NOT NULL DEFAULT 'PENDING';

-- 2) Check for duplicate like log rows before adding the unique constraint.
-- If this query returns rows, resolve duplicates before continuing.
SELECT post_id, identifier, COUNT(*) AS duplicate_count
FROM like_logs
GROUP BY post_id, identifier
HAVING COUNT(*) > 1;

-- Optional duplicate cleanup example. Review data first before running it.
-- This keeps the newest row per post_id + identifier and removes older rows.
--
-- DELETE old_log
-- FROM like_logs old_log
-- JOIN like_logs new_log
--   ON old_log.post_id = new_log.post_id
--  AND old_log.identifier = new_log.identifier
--  AND old_log.id < new_log.id;

-- 3) Add a database-level guard for the service rule:
-- one reaction row per post and identifier. Switching like/dislike updates the
-- existing row instead of inserting another one.
SET @like_log_unique_exists := (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'like_logs'
      AND INDEX_NAME = 'uk_like_logs_post_identifier'
);

SET @like_log_unique_sql := IF(
    @like_log_unique_exists = 0,
    'ALTER TABLE like_logs ADD CONSTRAINT uk_like_logs_post_identifier UNIQUE (post_id, identifier)',
    'SELECT ''uk_like_logs_post_identifier already exists'' AS message'
);

PREPARE stmt FROM @like_log_unique_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
