ALTER TABLE entries ADD COLUMN parent_remote_media_id uuid;

ALTER TABLE entries
    ADD CONSTRAINT entries_parent_remote_media_id_fkey
    FOREIGN KEY (parent_remote_media_id) REFERENCES remote_media_files(id)
    ON UPDATE CASCADE ON DELETE CASCADE;

CREATE INDEX entries_parent_remote_media_id_idx
    ON entries(parent_remote_media_id)
    WHERE parent_remote_media_id IS NOT NULL;

ALTER TABLE own_postings ADD COLUMN remote_parent_media_entry_id varchar(40);

ALTER TABLE picks ADD COLUMN remote_media_file_id uuid;

ALTER TABLE picks
    ADD CONSTRAINT picks_remote_media_file_id_fkey
        FOREIGN KEY (remote_media_file_id) REFERENCES remote_media_files(id)
            ON UPDATE CASCADE ON DELETE CASCADE;

CREATE INDEX picks_remote_media_file_id_idx
    ON picks(remote_media_file_id)
    WHERE remote_media_file_id IS NOT NULL;
