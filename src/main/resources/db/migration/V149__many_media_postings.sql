DROP INDEX entries_parent_media_id_idx;
CREATE UNIQUE INDEX entries_parent_media_id_null_idx
    ON entries(parent_media_id) WHERE receiver_name IS NULL;
CREATE UNIQUE INDEX entries_parent_media_id_not_null_idx
    ON entries(parent_media_id, receiver_name) WHERE receiver_name IS NOT NULL;
CREATE INDEX entries_parent_media_id_receiver_name_idx
    ON entries(parent_media_id, receiver_name);
