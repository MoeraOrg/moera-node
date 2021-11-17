CREATE TABLE remote_media_cache (
    id uuid NOT NULL PRIMARY KEY,
    remote_node_name varchar(63) NOT NULL,
    remote_media_id varchar(40) NOT NULL,
    digest bytea NOT NULL,
    media_file_id varchar(40),
    deadline timestamp without time zone NOT NULL
);
CREATE UNIQUE INDEX remote_media_cache_remote_node_name_remote_media_id_idx
    ON remote_media_cache(remote_node_name, remote_media_id);
ALTER TABLE remote_media_cache ADD FOREIGN KEY (media_file_id) REFERENCES media_files(id)
    ON UPDATE CASCADE ON DELETE SET NULL;
CREATE INDEX remote_media_cache_media_file_id_idx ON remote_media_cache(media_file_id);
CREATE INDEX remote_media_cache_deadline_idx ON remote_media_cache(deadline);
