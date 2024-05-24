INSERT INTO options(id, node_id, name, value)
SELECT uuid_generate_v4(), node_id, 'frozen', 'true'
FROM domains
WHERE NOT EXISTS (SELECT * FROM tokens WHERE tokens.node_id = domains.node_id)
    AND created_at < now() - interval '1 year';
