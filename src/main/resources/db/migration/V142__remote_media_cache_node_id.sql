ALTER TABLE remote_media_cache ADD node_id uuid;
DROP INDEX remote_media_cache_remote_node_name_remote_media_id_idx;
CREATE UNIQUE INDEX remote_media_cache_node_id_remote_node_name_remote_media_id_idx
    ON remote_media_cache(node_id, remote_node_name, remote_media_id);
