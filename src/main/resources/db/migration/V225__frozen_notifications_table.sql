CREATE TABLE frozen_notifications (
    id uuid NOT NULL PRIMARY KEY,
    node_id uuid NOT NULL,
    packet TEXT NOT NULL,
    received_at timestamp without time zone NOT NULL,
    deadline timestamp without time zone NOT NULL
);
CREATE INDEX frozen_notifications_node_id_idx ON frozen_notifications(node_id);
CREATE INDEX frozen_notifications_deadline_idx ON frozen_notifications(deadline);
