CREATE OR REPLACE FUNCTION update_media_file_deadline() RETURNS trigger AS $$
    BEGIN
        IF NEW.usage_count < 0 THEN
            NEW.usage_count := 0;
        END IF;
        IF NEW.usage_count = 0 AND NEW.deadline IS NULL THEN
            NEW.deadline := NOW() + interval '4 days';
        ELSIF NEW.usage_count > 0 AND NEW.deadline IS NOT NULL THEN
            NEW.deadline := NULL;
        END IF;
        RETURN NEW;
    END;
$$ LANGUAGE plpgsql;

UPDATE media_files SET deadline = deadline + interval '3 days'
WHERE usage_count = 0 AND deadline IS NOT NULL;

UPDATE media_file_owners SET deadline = deadline + interval '3 days'
WHERE usage_count = 0 AND deadline IS NOT NULL;

UPDATE remote_media_files SET deadline = deadline + interval '3 days'
WHERE usage_count = 0 AND deadline IS NOT NULL;
