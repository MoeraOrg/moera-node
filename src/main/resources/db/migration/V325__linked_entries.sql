CREATE TABLE linked_entries (
    id uuid PRIMARY KEY,
    entry_revision_id uuid NOT NULL REFERENCES entry_revisions(id) ON DELETE CASCADE,
    receiver_node_name varchar(135) NOT NULL,
    receiver_entry_id varchar(40) NOT NULL,
    linked_entry_id uuid REFERENCES entries(id) ON DELETE SET NULL
);

CREATE UNIQUE INDEX linked_entries_revision_receiver_idx
    ON linked_entries(entry_revision_id, receiver_node_name, receiver_entry_id);
CREATE INDEX linked_entries_linked_entry_id_idx ON linked_entries(linked_entry_id);

ALTER TABLE picks ADD COLUMN linked_entry_id uuid REFERENCES linked_entries(id) ON DELETE CASCADE;
CREATE INDEX picks_linked_entry_id_idx ON picks(linked_entry_id);
