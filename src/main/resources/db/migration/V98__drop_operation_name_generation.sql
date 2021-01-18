UPDATE options SET value = concat(value, '_0') WHERE name = 'naming.operation.registered-name';
DELETE FROM options WHERE name = 'naming.operation.registered-name.generation';
