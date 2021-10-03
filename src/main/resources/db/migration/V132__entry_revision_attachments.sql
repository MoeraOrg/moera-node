ALTER TABLE entry_attachments RENAME COLUMN entry_id TO entry_revision_id;
DROP INDEX entry_attachments_entry_id_idx;
ALTER TABLE entry_attachments DROP CONSTRAINT entry_attachments_entry_id_fkey;
ALTER TABLE entry_attachments ADD FOREIGN KEY (entry_revision_id) REFERENCES entry_revisions(id)
    ON UPDATE CASCADE ON DELETE CASCADE;
CREATE INDEX entry_attachments_entry_revision_id_idx ON entry_attachments(entry_revision_id);
