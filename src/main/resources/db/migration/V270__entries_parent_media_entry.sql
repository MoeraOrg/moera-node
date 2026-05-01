ALTER TABLE entries ADD COLUMN parent_media_entry_id uuid;

WITH candidates AS (
    SELECT DISTINCT
        child.id AS child_id,
        parent.id AS parent_id,
        parent.created_at AS parent_created_at
    FROM entries child
        JOIN entry_attachments ea
            ON ea.media_file_owner_id = child.parent_media_id
        JOIN entry_revisions er
            ON er.id = ea.entry_revision_id
        JOIN entries parent
            ON parent.id = er.entry_id
    WHERE child.parent_media_id IS NOT NULL
      AND parent.node_id = child.node_id
      AND parent.id <> child.id
      AND parent.receiver_name IS NOT DISTINCT FROM child.receiver_name
),
ranked AS (
    SELECT
        child_id,
        parent_id,
        row_number() OVER (
            PARTITION BY child_id
            ORDER BY parent_created_at, parent_id
        ) AS ordinal
    FROM candidates
)
UPDATE entries child
SET parent_media_entry_id = ranked.parent_id
FROM ranked
WHERE child.id = ranked.child_id
  AND ranked.ordinal = 1;

ALTER TABLE entries
    ADD CONSTRAINT entries_parent_media_entry_id_fkey
    FOREIGN KEY (parent_media_entry_id) REFERENCES entries(id)
    ON UPDATE CASCADE ON DELETE CASCADE;

CREATE INDEX entries_parent_media_entry_id_idx
    ON entries(parent_media_entry_id)
    WHERE parent_media_entry_id IS NOT NULL;

CREATE UNIQUE INDEX entries_parent_media_entry_id_parent_media_id_idx
    ON entries(parent_media_entry_id, parent_media_id)
    WHERE parent_media_entry_id IS NOT NULL
      AND parent_media_id IS NOT NULL;

DROP INDEX IF EXISTS entries_parent_media_id_idx;
DROP INDEX IF EXISTS entries_parent_media_id_null_idx;
DROP INDEX IF EXISTS entries_parent_media_id_not_null_idx;
DROP INDEX IF EXISTS entries_parent_media_id_receiver_name_idx;
