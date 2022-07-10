CREATE INDEX comments_slice_idx
    ON entries(node_id, parent_id, moment, view_principal, owner_name)
    WHERE deleted_at IS NULL;
