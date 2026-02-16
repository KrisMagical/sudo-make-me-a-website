insert ignore into users (id, username, password, role)
values (1, "gosling", "$2a$12$zBfG6tE.mgR28EON4eKQqeLJVwLn.aL5e213vvar8tA4fLcVFcJ1q", "ROOT");
insert ignore into categories (id,name,slug)
values (1,'blog','blog'),(2,'my-shares','my-shares'),(3,'creations','creations');

--INSERT INTO site_configs (site_name, author_name, is_active)
--SELECT 'My Blog', 'KrisMagic', TRUE
--FROM DUAL
--WHERE NOT EXISTS (SELECT 1 FROM site_configs WHERE is_active = TRUE);
--
--INSERT INTO browser_icons (favicon_url, apple_touch_icon_url, is_active)
--SELECT '/favicon.ico', '/apple-touch-icon.png', TRUE
--FROM DUAL
--WHERE NOT EXISTS (SELECT 1 FROM browser_icons WHERE is_active = TRUE);

INSERT IGNORE INTO embedded_images (
    id, owner_type, owner_id, original_filename, content_type, size, data, created_at
) VALUES (
    1,
    'FAVICON',
    -1,
    'favicon.png',
    'image/png',
    0,
    0,
    NOW()
);

INSERT IGNORE INTO embedded_images (
    id, owner_type, owner_id, original_filename, content_type, size, data, created_at
) VALUES (
    2,
    'APPLE_TOUCH_ICON',
    -1,
    'apple-touch-icon.png',
    'image/png',
    0,
    0,
    NOW()
);

INSERT INTO browser_icons (favicon_image_id, apple_touch_icon_image_id, is_active)
SELECT 1, 2, TRUE
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM browser_icons WHERE is_active = TRUE);