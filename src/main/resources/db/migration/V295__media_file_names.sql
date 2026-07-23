ALTER TABLE media_files ADD COLUMN file_name varchar(50);
ALTER TABLE media_files ADD COLUMN cloud_file_name varchar(65);

CREATE SEQUENCE media_file_upgrades_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE media_file_upgrades (
    id bigint NOT NULL PRIMARY KEY,
    upgrade_type smallint NOT NULL,
    media_file_id varchar(40) NOT NULL,
    CONSTRAINT media_file_upgrades_media_file_id_fkey
        FOREIGN KEY (media_file_id) REFERENCES media_files(id) ON UPDATE CASCADE ON DELETE CASCADE
);
CREATE INDEX media_file_upgrades_upgrade_type_idx ON media_file_upgrades(upgrade_type);

INSERT INTO media_file_upgrades(id, upgrade_type, media_file_id)
SELECT nextval('media_file_upgrades_seq'), 9, id FROM media_files;

CREATE SEQUENCE media_file_removals_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE media_file_removals (
    id bigint NOT NULL PRIMARY KEY,
    media_file_id varchar(40) NOT NULL,
    file_name varchar(50),
    cloud_file_name varchar(65),
    created_at timestamp without time zone NOT NULL
);
CREATE INDEX media_file_removals_media_file_id_idx ON media_file_removals(media_file_id);
