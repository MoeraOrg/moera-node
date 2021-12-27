ALTER TABLE entries ADD COLUMN parent_media_id uuid;
ALTER TABLE entries ADD FOREIGN KEY (parent_media_id) REFERENCES media_file_owners(id)
    ON UPDATE CASCADE ON DELETE CASCADE;
CREATE UNIQUE INDEX entries_parent_media_id_idx ON entries(parent_media_id);
