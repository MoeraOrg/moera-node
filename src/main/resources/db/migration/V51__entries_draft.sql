ALTER TABLE entry_revisions ALTER COLUMN moment DROP NOT NULL;
ALTER TABLE entries ADD COLUMN draft boolean NOT NULL DEFAULT false;
CREATE INDEX ON entries(node_id, draft);
