CREATE TABLE media_file_previews (
    id uuid NOT NULL PRIMARY KEY,
    original_media_file_id varchar(40) NOT NULL,
    size integer NOT NULL,
    media_file_id varchar(40)
);
ALTER TABLE media_file_previews ADD FOREIGN KEY (original_media_file_id) REFERENCES media_files(id)
    ON UPDATE CASCADE ON DELETE CASCADE;
ALTER TABLE media_file_previews ADD FOREIGN KEY (media_file_id) REFERENCES media_files(id)
    ON UPDATE CASCADE ON DELETE SET NULL;
CREATE INDEX media_file_previews_original_media_file_id_idx ON media_file_previews(original_media_file_id);
CREATE INDEX media_file_previews_media_file_id_idx ON media_file_previews(media_file_id);
CREATE TRIGGER update_media_file_id AFTER INSERT OR UPDATE OF media_file_id OR DELETE ON media_file_previews
    FOR EACH ROW EXECUTE PROCEDURE update_entity_media_file_id();
