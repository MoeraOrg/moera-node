CREATE TABLE domain_upgrades (
    id bigint NOT NULL PRIMARY KEY,
    upgrade_type smallint NOT NULL,
    node_id uuid NOT NULL
);
CREATE INDEX ON domain_upgrades(upgrade_type);
INSERT INTO domain_upgrades
SELECT nextval('hibernate_sequence') AS id, 3 AS upgrade_type, domains.node_id AS node_id
FROM domains;
