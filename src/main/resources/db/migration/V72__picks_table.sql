CREATE TABLE picks (
    id uuid NOT NULL PRIMARY KEY,
    node_id uuid NOT NULL,
    feed_name character varying(63),
    remote_node_name character varying(63) NOT NULL,
    remote_feed_name character varying(63),
    remote_posting_id character varying(40) NOT NULL,
    created_at timestamp without time zone NOT NULL,
    retry_at timestamp without time zone
);
CREATE INDEX ON picks(node_id);
