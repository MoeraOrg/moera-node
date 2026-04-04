ALTER TABLE remote_media_cache ALTER COLUMN digest DROP NOT NULL;
ALTER TABLE remote_media_cache ADD COLUMN error smallint;
