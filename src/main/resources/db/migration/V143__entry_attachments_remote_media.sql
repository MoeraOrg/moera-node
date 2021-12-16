ALTER TABLE entry_attachments ADD COLUMN remote_media_id varchar(40);
ALTER TABLE entry_attachments ADD COLUMN remote_media_hash varchar(40);
ALTER TABLE entry_attachments ADD COLUMN remote_media_digest bytea;
ALTER TABLE entry_attachments ALTER COLUMN media_file_owner_id DROP NOT NULL;
