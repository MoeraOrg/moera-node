ALTER TABLE stories ADD COLUMN remote_owner_name varchar(63);
ALTER TABLE stories ADD COLUMN remote_heading varchar(255);
DROP INDEX stories_node_id_remote_node_name_remote_entry_id_idx;
CREATE INDEX ON stories(node_id, remote_node_name, remote_posting_id, remote_comment_id);
