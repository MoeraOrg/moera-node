ALTER TABLE friend_groups ADD COLUMN created_at timestamp without time zone NOT NULL DEFAULT now();
ALTER TABLE friends ADD COLUMN created_at timestamp without time zone NOT NULL DEFAULT now();
