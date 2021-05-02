ALTER TABLE avatars ADD COLUMN ordinal integer NOT NULL DEFAULT 0;
CREATE INDEX ON avatars(node_id, ordinal, created_at);
