ALTER TABLE media_files ADD COLUMN stream_info text;
ALTER TABLE media_files ADD COLUMN uncompressed boolean NOT NULL DEFAULT false;
ALTER TABLE media_files ADD COLUMN compressed_file_id varchar(40);
ALTER TABLE media_files ADD COLUMN compression_job_id uuid;
ALTER TABLE media_file_owners ADD COLUMN downsize boolean NOT NULL DEFAULT false;

ALTER TABLE media_files ADD CONSTRAINT media_files_compressed_file_fk
    FOREIGN KEY (compressed_file_id) REFERENCES media_files(id) ON UPDATE CASCADE ON DELETE SET NULL;
ALTER TABLE media_files ADD CONSTRAINT media_files_compression_job_fk
    FOREIGN KEY (compression_job_id) REFERENCES pending_jobs(id) ON UPDATE CASCADE ON DELETE SET NULL;

CREATE INDEX media_files_compressed_file_id_idx ON media_files(compressed_file_id);
CREATE INDEX media_files_compression_job_id_idx ON media_files(compression_job_id);

CREATE OR REPLACE FUNCTION update_media_file_compressed_file_usage() RETURNS trigger AS $$
    BEGIN
        IF TG_OP = 'DELETE' THEN
            PERFORM update_media_file_reference(OLD.compressed_file_id, NULL);
            RETURN OLD;
        ELSIF TG_OP = 'UPDATE' THEN
            PERFORM update_media_file_reference(OLD.compressed_file_id, NEW.compressed_file_id);
            RETURN NEW;
        ELSIF TG_OP = 'INSERT' THEN
            PERFORM update_media_file_reference(NULL, NEW.compressed_file_id);
            RETURN NEW;
        END IF;
        RETURN NULL;
    END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER update_compressed_file_id
    AFTER INSERT OR UPDATE OF compressed_file_id OR DELETE ON media_files
    FOR EACH ROW EXECUTE PROCEDURE update_media_file_compressed_file_usage();

DROP INDEX media_files_cloud_upload_candidate_idx;
CREATE INDEX media_files_cloud_upload_candidate_idx ON media_files(created_at, id)
    WHERE cloud_file_name IS NULL
        AND cloud_upload_deadline IS NULL
        AND compression_job_id IS NULL
        AND file_name IS NOT NULL
        AND usage_count > 0;
