-- Migration 002: store lightweight local moderation reasons for admin review.
--
-- Apply manually before deploying code that reads comments.moderation_reason.
-- Back up production data first. Do not wire this migration into application
-- startup.

SET @comments_moderation_reason_exists := (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'comments'
      AND COLUMN_NAME = 'moderation_reason'
);

SET @comments_moderation_reason_sql := IF(
    @comments_moderation_reason_exists = 0,
    'ALTER TABLE comments ADD COLUMN moderation_reason VARCHAR(255) NULL',
    'SELECT ''comments.moderation_reason already exists'' AS message'
);

PREPARE stmt FROM @comments_moderation_reason_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
