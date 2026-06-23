ALTER TABLE search_engine_statistics ADD COLUMN node_id uuid;

UPDATE search_engine_statistics
SET node_id = options.node_id
FROM options
WHERE options.name = 'profile.node-name'
    AND options.value = search_engine_statistics.owner_name;

UPDATE search_engine_statistics
SET node_id = domains.node_id
FROM domains
WHERE search_engine_statistics.node_id IS NULL
    AND (SELECT count(*) FROM domains) = 1;

DELETE FROM search_engine_statistics WHERE node_id IS NULL;

ALTER TABLE search_engine_statistics ALTER COLUMN node_id SET NOT NULL;

DROP INDEX search_engine_statistics_owner_clicked_idx;
CREATE INDEX search_engine_statistics_node_id_clicked_at_idx ON search_engine_statistics(node_id, clicked_at);
