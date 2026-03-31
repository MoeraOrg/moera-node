ALTER TABLE entry_attachments ADD COLUMN remote_media_mime_type varchar(80) NOT NULL DEFAULT 'image/jpeg';
ALTER TABLE entry_attachments ADD COLUMN remote_media_attachment boolean NOT NULL DEFAULT false;
