CREATE TABLE entry_revision_upgrades (
    id bigint NOT NULL PRIMARY KEY,
    upgrade_type smallint NOT NULL,
    entry_revision_id uuid NOT NULL REFERENCES entry_revisions(id) ON UPDATE CASCADE ON DELETE CASCADE
);
CREATE INDEX ON entry_revision_upgrades(entry_revision_id);
INSERT INTO entry_revision_upgrades
SELECT nextval('hibernate_sequence') AS id, 0 AS upgrade_type, entry_revisions.id AS entry_revision_id
FROM entry_revisions
     LEFT JOIN entries ON entry_revisions.entry_id = entries.id
WHERE signature = '' AND entries.owner_name = (
      SELECT value FROM options WHERE options.name = 'profile.registered-name' AND options.node_id = entries.node_id);
