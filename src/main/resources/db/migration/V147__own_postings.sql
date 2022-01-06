CREATE TABLE own_postings (
    id uuid NOT NULL PRIMARY KEY,
    node_id uuid NOT NULL,
    remote_node_name varchar(63) NOT NULL,
    remote_full_name varchar(96),
    remote_posting_id varchar(40) NOT NULL,
    heading varchar(255) NOT NULL,
    created_at timestamp without time zone NOT NULL,
    remote_avatar_media_file_id varchar(40),
    remote_avatar_shape varchar(8)
);
CREATE UNIQUE INDEX own_postings_node_id_remote_node_name_remote_posting_id_idx
    ON own_postings(node_id, remote_node_name, remote_posting_id);
CREATE INDEX own_postings_remote_avatar_media_file_id_idx ON own_postings(remote_avatar_media_file_id);
CREATE TRIGGER update_remote_avatar_media_file_id
    AFTER INSERT OR DELETE OR UPDATE OF remote_avatar_media_file_id ON own_postings
    FOR EACH ROW EXECUTE FUNCTION update_entity_remote_avatar_media_file_id();
ALTER TABLE own_postings ADD FOREIGN KEY (remote_avatar_media_file_id) REFERENCES media_files(id)
    ON UPDATE CASCADE ON DELETE SET NULL;
