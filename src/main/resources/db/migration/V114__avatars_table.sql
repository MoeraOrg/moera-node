CREATE TABLE avatars (
    id uuid NOT NULL PRIMARY KEY,
    node_id uuid NOT NULL,
    media_file_id varchar(40) NOT NULL,
    current boolean NOT NULL,
    shape varchar(8) NOT NULL,
    created_at timestamp without time zone NOT NULL
);
ALTER TABLE avatars ADD FOREIGN KEY (media_file_id) REFERENCES media_files(id) ON UPDATE CASCADE ON DELETE CASCADE;
CREATE INDEX ON avatars(media_file_id);
CREATE INDEX ON media_file_owners(media_file_id);
