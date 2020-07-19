ALTER TABLE entries ADD COLUMN moment bigint;
CREATE INDEX ON entries(parent_id, moment);
ALTER TABLE entry_revisions ADD COLUMN parent_id uuid;
ALTER TABLE entry_revisions ADD COLUMN deadline TIMESTAMP WITHOUT TIME ZONE;
CREATE INDEX ON entry_revisions(parent_id);
ALTER TABLE entry_revisions ADD FOREIGN KEY (parent_id) REFERENCES entry_revisions(id) ON UPDATE CASCADE ON DELETE CASCADE;
CREATE INDEX ON entry_revisions(deadline);
