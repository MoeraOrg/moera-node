ALTER TABLE entries ADD published_at timestamp without time zone NOT NULL DEFAULT NOW();
UPDATE entries SET published_at = created_at;
