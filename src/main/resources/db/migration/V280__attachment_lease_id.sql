ALTER TABLE entry_attachments ADD COLUMN media_file_lease_id uuid;
ALTER TABLE entry_attachments ADD FOREIGN KEY (media_file_lease_id)
    REFERENCES media_leases(id) ON UPDATE CASCADE ON DELETE SET NULL;
CREATE INDEX entry_attachments_media_file_lease_id_idx ON media_leases(id);
