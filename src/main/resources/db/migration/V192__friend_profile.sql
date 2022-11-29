ALTER TABLE friends ADD COLUMN node_id uuid NOT NULL;
ALTER TABLE friends RENAME COLUMN node_name TO remote_node_name;
ALTER TABLE friends ADD COLUMN remote_full_name varchar(96);
ALTER TABLE friends ADD COLUMN remote_gender varchar(31);
ALTER TABLE friends ADD COLUMN remote_avatar_media_file_id varchar(40);
ALTER TABLE friends ADD COLUMN remote_avatar_shape varchar(8);

CREATE INDEX friends_remote_avatar_media_file_id_idx ON friends(remote_avatar_media_file_id);
CREATE TRIGGER update_remote_avatar_media_file_id
    AFTER INSERT OR DELETE OR UPDATE OF remote_avatar_media_file_id ON friends
    FOR EACH ROW EXECUTE FUNCTION update_entity_remote_avatar_media_file_id();
ALTER TABLE friends ADD FOREIGN KEY (remote_avatar_media_file_id) REFERENCES media_files(id)
    ON UPDATE CASCADE ON DELETE SET NULL;

CREATE OR REPLACE FUNCTION update_friend_remote_node() RETURNS trigger AS $$
    BEGIN
        IF TG_OP = 'DELETE' THEN
            PERFORM update_subscription_reference(
                OLD.node_id, 3, OLD.remote_node_name, NULL, NULL, NULL, NULL, NULL
            );
            RETURN OLD;
        ELSIF TG_OP = 'UPDATE' THEN
            PERFORM update_subscription_reference(
                NEW.node_id, 3, OLD.remote_node_name, NULL, NULL, NEW.remote_node_name, NULL, NULL
            );
            RETURN NEW;
        ELSIF TG_OP = 'INSERT' THEN
            PERFORM update_subscription_reference(
                NEW.node_id, 3, NULL, NULL, NULL, NEW.remote_node_name, NULL, NULL
            );
            RETURN NEW;
        END IF;
        RETURN NULL;
    END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER update_remote_node AFTER INSERT OR DELETE OR UPDATE OF node_id, remote_node_name
ON friends FOR EACH ROW
EXECUTE FUNCTION update_friend_remote_node();

CREATE OR REPLACE FUNCTION update_friend_of_remote_node() RETURNS trigger AS $$
    BEGIN
        IF TG_OP = 'DELETE' THEN
            PERFORM update_subscription_reference(
                OLD.node_id, 3, OLD.remote_node_name, NULL, NULL, NULL, NULL, NULL
            );
            RETURN OLD;
        ELSIF TG_OP = 'UPDATE' THEN
            PERFORM update_subscription_reference(
                NEW.node_id, 3, OLD.remote_node_name, NULL, NULL, NEW.remote_node_name, NULL, NULL
            );
            RETURN NEW;
        ELSIF TG_OP = 'INSERT' THEN
            PERFORM update_subscription_reference(
                NEW.node_id, 3, NULL, NULL, NULL, NEW.remote_node_name, NULL, NULL
            );
            RETURN NEW;
        END IF;
        RETURN NULL;
    END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER update_remote_node AFTER INSERT OR DELETE OR UPDATE OF node_id, remote_node_name
ON friend_ofs FOR EACH ROW
EXECUTE FUNCTION update_friend_of_remote_node();
