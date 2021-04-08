CREATE TABLE media_files (
    id varchar(40) NOT NULL PRIMARY KEY,
    mime_type varchar(80) NOT NULL,
    size_x smallint,
    size_y smallint,
    file_size bigint NOT NULL,
    created_at timestamp without time zone NOT NULL
);
CREATE INDEX ON media_files(created_at);

CREATE TABLE media_file_owners (
    id uuid NOT NULL PRIMARY KEY,
    node_id uuid NOT NULL,
    owner_name varchar(63),
    media_file_id varchar(40) NOT NULL,
    created_at timestamp without time zone NOT NULL
);
CREATE INDEX ON media_file_owners(node_id, owner_name);
ALTER TABLE media_file_owners ADD FOREIGN KEY (media_file_id) REFERENCES media_files(id) ON UPDATE CASCADE ON DELETE CASCADE;
