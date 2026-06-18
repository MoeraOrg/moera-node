WITH images AS (
    SELECT media_file_owners.id AS id
    FROM media_file_owners JOIN media_files ON media_file_owners.media_file_id=media_files.id
    WHERE media_files.mime_type LIKE 'image/%' AND media_file_owners.title IS NOT NULL
)
UPDATE media_file_owners SET title = NULL FROM images WHERE media_file_owners.id = images.id;
UPDATE entry_revisions SET attachments_cache = NULL;
