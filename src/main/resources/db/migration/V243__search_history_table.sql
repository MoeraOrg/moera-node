CREATE TABLE search_history (
    id uuid NOT NULL PRIMARY KEY,
    node_id uuid NOT NULL,
    query varchar(1024) NOT NULL,
    created_at timestamp without time zone NOT NULL,
    deadline timestamp without time zone NOT NULL
);
CREATE INDEX search_history_node_id_query_idx ON search_history(node_id, query);
CREATE INDEX search_history_node_id_created_at_idx ON search_history(node_id, created_at);
CREATE INDEX search_history_deadline_idx ON search_history(deadline);
