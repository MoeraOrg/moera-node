ALTER TABLE entries ADD COLUMN receiver_created_at timestamp without time zone;
ALTER TABLE entries ADD COLUMN current_receiver_revision_id VARCHAR(40);
ALTER TABLE entries ADD COLUMN receiver_entry_id VARCHAR(40);
CREATE UNIQUE INDEX ON entries(node_id, receiver_name, receiver_entry_id);
ALTER TABLE entry_revisions ADD COLUMN receiver_created_at timestamp without time zone;
ALTER TABLE entry_revisions ADD COLUMN receiver_deleted_at timestamp without time zone;
ALTER TABLE entry_revisions ADD COLUMN receiver_revision_id VARCHAR(40);
