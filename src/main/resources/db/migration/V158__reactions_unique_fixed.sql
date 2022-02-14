DROP INDEX reactions_entry_revision_id_null_owner_name_id_idx;
CREATE UNIQUE INDEX reactions_entry_revision_id_null_owner_name_id_idx ON reactions(entry_revision_id, owner_name)
    WHERE deleted_at IS NULL;
