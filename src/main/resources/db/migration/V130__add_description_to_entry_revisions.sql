ALTER TABLE entry_revisions ADD description character varying(255) NOT NULL DEFAULT '';
UPDATE entry_revisions SET description = heading;
