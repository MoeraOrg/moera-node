DROP INDEX media_file_owners_node_id_null_media_file_id_idx;
DROP INDEX media_file_owners_node_id_owner_name_media_file_id_idx;
CREATE INDEX media_file_owners_node_id_owner_name_media_file_id_idx
    ON media_file_owners(node_id, owner_name, media_file_id);
