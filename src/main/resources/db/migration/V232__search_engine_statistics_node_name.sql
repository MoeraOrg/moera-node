ALTER TABLE search_engine_statistics ADD COLUMN node_name varchar(63) NOT NULL DEFAULT '';
ALTER TABLE search_engine_statistics ADD COLUMN heading varchar(255);
UPDATE search_engine_statistics
    SET node_name = (
        SELECT value FROM options WHERE node_id = search_engine_statistics.node_id AND name = 'profile.node-name'
    );
ALTER TABLE search_engine_statistics DROP COLUMN node_id;
