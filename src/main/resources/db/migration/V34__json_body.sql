INSERT INTO entry_revision_upgrades
SELECT nextval('hibernate_sequence') AS id, 1 AS upgrade_type, entry_revisions.id AS entry_revision_id
FROM entry_revisions;
INSERT INTO entry_revision_upgrades
SELECT nextval('hibernate_sequence') AS id, 0 AS upgrade_type, entry_revisions.id AS entry_revision_id
FROM entry_revisions
     LEFT JOIN entries ON entry_revisions.entry_id = entries.id
WHERE entries.owner_name = (
      SELECT value FROM options WHERE options.name = 'profile.registered-name' AND options.node_id = entries.node_id);
