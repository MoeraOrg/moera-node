ALTER TABLE entry_attachments ALTER COLUMN entry_revision_id DROP NOT NULL;
ALTER TABLE entry_attachments ADD COLUMN draft_id uuid;
ALTER TABLE entry_attachments ADD FOREIGN KEY (draft_id) REFERENCES drafts(id)
    ON UPDATE CASCADE ON DELETE CASCADE;
CREATE INDEX entry_attachments_draft_id_ordinal_idx ON entry_attachments(draft_id, ordinal);
