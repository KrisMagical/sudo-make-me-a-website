-- Migration 002: store lightweight local moderation reasons for admin review.
--
-- Apply manually before deploying code that reads comments.moderation_reason.
-- Back up production data first. Do not wire this migration into application
-- startup.

ALTER TABLE comments
    ADD COLUMN moderation_reason VARCHAR(255) NULL;

-- MySQL versions differ in support for IF NOT EXISTS on ADD COLUMN. If this
-- column already exists, skip the ALTER statement above.
