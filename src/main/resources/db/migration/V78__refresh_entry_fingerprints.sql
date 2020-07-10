INSERT INTO entry_revision_upgrades
SELECT nextval('hibernate_sequence') AS id, 2 AS upgrade_type, entry_revisions.id AS entry_revision_id
FROM entry_revisions;
