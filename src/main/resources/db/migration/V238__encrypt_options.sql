INSERT INTO domain_upgrades
SELECT nextval('hibernate_sequence') AS id, 7 AS upgrade_type, domains.node_id AS node_id
FROM domains;
