ALTER TABLE picks ADD COLUMN media_file_owner_id uuid;
ALTER TABLE picks ADD FOREIGN KEY (media_file_owner_id)
    REFERENCES media_file_owners(id) ON UPDATE CASCADE ON DELETE CASCADE;
CREATE INDEX picks_media_file_owner_id_idx ON picks(media_file_owner_id);
