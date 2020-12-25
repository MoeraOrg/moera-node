DELETE FROM web_push_subscriptions;
CREATE UNIQUE INDEX ON web_push_subscriptions(node_id, public_key, auth_key);
