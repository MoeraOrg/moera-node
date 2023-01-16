ALTER TABLE blocked_instants ADD COLUMN remote_node_name varchar(63);
ALTER TABLE blocked_instants ADD COLUMN remote_posting_id varchar(40);
ALTER TABLE blocked_instants ADD COLUMN remote_owner_name varchar(63);
ALTER TABLE blocked_instants ADD COLUMN deadline timestamp without time zone;
CREATE INDEX blocked_instants_node_id_type_remote_idx
    ON blocked_instants(node_id, story_type, remote_node_name, remote_posting_id);
CREATE INDEX blocked_instants_node_id_type_owner_idx
    ON blocked_instants(node_id, story_type, remote_owner_name);
CREATE INDEX blocked_instants_deadline_idx ON blocked_instants(deadline);
