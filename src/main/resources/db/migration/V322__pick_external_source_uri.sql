ALTER TABLE picks ADD COLUMN IF NOT EXISTS external_source_uri varchar(1024)[] NOT NULL DEFAULT ARRAY[]::varchar(1024)[];
