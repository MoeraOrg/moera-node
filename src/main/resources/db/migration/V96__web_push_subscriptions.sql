CREATE TABLE web_push_subscriptions (
    id uuid NOT NULL PRIMARY KEY,
    node_id uuid NOT NULL,
    endpoint varchar(255) NOT NULL,
    public_key varchar(128) NOT NULL,
    auth_key varchar(32) NOT NULL,
    created_at timestamp without time zone NOT NULL
);
