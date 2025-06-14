WITH latest AS (
    SELECT DISTINCT ON (mf.id)
           mf.id,
           mf.created_at
    FROM   entry_attachments ea
    JOIN   media_file_owners mfo ON mfo.id = ea.media_file_owner_id
    JOIN   media_files       mf  ON mf.id  = mfo.media_file_id
    WHERE  mf.file_size < 1048000 AND mf.recognized_at IS NULL
    ORDER  BY mf.id, mf.created_at DESC
),

numbered AS (
    SELECT id,
           row_number() OVER (ORDER BY created_at DESC) - 1 AS idx
    FROM   latest
)

UPDATE media_files AS m
SET    recognize_at = NOW() + numbered.idx * INTERVAL '4 minutes'
FROM   numbered
WHERE  m.id = numbered.id;
