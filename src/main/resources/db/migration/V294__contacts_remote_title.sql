ALTER TABLE contacts ADD COLUMN remote_title varchar(120);

INSERT INTO contact_upgrades(id, node_id, upgrade_type, remote_node_name)
SELECT nextval('contact_upgrades_seq') AS id, contacts.node_id, 6 AS upgrade_type, contacts.remote_node_name
FROM contacts;
