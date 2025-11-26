CREATE TABLE verified_emails (
    id uuid NOT NULL PRIMARY KEY,
    node_id uuid NOT NULL,
    email character varying(127) NOT NULL
);
CREATE INDEX verified_emails_node_id_email_idx ON verified_emails(node_id, email);

INSERT INTO verified_emails(id, node_id, email)
SELECT uuid_generate_v4(), options.node_id, options.value
FROM options
WHERE options.name = 'profile.email';

INSERT INTO options(node_id, name, value, id)
SELECT options.node_id, 'profile.email.verified', true, uuid_generate_v4()
FROM options
WHERE options.name = 'profile.email';
