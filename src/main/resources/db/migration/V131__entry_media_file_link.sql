ALTER TABLE media_file_owners ADD COLUMN usage_count integer NOT NULL DEFAULT 0;
ALTER TABLE media_file_owners ADD COLUMN deadline timestamp without time zone;
CREATE INDEX media_file_owners_deadline_idx ON media_file_owners(deadline);
CREATE TRIGGER update_deadline BEFORE INSERT OR UPDATE OF usage_count, deadline ON media_file_owners
    FOR EACH ROW EXECUTE PROCEDURE update_media_file_deadline();
CREATE TABLE entry_attachments (
    id uuid NOT NULL PRIMARY KEY,
    entry_id uuid NOT NULL,
    media_file_owner_id uuid NOT NULL
);
ALTER TABLE entry_attachments ADD FOREIGN KEY (entry_id) REFERENCES entries(id) ON UPDATE CASCADE ON DELETE CASCADE;
ALTER TABLE entry_attachments ADD FOREIGN KEY (media_file_owner_id) REFERENCES media_file_owners(id)
    ON UPDATE CASCADE ON DELETE CASCADE;
CREATE INDEX entry_attachments_entry_id_idx ON entry_attachments(entry_id);
CREATE INDEX entry_attachments_media_file_owner_id_idx ON entry_attachments(media_file_owner_id);
CREATE FUNCTION update_media_file_owner_reference(old_id uuid, new_id uuid) RETURNS void
    LANGUAGE plpgsql
    AS $$
    BEGIN
        IF old_id = new_id THEN
            RETURN;
        END IF;
        IF old_id IS NOT NULL THEN
            UPDATE media_file_owners SET usage_count = usage_count - 1 WHERE id = old_id;
        END IF;
        IF new_id IS NOT NULL THEN
            UPDATE media_file_owners SET usage_count = usage_count + 1 WHERE id = new_id;
        END IF;
    END;
$$;
CREATE FUNCTION update_entry_attachments_media_file_owner_id() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
    BEGIN
        IF TG_OP = 'DELETE' THEN
            PERFORM update_media_file_owner_reference(OLD.media_file_owner_id, NULL);
            RETURN OLD;
        ELSIF TG_OP = 'UPDATE' THEN
            PERFORM update_media_file_owner_reference(OLD.media_file_owner_id, NEW.media_file_owner_id);
            RETURN NEW;
        ELSIF TG_OP = 'INSERT' THEN
            PERFORM update_media_file_owner_reference(NULL, NEW.media_file_owner_id);
            RETURN NEW;
        END IF;
        RETURN NULL;
    END;
$$;
CREATE TRIGGER update_media_file_owner_id
    AFTER INSERT OR UPDATE OF media_file_owner_id OR DELETE ON entry_attachments
    FOR EACH ROW EXECUTE PROCEDURE update_entry_attachments_media_file_owner_id();
