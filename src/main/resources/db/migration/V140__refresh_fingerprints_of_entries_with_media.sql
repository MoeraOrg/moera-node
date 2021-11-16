INSERT INTO entry_revision_upgrades
SELECT nextval('hibernate_sequence') AS id, 0 AS upgrade_type, entry_revisions.id AS entry_revision_id
FROM entry_revisions LEFT JOIN entries ON entry_revisions.entry_id = entries.id
WHERE entries.receiver_name IS NULL
      AND EXISTS(SELECT * FROM entry_attachments WHERE entry_revision_id = entry_revisions.id);
INSERT INTO entry_revision_upgrades
SELECT nextval('hibernate_sequence') AS id, 2 AS upgrade_type, entry_revisions.id AS entry_revision_id
FROM entry_revisions LEFT JOIN entries ON entry_revisions.entry_id = entries.id
WHERE entries.receiver_name IS NULL
      AND EXISTS(SELECT * FROM entry_attachments WHERE entry_revision_id = entry_revisions.id);
