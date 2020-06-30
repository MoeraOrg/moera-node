CREATE TABLE pending_notifications (
    id uuid NOT NULL PRIMARY KEY,
    node_id uuid NOT NULL,
    node_name character varying(63) NOT NULL,
    notification TEXT NOT NULL,
    created_at timestamp without time zone NOT NULL,
    subscription_created_at timestamp without time zone
);
CREATE INDEX ON pending_notifications(node_id);
CREATE INDEX ON pending_notifications(created_at);
