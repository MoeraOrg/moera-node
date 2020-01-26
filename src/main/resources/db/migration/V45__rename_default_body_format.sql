UPDATE entry_revisions SET body_format = 'message' WHERE body_format = 'html';
ALTER TABLE entry_revisions ALTER COLUMN body_format SET DEFAULT 'message';
