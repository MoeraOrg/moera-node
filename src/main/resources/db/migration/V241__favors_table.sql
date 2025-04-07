CREATE TABLE favors (
    id uuid PRIMARY KEY,
    node_id uuid NOT NULL,
    node_name varchar(63) NOT NULL,
    value real NOT NULL,
    decay_hours integer NOT NULL,
    created_at timestamp without time zone NOT NULL,
    deadline timestamp without time zone NOT NULL
);
CREATE INDEX favors_node_id_name_idx ON favors(node_id, node_name);
CREATE INDEX favors_deadline_idx ON favors(deadline);

ALTER TABLE contacts DROP COLUMN closeness_base;
ALTER TABLE contacts DROP COLUMN closeness;
ALTER TABLE contacts ADD COLUMN distance real DEFAULT 3 NOT NULL;
CREATE INDEX contacts_node_id_distance_idx ON contacts(node_id, distance);

ALTER TABLE own_postings ADD COLUMN remote_parent_media_id varchar(40);
