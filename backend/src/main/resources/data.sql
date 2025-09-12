insert ignore into users (id, username, password, role)
values (1, "gosling", "$2a$12$zBfG6tE.mgR28EON4eKQqeLJVwLn.aL5e213vvar8tA4fLcVFcJ1q", "ROOT");
insert ignore into categories (id,name,slug)
values (1,'blog','blog'),(2,'my-shares','my-shares'),(3,'creations','creations')