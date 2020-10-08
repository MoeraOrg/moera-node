ALTER TABLE domains ADD COLUMN created_at timestamp without time zone NOT NULL DEFAULT NOW();
