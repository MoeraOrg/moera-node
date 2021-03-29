ALTER TABLE entry_revisions ADD COLUMN update_important boolean NOT NULL DEFAULT false;
ALTER TABLE entry_revisions ADD COLUMN update_description varchar(128) NOT NULL DEFAULT '';
