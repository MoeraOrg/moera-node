INSERT INTO reminders
SELECT uuid_generate_v4() AS id, domains.node_id AS node_id, 43 AS story_type, 2 as priority, NULL AS published_at,
       NULL AS story_id, NULL AS read_at, 0 as read_count, now() AS next_at
FROM domains;
