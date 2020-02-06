ALTER TABLE entries ADD COLUMN draft_revision_id uuid;
CREATE INDEX ON entries(draft_revision_id);
ALTER TABLE entries ADD FOREIGN KEY (draft_revision_id) REFERENCES entry_revisions(id) ON UPDATE CASCADE ON DELETE SET NULL;
