CREATE INDEX push_clients_node_id_last_seen_at_idx ON push_clients(node_id, last_seen_at);
CREATE INDEX push_notifications_moment_push_client_id_idx ON push_notifications(moment, push_client_id);
