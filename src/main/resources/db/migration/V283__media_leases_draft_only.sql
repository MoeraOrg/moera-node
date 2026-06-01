ALTER TABLE media_leases ADD COLUMN draft_only boolean NOT NULL DEFAULT false;
ALTER TABLE media_leases ADD COLUMN deadline timestamp without time zone;

CREATE INDEX media_leases_draft_only_deadline_idx
    ON media_leases(deadline) WHERE draft_only = true AND deadline IS NOT NULL;
CREATE INDEX entry_attachments_draft_media_file_lease_id_idx
    ON entry_attachments(media_file_lease_id) WHERE draft_id IS NOT NULL AND media_file_lease_id IS NOT NULL;
