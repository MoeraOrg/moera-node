CREATE TABLE push_clients (
    id uuid NOT NULL PRIMARY KEY,
    node_id uuid NOT NULL,
    client_id varchar(24) NOT NULL,
    last_seen_at timestamp without time zone NOT NULL
);

CREATE TABLE push_notifications (
    id uuid NOT NULL PRIMARY KEY,
    push_client_id uuid NOT NULL,
    moment bigint NOT NULL,
    content text NOT NULL
);

CREATE UNIQUE INDEX push_clients_node_id_client_id_idx ON push_clients(node_id, client_id);
CREATE INDEX push_notifications_push_client_id_idx ON push_notifications(push_client_id);
ALTER TABLE push_notifications ADD FOREIGN KEY (push_client_id) REFERENCES push_clients(id) ON UPDATE CASCADE ON DELETE CASCADE;
