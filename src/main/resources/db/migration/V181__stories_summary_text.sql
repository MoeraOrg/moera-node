ALTER TABLE stories ALTER COLUMN summary TYPE text;
ALTER TABLE stories ADD COLUMN remote_posting_node_name varchar(63);
ALTER TABLE stories ADD COLUMN remote_posting_full_name varchar(96);
ALTER TABLE stories ADD COLUMN remote_posting_avatar_media_file_id varchar(40);
ALTER TABLE stories ADD COLUMN remote_posting_avatar_shape varchar(8);
ALTER TABLE stories ADD FOREIGN KEY (remote_posting_avatar_media_file_id) REFERENCES media_files(id)
    ON UPDATE CASCADE ON DELETE SET NULL;
CREATE INDEX stories_remote_posting_avatar_media_file_id_idx ON stories(remote_posting_avatar_media_file_id);
CREATE OR REPLACE FUNCTION update_entity_remote_posting_avatar_media_file_id() RETURNS trigger AS $$
    BEGIN
        IF TG_OP = 'DELETE' THEN
            PERFORM update_media_file_reference(OLD.remote_posting_avatar_media_file_id, NULL);
            RETURN OLD;
        ELSIF TG_OP = 'UPDATE' THEN
            PERFORM update_media_file_reference(OLD.remote_posting_avatar_media_file_id,
                                                NEW.remote_posting_avatar_media_file_id);
            RETURN NEW;
        ELSIF TG_OP = 'INSERT' THEN
            PERFORM update_media_file_reference(NULL, NEW.remote_posting_avatar_media_file_id);
            RETURN NEW;
        END IF;
        RETURN NULL;
    END;
$$ LANGUAGE plpgsql;
CREATE TRIGGER update_remote_posting_avatar_media_file_id
    AFTER INSERT OR DELETE OR UPDATE OF remote_posting_avatar_media_file_id ON stories
    FOR EACH ROW EXECUTE FUNCTION update_entity_remote_posting_avatar_media_file_id();
UPDATE stories
SET remote_posting_node_name = remote_node_name,
    remote_posting_full_name = remote_full_name,
    remote_posting_avatar_media_file_id = remote_avatar_media_file_id,
    remote_posting_avatar_shape = remote_avatar_shape;
