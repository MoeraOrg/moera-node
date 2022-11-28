CREATE TABLE friend_ofs (
    id uuid NOT NULL PRIMARY KEY,
    node_id uuid NOT NULL,
    remote_node_name varchar(63) NOT NULL,
    remote_full_name varchar(96),
    remote_gender varchar(31),
    remote_avatar_media_file_id varchar(40),
    remote_avatar_shape varchar(8),
    remote_group_id varchar(40) NOT NULL,
    remote_group_title varchar(63),
    remote_added_at timestamp without time zone NOT NULL,
    created_at timestamp without time zone NOT NULL
);
CREATE INDEX friend_ofs_node_id_remote_node_name_idx ON friend_ofs(node_id, remote_node_name);
CREATE INDEX friend_ofs_remote_avatar_media_file_id_idx ON friend_ofs(remote_avatar_media_file_id);
CREATE TRIGGER update_remote_avatar_media_file_id
    AFTER INSERT OR DELETE OR UPDATE OF remote_avatar_media_file_id ON friend_ofs
    FOR EACH ROW EXECUTE FUNCTION update_entity_remote_avatar_media_file_id();
ALTER TABLE friend_ofs ADD FOREIGN KEY (remote_avatar_media_file_id) REFERENCES media_files(id)
    ON UPDATE CASCADE ON DELETE SET NULL;
