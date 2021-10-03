ALTER TABLE entry_attachments ADD COLUMN ordinal integer NOT NULL;
DROP INDEX entry_attachments_entry_revision_id_idx;
CREATE INDEX entry_attachments_entry_revision_id_ordinal_idx ON entry_attachments(entry_revision_id, ordinal);
