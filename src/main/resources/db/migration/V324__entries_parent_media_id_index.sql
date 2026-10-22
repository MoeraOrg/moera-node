CREATE INDEX entries_parent_media_id_idx
    ON entries(parent_media_id)
    WHERE parent_media_id IS NOT NULL;
