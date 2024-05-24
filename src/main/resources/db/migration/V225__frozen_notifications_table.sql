CREATE TABLE frozen_notifications (
    id uuid NOT NULL PRIMARY KEY,
    node_id uuid NOT NULL,
    packet TEXT NOT NULL,
    received_at timestamp without time zone NOT NULL,
    deadline timestamp without time zone NOT NULL
);
CREATE INDEX ON frozen_notifications(node_id);
CREATE INDEX ON frozen_notifications(deadline);
