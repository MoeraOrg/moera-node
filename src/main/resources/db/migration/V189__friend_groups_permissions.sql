ALTER TABLE friend_groups ADD COLUMN view_principal varchar(70) NOT NULL DEFAULT 'public';
UPDATE friend_groups SET view_principal = 'admin' WHERE visible = false;
ALTER TABLE friend_groups DROP COLUMN visible;
