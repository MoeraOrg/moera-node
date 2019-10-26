ALTER TABLE entries ADD receiver_name VARCHAR(127);
UPDATE entries SET receiver_name = owner_name;
ALTER TABLE entries ALTER COLUMN receiver_name SET NOT NULL;
ALTER TABLE entry_revisions ADD body_html_format VARCHAR(75) DEFAULT 'html';
UPDATE entry_revisions SET signature = '';
