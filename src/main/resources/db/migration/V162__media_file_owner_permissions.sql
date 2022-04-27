ALTER TABLE media_file_owners ADD COLUMN view_principal varchar(70) NOT NULL DEFAULT 'public';
ALTER TABLE media_file_owners ADD COLUMN usage_updated_at timestamp without time zone NOT NULL DEFAULT now();
ALTER TABLE media_file_owners ADD COLUMN permissions_updated_at timestamp without time zone NOT NULL DEFAULT now();
CREATE OR REPLACE FUNCTION update_media_file_owner_reference(old_id uuid, new_id uuid) RETURNS void
    LANGUAGE plpgsql
    AS $$
    BEGIN
        IF old_id = new_id THEN
            RETURN;
        END IF;
        IF old_id IS NOT NULL THEN
            UPDATE media_file_owners SET usage_count = usage_count - 1, usage_updated_at = now() WHERE id = old_id;
        END IF;
        IF new_id IS NOT NULL THEN
            UPDATE media_file_owners SET usage_count = usage_count + 1, usage_updated_at = now() WHERE id = new_id;
        END IF;
    END;
$$;
CREATE FUNCTION update_entry_media_file_owner_usage() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
    BEGIN
        UPDATE media_file_owners
        SET usage_updated_at = now()
        WHERE id IN (
            SELECT media_file_owner_id
            FROM entry_attachments
            WHERE entry_attachments.entry_revision_id = OLD.current_revision_id
             OR entry_attachments.entry_revision_id = NEW.current_revision_id
        );
        RETURN NEW;
    END;
$$;
CREATE TRIGGER update_media_file_owner_usage
    AFTER UPDATE OF current_revision_id, deleted_at, view_principal ON entries
    FOR EACH ROW EXECUTE PROCEDURE update_entry_media_file_owner_usage();
