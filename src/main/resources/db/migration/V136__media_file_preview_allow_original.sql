CREATE OR REPLACE FUNCTION update_media_file_preview_media_file_id() RETURNS trigger AS $$
    DECLARE
        old_media_file_id varchar(40) := NULL;
        new_media_file_id varchar(40) := NULL;
    BEGIN
        IF TG_OP = 'DELETE' OR TG_OP = 'UPDATE' THEN
            IF OLD.media_file_id <> OLD.original_media_file_id THEN
                old_media_file_id := OLD.media_file_id;
            END IF;
        END IF;
        IF TG_OP = 'UPDATE' OR TG_OP = 'INSERT' THEN
            IF NEW.media_file_id <> NEW.original_media_file_id THEN
                new_media_file_id := NEW.media_file_id;
            END IF;
        END IF;
        IF TG_OP = 'DELETE' THEN
            PERFORM update_media_file_reference(old_media_file_id, NULL);
            RETURN OLD;
        ELSIF TG_OP = 'UPDATE' THEN
            PERFORM update_media_file_reference(old_media_file_id, new_media_file_id);
            RETURN NEW;
        ELSIF TG_OP = 'INSERT' THEN
            PERFORM update_media_file_reference(NULL, new_media_file_id);
            RETURN NEW;
        END IF;
        RETURN NULL;
    END;
$$ LANGUAGE plpgsql;
DROP TRIGGER IF EXISTS update_media_file_id ON media_file_previews;
CREATE TRIGGER update_media_file_id AFTER INSERT OR UPDATE OF media_file_id, original_media_file_id OR DELETE
    ON media_file_previews FOR EACH ROW EXECUTE PROCEDURE update_media_file_preview_media_file_id();
