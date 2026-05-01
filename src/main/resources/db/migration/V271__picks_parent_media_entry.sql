ALTER TABLE picks ADD COLUMN parent_media_entry_id uuid;

ALTER TABLE picks
    ADD CONSTRAINT picks_parent_media_entry_id_fkey
    FOREIGN KEY (parent_media_entry_id) REFERENCES entries(id)
    ON UPDATE CASCADE ON DELETE CASCADE;

CREATE INDEX picks_parent_media_entry_id_idx
    ON picks(parent_media_entry_id)
    WHERE parent_media_entry_id IS NOT NULL;
