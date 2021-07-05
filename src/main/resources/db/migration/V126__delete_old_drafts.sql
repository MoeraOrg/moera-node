DELETE FROM entries WHERE draft = true;
DELETE FROM entry_revisions WHERE id IN (SELECT draft_revision_id FROM entries);
ALTER TABLE entries DROP COLUMN draft;
ALTER TABLE entries DROP COLUMN draft_revision_id;
