CREATE TABLE remote_posting_verifications (
    id uuid NOT NULL PRIMARY KEY,
    node_id uuid NOT NULL,
    node_name character varying(63) NOT NULL,
    posting_id character varying(40) NOT NULL,
    revision_id character varying(40),
    status smallint NOT NULL,
    error_code character varying(63),
    error_message character varying(255)
);
CREATE INDEX ON remote_posting_verifications(node_id, node_name, posting_id, revision_id);
