ALTER TABLE entries ADD COLUMN receiver_edited_at timestamp without time zone;
UPDATE entries
SET receiver_edited_at = (
    SELECT receiver_created_at
    FROM entry_revisions
    WHERE entry_revisions.id = entries.current_revision_id
)
WHERE receiver_name IS NOT NULL;
INSERT INTO entry_revision_upgrades
SELECT nextval('hibernate_sequence') AS id, 2 AS upgrade_type, entry_revisions.id AS entry_revision_id
FROM entry_revisions;
