-- Bootstrap schema for a new production MySQL 8 database.
--
-- Use this only for a fresh database. Existing production databases should use
-- the numbered migration files in this directory instead.
--
-- This file intentionally creates schema only. It does not insert a default
-- administrator, weak password, or sample production data.
--
-- Review before execution:
--   mysql --default-character-set=utf8mb4 -u <user> -p <database> < docs/migrations/bootstrap-schema.sql

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

CREATE TABLE IF NOT EXISTS users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(255) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS jwt_secret (
    id BIGINT NOT NULL,
    secret_base64 VARCHAR(512) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS categories (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL,
    slug VARCHAR(100) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_categories_name (name),
    UNIQUE KEY uk_categories_slug (slug)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS posts (
    id BIGINT NOT NULL AUTO_INCREMENT,
    title VARCHAR(200) NOT NULL,
    content MEDIUMTEXT NOT NULL,
    slug VARCHAR(200) NOT NULL,
    category_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NULL,
    published BIT NOT NULL DEFAULT b'1',
    like_count INT NOT NULL DEFAULT 0,
    dislike_count INT NOT NULL DEFAULT 0,
    view_count INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_posts_slug (slug),
    KEY idx_posts_category_id (category_id),
    CONSTRAINT fk_posts_category FOREIGN KEY (category_id) REFERENCES categories (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS comments (
    id BIGINT NOT NULL AUTO_INCREMENT,
    post_id BIGINT NOT NULL,
    name VARCHAR(50) NOT NULL,
    email VARCHAR(100) NOT NULL,
    content TEXT NOT NULL,
    created_at DATETIME(6) NULL,
    parent_id BIGINT NULL,
    author BIT NOT NULL DEFAULT b'0',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    moderation_reason VARCHAR(255) NULL,
    PRIMARY KEY (id),
    KEY idx_comments_post_id (post_id),
    KEY idx_comments_parent_id (parent_id),
    KEY idx_comments_status (status),
    CONSTRAINT fk_comments_post FOREIGN KEY (post_id) REFERENCES posts (id),
    CONSTRAINT fk_comments_parent FOREIGN KEY (parent_id) REFERENCES comments (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS like_logs (
    id BIGINT NOT NULL AUTO_INCREMENT,
    post_id BIGINT NOT NULL,
    identifier VARCHAR(255) NOT NULL,
    positive BIT NOT NULL,
    created_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_like_logs_post_identifier (post_id, identifier),
    KEY idx_like_logs_post_id (post_id),
    CONSTRAINT fk_like_logs_post FOREIGN KEY (post_id) REFERENCES posts (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS post_groups (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(200) NOT NULL,
    description MEDIUMTEXT NULL,
    cover_image_id BIGINT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_post_groups_slug (slug)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS post_group_items (
    id BIGINT NOT NULL AUTO_INCREMENT,
    post_group_id BIGINT NOT NULL,
    post_id BIGINT NOT NULL,
    order_index INT NOT NULL DEFAULT 0,
    added_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_post_group_items_group_post (post_group_id, post_id),
    KEY idx_post_group_items_group (post_group_id),
    KEY idx_post_group_items_post (post_id),
    CONSTRAINT fk_post_group_items_group FOREIGN KEY (post_group_id) REFERENCES post_groups (id),
    CONSTRAINT fk_post_group_items_post FOREIGN KEY (post_id) REFERENCES posts (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS embedded_images (
    id BIGINT NOT NULL AUTO_INCREMENT,
    owner_type VARCHAR(20) NOT NULL,
    owner_id BIGINT NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    size BIGINT NOT NULL,
    object_key VARCHAR(512) NOT NULL,
    url VARCHAR(512) NOT NULL,
    created_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    KEY idx_embedded_images_owner (owner_type, owner_id),
    KEY idx_embedded_images_owner_created (owner_type, owner_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS embedded_videos (
    id BIGINT NOT NULL AUTO_INCREMENT,
    owner_type VARCHAR(20) NOT NULL,
    owner_id BIGINT NOT NULL,
    provider VARCHAR(50) NOT NULL,
    source_url VARCHAR(3000) NOT NULL,
    embed_url VARCHAR(3000) NOT NULL,
    title VARCHAR(2000) NULL,
    order_index INT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    KEY idx_embedded_videos_owner (owner_type, owner_id),
    KEY idx_embedded_videos_owner_order (owner_type, owner_id, order_index)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS home_profiles (
    id BIGINT NOT NULL AUTO_INCREMENT,
    title VARCHAR(200) NOT NULL,
    content MEDIUMTEXT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS socials (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    url VARCHAR(300) NOT NULL,
    description VARCHAR(500) NULL,
    icon_image_id BIGINT NULL,
    external_icon_url VARCHAR(1000) NULL,
    icon_url VARCHAR(1000) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_socials_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS site_configs (
    id BIGINT NOT NULL AUTO_INCREMENT,
    site_name VARCHAR(100) NOT NULL,
    author_name VARCHAR(100) NULL,
    site_avatar_image_id BIGINT NULL,
    footer_text TEXT NULL,
    meta_description TEXT NULL,
    meta_keywords TEXT NULL,
    copyright_text TEXT NULL,
    is_active BIT NOT NULL DEFAULT b'1',
    PRIMARY KEY (id),
    KEY idx_site_configs_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS browser_icons (
    id BIGINT NOT NULL AUTO_INCREMENT,
    favicon_image_id BIGINT NULL,
    apple_touch_icon_image_id BIGINT NULL,
    is_active BIT NOT NULL DEFAULT b'1',
    PRIMARY KEY (id),
    KEY idx_browser_icons_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS maintenance_config (
    id BIGINT NOT NULL AUTO_INCREMENT,
    enabled BIT NULL DEFAULT b'0',
    mode VARCHAR(255) NULL DEFAULT 'maintenance',
    updated_at DATETIME(6) NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SET FOREIGN_KEY_CHECKS = 1;
