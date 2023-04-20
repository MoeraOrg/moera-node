CREATE TABLE sheriff_orders (
    id uuid NOT NULL PRIMARY KEY,
    node_id uuid NOT NULL,
    delete boolean DEFAULT false NOT NULL,
    remote_node_name varchar(63) NOT NULL,
    remote_feed_name varchar(63) NOT NULL,
    remote_posting_id varchar(40),
    remote_comment_id varchar(40),
    category smallint NOT NULL,
    reason_code smallint NOT NULL,
    reason_details text,
    created_at timestamp without time zone NOT NULL,
    signature bytea NOT NULL,
    signature_version smallint NOT NULL
);
CREATE INDEX sheriff_orders_target_idx
    ON sheriff_orders(node_id, remote_node_name, remote_feed_name, remote_posting_id, remote_comment_id);
