ALTER TABLE reactions ADD COLUMN deleted_at TIMESTAMP WITHOUT TIME ZONE;
DROP INDEX reactions_owner_name_entry_revision_id_idx;
CREATE INDEX ON reactions(entry_revision_id, deleted_at, owner_name);
