ALTER TABLE stories ADD COLUMN external_source_uri varchar(1024)[] NOT NULL DEFAULT ARRAY[]::varchar(1024)[];
ALTER TABLE picks ADD COLUMN external_source_uri varchar(1024)[] NOT NULL DEFAULT ARRAY[]::varchar(1024)[];
