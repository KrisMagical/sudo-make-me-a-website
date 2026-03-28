insert ignore into users (id, username, password, role)
values (1, "gosling", "$2a$12$zBfG6tE.mgR28EON4eKQqeLJVwLn.aL5e213vvar8tA4fLcVFcJ1q", "ROOT");
insert ignore into categories (id,name,slug)
values (1,'blog','blog'),(2,'my-shares','my-shares'),(3,'creations','creations');

INSERT IGNORE INTO embedded_images (
    id, owner_type, owner_id, original_filename, content_type, size, object_key, url, created_at
) VALUES (
    1,
    'FAVICON',
    -1,
    'favicon.png',
    'image/png',
    0,
    'favicon-default-key',
    '/api/images/FAVICON/-1/1',
    NOW()
);

INSERT IGNORE INTO embedded_images (
    id, owner_type, owner_id, original_filename, content_type, size, object_key, url, created_at
) VALUES (
    2,
    'APPLE_TOUCH_ICON',
    -1,
    'apple-touch-icon.png',
    'image/png',
    0,
    'apple-touch-icon-default-key',
    '/api/images/APPLE_TOUCH_ICON/-1/2',
    NOW()
);

INSERT INTO browser_icons (favicon_image_id, apple_touch_icon_image_id, is_active)
SELECT 1, 2, TRUE
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM browser_icons WHERE is_active = TRUE);