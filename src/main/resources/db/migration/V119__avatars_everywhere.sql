CREATE OR REPLACE FUNCTION update_entity_remote_avatar_media_file_id() RETURNS trigger AS $$
    BEGIN
        IF TG_OP = 'DELETE' THEN
            PERFORM update_media_file_reference(OLD.remote_avatar_media_file_id, NULL);
            RETURN OLD;
        ELSIF TG_OP = 'UPDATE' THEN
            PERFORM update_media_file_reference(OLD.remote_avatar_media_file_id, NEW.remote_avatar_media_file_id);
            RETURN NEW;
        ELSIF TG_OP = 'INSERT' THEN
            PERFORM update_media_file_reference(NULL, NEW.remote_avatar_media_file_id);
            RETURN NEW;
        END IF;
        RETURN NULL;
    END;
$$ LANGUAGE plpgsql;
CREATE OR REPLACE FUNCTION update_entity_owner_avatar_media_file_id() RETURNS trigger AS $$
    BEGIN
        IF TG_OP = 'DELETE' THEN
            PERFORM update_media_file_reference(OLD.owner_avatar_media_file_id, NULL);
            RETURN OLD;
        ELSIF TG_OP = 'UPDATE' THEN
            PERFORM update_media_file_reference(OLD.owner_avatar_media_file_id, NEW.owner_avatar_media_file_id);
            RETURN NEW;
        ELSIF TG_OP = 'INSERT' THEN
            PERFORM update_media_file_reference(NULL, NEW.owner_avatar_media_file_id);
            RETURN NEW;
        END IF;
        RETURN NULL;
    END;
$$ LANGUAGE plpgsql;
CREATE OR REPLACE FUNCTION update_entity_remote_owner_avatar_media_file_id() RETURNS trigger AS $$
    BEGIN
        IF TG_OP = 'DELETE' THEN
            PERFORM update_media_file_reference(OLD.remote_owner_avatar_media_file_id, NULL);
            RETURN OLD;
        ELSIF TG_OP = 'UPDATE' THEN
            PERFORM update_media_file_reference(OLD.remote_owner_avatar_media_file_id, NEW.remote_owner_avatar_media_file_id);
            RETURN NEW;
        ELSIF TG_OP = 'INSERT' THEN
            PERFORM update_media_file_reference(NULL, NEW.remote_owner_avatar_media_file_id);
            RETURN NEW;
        END IF;
        RETURN NULL;
    END;
$$ LANGUAGE plpgsql;
CREATE OR REPLACE FUNCTION update_entity_receiver_avatar_media_file_id() RETURNS trigger AS $$
    BEGIN
        IF TG_OP = 'DELETE' THEN
            PERFORM update_media_file_reference(OLD.receiver_avatar_media_file_id, NULL);
            RETURN OLD;
        ELSIF TG_OP = 'UPDATE' THEN
            PERFORM update_media_file_reference(OLD.receiver_avatar_media_file_id, NEW.receiver_avatar_media_file_id);
            RETURN NEW;
        ELSIF TG_OP = 'INSERT' THEN
            PERFORM update_media_file_reference(NULL, NEW.receiver_avatar_media_file_id);
            RETURN NEW;
        END IF;
        RETURN NULL;
    END;
$$ LANGUAGE plpgsql;
CREATE OR REPLACE FUNCTION update_entity_replied_to_avatar_media_file_id() RETURNS trigger AS $$
    BEGIN
        IF TG_OP = 'DELETE' THEN
            PERFORM update_media_file_reference(OLD.replied_to_avatar_media_file_id, NULL);
            RETURN OLD;
        ELSIF TG_OP = 'UPDATE' THEN
            PERFORM update_media_file_reference(OLD.replied_to_avatar_media_file_id, NEW.replied_to_avatar_media_file_id);
            RETURN NEW;
        ELSIF TG_OP = 'INSERT' THEN
            PERFORM update_media_file_reference(NULL, NEW.replied_to_avatar_media_file_id);
            RETURN NEW;
        END IF;
        RETURN NULL;
    END;
$$ LANGUAGE plpgsql;
CREATE OR REPLACE FUNCTION update_entity_remote_replied_to_avatar_media_file_id() RETURNS trigger AS $$
    BEGIN
        IF TG_OP = 'DELETE' THEN
            PERFORM update_media_file_reference(OLD.remote_replied_to_avatar_media_file_id, NULL);
            RETURN OLD;
        ELSIF TG_OP = 'UPDATE' THEN
            PERFORM update_media_file_reference(OLD.remote_replied_to_avatar_media_file_id, NEW.remote_replied_to_avatar_media_file_id);
            RETURN NEW;
        ELSIF TG_OP = 'INSERT' THEN
            PERFORM update_media_file_reference(NULL, NEW.remote_replied_to_avatar_media_file_id);
            RETURN NEW;
        END IF;
        RETURN NULL;
    END;
$$ LANGUAGE plpgsql;
ALTER TABLE contacts ADD COLUMN remote_avatar_media_file_id varchar(40);
ALTER TABLE contacts ADD COLUMN remote_avatar_shape varchar(8);
ALTER TABLE contacts ADD FOREIGN KEY (remote_avatar_media_file_id) REFERENCES media_files(id) ON UPDATE CASCADE ON DELETE SET NULL;
CREATE INDEX ON contacts(remote_avatar_media_file_id);
CREATE TRIGGER update_remote_avatar_media_file_id
    AFTER INSERT OR UPDATE OF remote_avatar_media_file_id OR DELETE ON contacts
    FOR EACH ROW EXECUTE PROCEDURE update_entity_remote_avatar_media_file_id();
ALTER TABLE entries ADD COLUMN owner_avatar_media_file_id varchar(40);
ALTER TABLE entries ADD COLUMN owner_avatar_shape varchar(8);
ALTER TABLE entries ADD FOREIGN KEY (owner_avatar_media_file_id) REFERENCES media_files(id) ON UPDATE CASCADE ON DELETE SET NULL;
CREATE INDEX ON entries(owner_avatar_media_file_id);
CREATE TRIGGER update_owner_avatar_media_file_id
    AFTER INSERT OR UPDATE OF owner_avatar_media_file_id OR DELETE ON entries
    FOR EACH ROW EXECUTE PROCEDURE update_entity_owner_avatar_media_file_id();
ALTER TABLE entries ADD COLUMN receiver_avatar_media_file_id varchar(40);
ALTER TABLE entries ADD COLUMN receiver_avatar_shape varchar(8);
ALTER TABLE entries ADD FOREIGN KEY (receiver_avatar_media_file_id) REFERENCES media_files(id) ON UPDATE CASCADE ON DELETE SET NULL;
CREATE INDEX ON entries(receiver_avatar_media_file_id);
CREATE TRIGGER update_receiver_avatar_media_file_id
    AFTER INSERT OR UPDATE OF receiver_avatar_media_file_id OR DELETE ON entries
    FOR EACH ROW EXECUTE PROCEDURE update_entity_receiver_avatar_media_file_id();
ALTER TABLE entries ADD COLUMN replied_to_avatar_media_file_id varchar(40);
ALTER TABLE entries ADD COLUMN replied_to_avatar_shape varchar(8);
ALTER TABLE entries ADD FOREIGN KEY (replied_to_avatar_media_file_id) REFERENCES media_files(id) ON UPDATE CASCADE ON DELETE SET NULL;
CREATE INDEX ON entries(replied_to_avatar_media_file_id);
CREATE TRIGGER update_replied_to_avatar_media_file_id
    AFTER INSERT OR UPDATE OF replied_to_avatar_media_file_id OR DELETE ON entries
    FOR EACH ROW EXECUTE PROCEDURE update_entity_replied_to_avatar_media_file_id();
ALTER TABLE entry_sources ADD COLUMN remote_avatar_media_file_id varchar(40);
ALTER TABLE entry_sources ADD COLUMN remote_avatar_shape varchar(8);
ALTER TABLE entry_sources ADD FOREIGN KEY (remote_avatar_media_file_id) REFERENCES media_files(id) ON UPDATE CASCADE ON DELETE SET NULL;
CREATE INDEX ON entry_sources(remote_avatar_media_file_id);
CREATE TRIGGER update_remote_avatar_media_file_id
    AFTER INSERT OR UPDATE OF remote_avatar_media_file_id OR DELETE ON entry_sources
    FOR EACH ROW EXECUTE PROCEDURE update_entity_remote_avatar_media_file_id();
ALTER TABLE own_comments ADD COLUMN remote_avatar_media_file_id varchar(40);
ALTER TABLE own_comments ADD COLUMN remote_avatar_shape varchar(8);
ALTER TABLE own_comments ADD FOREIGN KEY (remote_avatar_media_file_id) REFERENCES media_files(id) ON UPDATE CASCADE ON DELETE SET NULL;
CREATE INDEX ON own_comments(remote_avatar_media_file_id);
CREATE TRIGGER update_remote_avatar_media_file_id
    AFTER INSERT OR UPDATE OF remote_avatar_media_file_id OR DELETE ON own_comments
    FOR EACH ROW EXECUTE PROCEDURE update_entity_remote_avatar_media_file_id();
ALTER TABLE own_comments ADD COLUMN remote_replied_to_avatar_media_file_id varchar(40);
ALTER TABLE own_comments ADD COLUMN remote_replied_to_avatar_shape varchar(8);
ALTER TABLE own_comments ADD FOREIGN KEY (remote_replied_to_avatar_media_file_id) REFERENCES media_files(id) ON UPDATE CASCADE ON DELETE SET NULL;
CREATE INDEX ON own_comments(remote_replied_to_avatar_media_file_id);
CREATE TRIGGER update_remote_replied_to_avatar_media_file_id
    AFTER INSERT OR UPDATE OF remote_replied_to_avatar_media_file_id OR DELETE ON own_comments
    FOR EACH ROW EXECUTE PROCEDURE update_entity_remote_replied_to_avatar_media_file_id();
ALTER TABLE own_reactions ADD COLUMN remote_avatar_media_file_id varchar(40);
ALTER TABLE own_reactions ADD COLUMN remote_avatar_shape varchar(8);
ALTER TABLE own_reactions ADD FOREIGN KEY (remote_avatar_media_file_id) REFERENCES media_files(id) ON UPDATE CASCADE ON DELETE SET NULL;
CREATE INDEX ON own_reactions(remote_avatar_media_file_id);
CREATE TRIGGER update_remote_avatar_media_file_id
    AFTER INSERT OR UPDATE OF remote_avatar_media_file_id OR DELETE ON own_reactions
    FOR EACH ROW EXECUTE PROCEDURE update_entity_remote_avatar_media_file_id();
ALTER TABLE reactions ADD COLUMN owner_avatar_media_file_id varchar(40);
ALTER TABLE reactions ADD COLUMN owner_avatar_shape varchar(8);
ALTER TABLE reactions ADD FOREIGN KEY (owner_avatar_media_file_id) REFERENCES media_files(id) ON UPDATE CASCADE ON DELETE SET NULL;
CREATE INDEX ON reactions(owner_avatar_media_file_id);
CREATE TRIGGER update_owner_avatar_media_file_id
    AFTER INSERT OR UPDATE OF owner_avatar_media_file_id OR DELETE ON reactions
    FOR EACH ROW EXECUTE PROCEDURE update_entity_owner_avatar_media_file_id();
ALTER TABLE stories ADD COLUMN remote_avatar_media_file_id varchar(40);
ALTER TABLE stories ADD COLUMN remote_avatar_shape varchar(8);
ALTER TABLE stories ADD FOREIGN KEY (remote_avatar_media_file_id) REFERENCES media_files(id) ON UPDATE CASCADE ON DELETE SET NULL;
CREATE INDEX ON stories(remote_avatar_media_file_id);
CREATE TRIGGER update_remote_avatar_media_file_id
    AFTER INSERT OR UPDATE OF remote_avatar_media_file_id OR DELETE ON stories
    FOR EACH ROW EXECUTE PROCEDURE update_entity_remote_avatar_media_file_id();
ALTER TABLE stories ADD COLUMN remote_owner_avatar_media_file_id varchar(40);
ALTER TABLE stories ADD COLUMN remote_owner_avatar_shape varchar(8);
ALTER TABLE stories ADD FOREIGN KEY (remote_owner_avatar_media_file_id) REFERENCES media_files(id) ON UPDATE CASCADE ON DELETE SET NULL;
CREATE INDEX ON stories(remote_owner_avatar_media_file_id);
CREATE TRIGGER update_remote_owner_avatar_media_file_id
    AFTER INSERT OR UPDATE OF remote_owner_avatar_media_file_id OR DELETE ON stories
    FOR EACH ROW EXECUTE PROCEDURE update_entity_remote_owner_avatar_media_file_id();
ALTER TABLE subscribers ADD COLUMN remote_avatar_media_file_id varchar(40);
ALTER TABLE subscribers ADD COLUMN remote_avatar_shape varchar(8);
ALTER TABLE subscribers ADD FOREIGN KEY (remote_avatar_media_file_id) REFERENCES media_files(id) ON UPDATE CASCADE ON DELETE SET NULL;
CREATE INDEX ON subscribers(remote_avatar_media_file_id);
CREATE TRIGGER update_remote_avatar_media_file_id
    AFTER INSERT OR UPDATE OF remote_avatar_media_file_id OR DELETE ON subscribers
    FOR EACH ROW EXECUTE PROCEDURE update_entity_remote_avatar_media_file_id();
ALTER TABLE subscriptions ADD COLUMN remote_avatar_media_file_id varchar(40);
ALTER TABLE subscriptions ADD COLUMN remote_avatar_shape varchar(8);
ALTER TABLE subscriptions ADD FOREIGN KEY (remote_avatar_media_file_id) REFERENCES media_files(id) ON UPDATE CASCADE ON DELETE SET NULL;
CREATE INDEX ON subscriptions(remote_avatar_media_file_id);
CREATE TRIGGER update_remote_avatar_media_file_id
    AFTER INSERT OR UPDATE OF remote_avatar_media_file_id OR DELETE ON subscriptions
    FOR EACH ROW EXECUTE PROCEDURE update_entity_remote_avatar_media_file_id();
