DROP INDEX entries_external_source_uri_idx;

ALTER TABLE entries ALTER COLUMN external_source_uri DROP DEFAULT;
ALTER TABLE entries ALTER COLUMN external_source_uri TYPE varchar(1024)[]
    USING CASE
        WHEN external_source_uri = '' THEN ARRAY[]::varchar(1024)[]
        ELSE ARRAY[external_source_uri]::varchar(1024)[]
    END;
ALTER TABLE entries ALTER COLUMN external_source_uri SET DEFAULT ARRAY[]::varchar(1024)[];

CREATE INDEX entries_external_source_uri_idx ON entries USING gin(external_source_uri);
