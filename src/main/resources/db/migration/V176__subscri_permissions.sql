ALTER TABLE subscribers ADD COLUMN admin_view_principal varchar(70) NOT NULL DEFAULT 'unset';
ALTER TABLE subscribers ADD COLUMN view_principal varchar(70) NOT NULL DEFAULT 'public';
ALTER TABLE subscriptions ADD COLUMN view_principal varchar(70) NOT NULL DEFAULT 'public';
