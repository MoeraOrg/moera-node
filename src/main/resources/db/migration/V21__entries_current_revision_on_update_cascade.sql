ALTER TABLE entries DROP CONSTRAINT entries_current_revision_id_fkey;
ALTER TABLE entries ADD FOREIGN KEY (current_revision_id) REFERENCES entry_revisions(id) ON UPDATE CASCADE ON DELETE SET NULL;
