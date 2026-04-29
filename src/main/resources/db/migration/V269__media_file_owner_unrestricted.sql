ALTER TABLE media_file_owners ADD COLUMN unrestricted boolean NOT NULL DEFAULT false;

UPDATE media_file_owners AS mfo
SET
    unrestricted = EXISTS (
        SELECT 1
        FROM entry_attachments ea
        JOIN entry_revisions er ON er.id = ea.entry_revision_id
        JOIN entries e ON e.id = er.entry_id
        LEFT JOIN entries p ON p.id = e.parent_id
        WHERE ea.media_file_owner_id = mfo.id
          AND e.deleted_at IS NULL
          AND e.current_revision_id = er.id
          AND e.view_principal = 'public'
          AND e.parent_view_principal IN ('unset', 'public')
          AND (
              e.parent_id IS NULL
              OR (
                  p.deleted_at IS NULL
                  AND p.view_principal = 'public'
                  AND p.parent_view_principal IN ('unset', 'public')
                  AND p.view_comments_principal = 'public'
                  AND p.parent_view_comments_principal IN ('unset', 'public')
              )
          )
    ),
    permissions_updated_at = now();

ALTER TABLE media_file_owners DROP COLUMN view_principal;
