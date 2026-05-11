CREATE TABLE media_leases (
    id uuid NOT NULL PRIMARY KEY,
    node_id uuid NOT NULL,
    owner_name varchar(135) NOT NULL,
    media_file_owner_id uuid NOT NULL,
    entry_id uuid,
    created_at timestamp without time zone NOT NULL
);
ALTER TABLE media_leases ADD FOREIGN KEY (media_file_owner_id)
    REFERENCES media_file_owners(id) ON UPDATE CASCADE ON DELETE CASCADE;
ALTER TABLE media_leases ADD FOREIGN KEY (entry_id)
    REFERENCES entries(id) ON UPDATE CASCADE ON DELETE CASCADE;
CREATE INDEX media_leases_media_file_owner_id_idx ON media_leases(media_file_owner_id);

CREATE FUNCTION update_media_lease_media_file_owner_id() RETURNS trigger
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
CREATE TRIGGER update_media_lease_media_file_owner_id
    AFTER INSERT OR UPDATE OF media_file_owner_id OR DELETE ON media_leases
    FOR EACH ROW EXECUTE PROCEDURE update_media_lease_media_file_owner_id();
