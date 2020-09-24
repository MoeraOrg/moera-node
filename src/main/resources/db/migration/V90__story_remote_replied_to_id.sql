ALTER TABLE stories ADD COLUMN remote_replied_to_id varchar(40);
ALTER TABLE stories ADD COLUMN remote_replied_to_heading varchar(255);
CREATE INDEX ON stories(node_id, remote_node_name, remote_posting_id, remote_replied_to_id);
