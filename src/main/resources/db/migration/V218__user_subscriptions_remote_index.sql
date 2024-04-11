CREATE INDEX user_subscriptions_remote_entry_idx ON user_subscriptions(node_id, subscription_type, remote_entry_id);
CREATE INDEX user_subscriptions_remote_node_idx ON user_subscriptions(node_id, remote_node_name);
