CREATE SEQUENCE contact_upgrades_seq START WITH 1 INCREMENT BY 50;
SELECT setval('contact_upgrades_seq', last_value, is_called) FROM hibernate_sequence;
CREATE SEQUENCE domain_upgrades_seq START WITH 1 INCREMENT BY 50;
SELECT setval('domain_upgrades_seq', last_value, is_called) FROM hibernate_sequence;
CREATE SEQUENCE entry_revision_seq START WITH 1 INCREMENT BY 50;
SELECT setval('entry_revision_seq', last_value, is_called) FROM hibernate_sequence;
CREATE SEQUENCE public_pages_seq START WITH 1 INCREMENT BY 50;
SELECT setval('public_pages_seq', last_value, is_called) FROM hibernate_sequence;
DROP SEQUENCE hibernate_sequence;
