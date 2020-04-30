ALTER TABLE stories ADD COLUMN remote_node_name varchar(63);
ALTER TABLE stories ADD COLUMN remote_entry_id varchar(40);
CREATE INDEX ON stories(node_id, remote_node_name, remote_entry_id);
