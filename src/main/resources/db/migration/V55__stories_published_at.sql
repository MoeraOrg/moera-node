ALTER TABLE stories ADD published_at timestamp without time zone NOT NULL DEFAULT NOW();
UPDATE stories
SET published_at = (
    SELECT published_at
    FROM entry_revisions INNER JOIN entries ON entries.current_revision_id = entry_revisions.id
    WHERE entries.id = stories.entry_id
)
WHERE entry_id IS NOT NULL;
