ALTER TABLE tokens ADD COLUMN auth_category bigint not null default 0;
ALTER TABLE tokens DROP COLUMN admin;
