CREATE INDEX entries_live_postings_node_id_id_idx
    ON entries (node_id, id)
    WHERE entry_type = 0 AND deleted_at IS NULL AND parent_media_entry_id IS NULL;
