CREATE TABLE subscriptions (
    id uuid NOT NULL PRIMARY KEY,
    node_id uuid NOT NULL,
    subscription_type smallint NOT NULL,
    feed_name character varying(63) NOT NULL,
    remote_subscription_id character varying(40) NOT NULL,
    remote_node_name character varying(63) NOT NULL,
    remote_feed_name character varying(63),
    remote_entry_id character varying(40),
    created_at timestamp without time zone NOT NULL
);
CREATE INDEX ON subscriptions(remote_subscription_id);
CREATE INDEX ON subscriptions(node_id, subscription_type, remote_node_name);
CREATE TABLE subscribers (
    id uuid NOT NULL PRIMARY KEY,
    node_id uuid NOT NULL,
    subscription_type smallint NOT NULL,
    feed_name character varying(63) NOT NULL,
    entry_id UUID,
    remote_node_name character varying(63) NOT NULL,
    created_at timestamp without time zone NOT NULL
);
CREATE INDEX ON subscribers(node_id, feed_name);
CREATE INDEX ON subscribers(entry_id);
ALTER TABLE subscribers ADD FOREIGN KEY (entry_id) REFERENCES entries(id) ON UPDATE CASCADE ON DELETE CASCADE;
