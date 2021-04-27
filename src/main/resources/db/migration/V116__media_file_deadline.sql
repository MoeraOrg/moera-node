ALTER TABLE media_files ADD COLUMN usage_count integer NOT NULL DEFAULT 0;
ALTER TABLE media_files ADD COLUMN deadline timestamp without time zone;
CREATE OR REPLACE FUNCTION update_media_file_deadline() RETURNS trigger AS $$
    BEGIN
        IF NEW.usage_count < 0 THEN
            NEW.usage_count := 0;
        END IF;
        IF NEW.usage_count = 0 AND NEW.deadline IS NULL THEN
            NEW.deadline := NOW() + interval '1 day';
        ELSIF NEW.usage_count > 0 AND NEW.deadline IS NOT NULL THEN
            NEW.deadline := NULL;
        END IF;
        RETURN NEW;
    END;
$$ LANGUAGE plpgsql;
CREATE TRIGGER update_deadline BEFORE INSERT OR UPDATE OF usage_count, deadline ON media_files
    FOR EACH ROW EXECUTE PROCEDURE update_media_file_deadline();
CREATE OR REPLACE FUNCTION update_media_file_reference(old_id varchar(40), new_id varchar(40)) RETURNS void AS $$
    BEGIN
        IF old_id = new_id THEN
            RETURN;
        END IF;
        IF old_id IS NOT NULL THEN
            UPDATE media_files SET usage_count = usage_count - 1 WHERE id = old_id;
        END IF;
        IF new_id IS NOT NULL THEN
            UPDATE media_files SET usage_count = usage_count + 1 WHERE id = new_id;
        END IF;
    END;
$$ LANGUAGE plpgsql;
CREATE OR REPLACE FUNCTION update_entity_media_file_id() RETURNS trigger AS $$
    BEGIN
        IF TG_OP = 'DELETE' THEN
            PERFORM update_media_file_reference(OLD.media_file_id, NULL);
            RETURN OLD;
        ELSIF TG_OP = 'UPDATE' THEN
            PERFORM update_media_file_reference(OLD.media_file_id, NEW.media_file_id);
            RETURN NEW;
        ELSIF TG_OP = 'INSERT' THEN
            PERFORM update_media_file_reference(NULL, NEW.media_file_id);
            RETURN NEW;
        END IF;
        RETURN NULL;
    END;
$$ LANGUAGE plpgsql;
CREATE TRIGGER update_media_file_id AFTER INSERT OR UPDATE OF media_file_id OR DELETE ON media_file_owners
    FOR EACH ROW EXECUTE PROCEDURE update_entity_media_file_id();
CREATE TRIGGER update_media_file_id AFTER INSERT OR UPDATE OF media_file_id OR DELETE ON avatars
    FOR EACH ROW EXECUTE PROCEDURE update_entity_media_file_id();
