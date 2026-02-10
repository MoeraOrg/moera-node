ALTER TABLE entries ADD COLUMN external_source_uri varchar(1024) NOT NULL DEFAULT '';
CREATE INDEX entries_external_source_uri_idx ON entries(external_source_uri);
