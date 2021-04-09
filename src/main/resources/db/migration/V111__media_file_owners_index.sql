DROP INDEX IF EXISTS media_file_owners_node_id_owner_name_idx;
CREATE UNIQUE INDEX ON media_file_owners(node_id, owner_name, media_file_id);
