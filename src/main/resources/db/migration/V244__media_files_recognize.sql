ALTER TABLE media_files ADD COLUMN recognize_at timestamp without time zone;
ALTER TABLE media_files ADD COLUMN recognized_text text;
ALTER TABLE media_files ADD COLUMN recognized_at timestamp without time zone;
CREATE INDEX media_files_recognize_idx ON media_files(recognized_at, recognize_at);
WITH numbered AS (
    SELECT DISTINCT media_files.id AS id, (row_number() OVER (ORDER BY media_files.created_at DESC) - 1) AS idx
    FROM entry_attachments
        LEFT JOIN media_file_owners ON media_file_owners.id = entry_attachments.media_file_owner_id
        LEFT JOIN media_files ON media_files.id = media_file_owners.media_file_id
    WHERE media_files.file_size < 1048000
)
UPDATE media_files AS m
SET recognize_at = NOW() + (n.idx * INTERVAL '4 minutes')
FROM numbered AS n
WHERE m.id = n.id;
