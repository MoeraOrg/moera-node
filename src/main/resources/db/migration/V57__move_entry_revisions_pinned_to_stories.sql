ALTER TABLE stories ADD pinned boolean NOT NULL DEFAULT false;
UPDATE stories
SET pinned = (
    SELECT pinned
    FROM entry_revisions INNER JOIN entries ON entries.current_revision_id = entry_revisions.id
    WHERE entries.id = stories.entry_id
)
WHERE entry_id IS NOT NULL;
ALTER TABLE entry_revisions DROP COLUMN pinned;
