UPDATE options opname
SET value = CONCAT(value, '_', (
    SELECT value
    FROM options opgen
    WHERE name = 'profile.registered-name.generation' AND node_id = opname.node_id
)) WHERE name = 'profile.registered-name';

