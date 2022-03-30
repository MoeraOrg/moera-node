CREATE UNIQUE INDEX media_file_owners_node_id_null_media_file_id_idx ON media_file_owners(node_id, media_file_id)
    WHERE owner_name IS NULL;
