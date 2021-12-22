DELETE FROM remote_media_cache
    WHERE ctid NOT IN (
        SELECT MIN(rmc.ctid)
        FROM remote_media_cache rmc
        WHERE rmc.node_id IS NULL
        GROUP BY rmc.remote_node_name, rmc.remote_media_id
    );
CREATE UNIQUE INDEX remote_media_cache_null_remote_node_name_remote_media_id_idx
    ON remote_media_cache(remote_node_name, remote_media_id) WHERE node_id IS NULL;
