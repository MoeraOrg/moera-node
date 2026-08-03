ALTER TABLE media_file_owners ADD COLUMN compressed_owner_id uuid;

ALTER TABLE media_file_owners ADD CONSTRAINT media_file_owners_compressed_owner_fk
    FOREIGN KEY (compressed_owner_id) REFERENCES media_file_owners(id) ON UPDATE CASCADE ON DELETE SET NULL;

CREATE INDEX media_file_owners_compressed_owner_id_idx ON media_file_owners(compressed_owner_id);

CREATE FUNCTION update_media_file_owner_compressed_owner_usage() RETURNS trigger AS $$
    BEGIN
        IF TG_OP = 'DELETE' THEN
            PERFORM update_media_file_owner_reference(OLD.compressed_owner_id, NULL);
            RETURN OLD;
        ELSIF TG_OP = 'UPDATE' THEN
            PERFORM update_media_file_owner_reference(OLD.compressed_owner_id, NEW.compressed_owner_id);
            RETURN NEW;
        ELSIF TG_OP = 'INSERT' THEN
            PERFORM update_media_file_owner_reference(NULL, NEW.compressed_owner_id);
            RETURN NEW;
        END IF;
        RETURN NULL;
    END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER update_compressed_owner_id
    AFTER INSERT OR UPDATE OF compressed_owner_id OR DELETE ON media_file_owners
    FOR EACH ROW EXECUTE PROCEDURE update_media_file_owner_compressed_owner_usage();
