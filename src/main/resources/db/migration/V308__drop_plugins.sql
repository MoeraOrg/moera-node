DELETE FROM tokens
WHERE plugin_name IS NOT NULL;

DELETE FROM options
WHERE name LIKE 'plugin.%';

DELETE FROM option_defaults
WHERE name LIKE 'plugin.%';

UPDATE tokens
SET auth_scope = auth_scope & ~262144::bigint;

UPDATE grants
SET auth_scope = auth_scope & ~262144::bigint;

UPDATE remote_grants
SET auth_scope = auth_scope & ~262144::bigint;

ALTER TABLE tokens
DROP COLUMN plugin_name;
