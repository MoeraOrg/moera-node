ALTER TABLE entries ADD COLUMN replied_to_revision_id uuid;
CREATE INDEX ON entries(replied_to_revision_id);
ALTER TABLE entries
ADD FOREIGN KEY (replied_to_revision_id)
REFERENCES entry_revisions(id)
ON UPDATE CASCADE ON DELETE SET NULL;
UPDATE entries
SET replied_to_revision_id = (
    SELECT id
    FROM entry_revisions
    WHERE entry_id = entries.replied_to_id AND created_at = (
       SELECT MAX(created_at)
       FROM entry_revisions
       WHERE entry_id = entries.replied_to_id AND created_at <= entries.created_at
    )
)
WHERE replied_to_id IS NOT NULL;
