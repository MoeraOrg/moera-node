ALTER TABLE subscribers ADD COLUMN remote_gender varchar(31);
ALTER TABLE subscriptions ADD COLUMN remote_gender varchar(31);
ALTER TABLE contacts ADD COLUMN remote_gender varchar(31);
ALTER TABLE entries ADD COLUMN owner_gender varchar(31);
ALTER TABLE entries ADD COLUMN receiver_gender varchar(31);
ALTER TABLE entries ADD COLUMN replied_to_gender varchar(31);
INSERT INTO domain_upgrades
SELECT nextval('hibernate_sequence') AS id, 5 AS upgrade_type, domains.node_id AS node_id
FROM domains;
