ALTER TABLE contacts ADD COLUMN feed_subscription_count integer NOT NULL DEFAULT 0;
ALTER TABLE contacts ADD COLUMN feed_subscriber_count integer NOT NULL DEFAULT 0;
ALTER TABLE contacts ADD COLUMN friend_count integer NOT NULL DEFAULT 0;
ALTER TABLE contacts ADD COLUMN friend_of_count integer NOT NULL DEFAULT 0;

UPDATE contacts
SET feed_subscription_count = s.total
FROM (
    SELECT node_id, remote_node_name, count(*) AS total
    FROM user_subscriptions
    WHERE subscription_type = 0
    GROUP BY node_id, remote_node_name
) AS s
WHERE contacts.node_id = s.node_id AND contacts.remote_node_name = s.remote_node_name;

UPDATE contacts
SET feed_subscriber_count = s.total
FROM (
    SELECT node_id, remote_node_name, count(*) AS total
    FROM subscribers
    WHERE subscription_type = 0
    GROUP BY node_id, remote_node_name
) AS s
WHERE contacts.node_id = s.node_id AND contacts.remote_node_name = s.remote_node_name;

UPDATE contacts
SET friend_count = s.total
FROM (
    SELECT node_id, remote_node_name, count(*) AS total
    FROM friends
    GROUP BY node_id, remote_node_name
) AS s
WHERE contacts.node_id = s.node_id AND contacts.remote_node_name = s.remote_node_name;

UPDATE contacts
SET friend_of_count = s.total
FROM (
    SELECT node_id, remote_node_name, count(*) AS total
    FROM friend_ofs
    GROUP BY node_id, remote_node_name
) AS s
WHERE contacts.node_id = s.node_id AND contacts.remote_node_name = s.remote_node_name;
