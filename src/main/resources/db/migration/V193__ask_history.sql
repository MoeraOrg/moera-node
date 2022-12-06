CREATE TABLE ask_history (
    id uuid NOT NULL PRIMARY KEY,
    node_id uuid NOT NULL,
    remote_node_name varchar(63) NOT NULL,
    subject smallint NOT NULL,
    created_at timestamp without time zone NOT NULL
);
CREATE INDEX ask_history_node_id_remote_node_name_subject_idx ON ask_history(node_id, remote_node_name, subject);
