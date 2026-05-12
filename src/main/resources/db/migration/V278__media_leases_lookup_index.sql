CREATE INDEX media_leases_node_id_owner_name_media_file_owner_id_idx
    ON media_leases(node_id, owner_name, media_file_owner_id);
