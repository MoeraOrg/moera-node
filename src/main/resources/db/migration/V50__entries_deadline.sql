ALTER TABLE entries ADD COLUMN deadline TIMESTAMP WITHOUT TIME ZONE;
CREATE INDEX ON entries(deadline);