CREATE TABLE blocked_instants (
    id uuid NOT NULL PRIMARY KEY,
    node_id uuid NOT NULL,
    story_type smallint NOT NULL,
    entry_id uuid,
    created_at timestamp without time zone NOT NULL
);
CREATE INDEX blocked_instants_entry_id_idx ON blocked_instants(entry_id);
ALTER TABLE blocked_instants ADD FOREIGN KEY (entry_id) REFERENCES entries(id)
    ON UPDATE CASCADE ON DELETE CASCADE;
