ALTER TABLE entries ADD COLUMN edited_at timestamp without time zone NOT NULL DEFAULT NOW();
UPDATE entries
SET edited_at = (SELECT created_at FROM entry_revisions WHERE id = entries.current_revision_id)
WHERE current_revision_id IS NOT NULL;
