CREATE TABLE email_verifications (
    id uuid NOT NULL PRIMARY KEY,
    node_id uuid NOT NULL,
    email character varying(127) NOT NULL,
    token character varying(64) NOT NULL,
    deadline timestamp without time zone NOT NULL
);
CREATE INDEX email_verifications_token_idx ON email_verifications(token);
CREATE INDEX email_verifications_deadline_idx ON email_verifications(deadline);
