DELETE FROM remote_media_cache;
ALTER TABLE remote_media_cache DROP CONSTRAINT remote_media_cache_media_file_id_fkey;
ALTER TABLE remote_media_cache ADD FOREIGN KEY (media_file_id) REFERENCES media_files(id) ON UPDATE CASCADE ON DELETE CASCADE;
CREATE TRIGGER update_media_file_id AFTER INSERT OR UPDATE OF media_file_id OR DELETE ON remote_media_cache
    FOR EACH ROW EXECUTE PROCEDURE update_entity_media_file_id();
