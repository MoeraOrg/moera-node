CREATE TABLE remote_verifications (
    id uuid NOT NULL PRIMARY KEY,
    node_id uuid NOT NULL,
    verification_type smallint NOT NULL,
    node_name character varying(63) NOT NULL,
    posting_id character varying(40) NOT NULL,
    comment_id character varying(40) NOT NULL,
    revision_id character varying(40),
    owner_name character varying(63),
    status smallint NOT NULL,
    error_code character varying(63),
    error_message character varying(255),
    deadline timestamp without time zone NOT NULL
);
CREATE INDEX ON remote_verifications(deadline);
CREATE INDEX ON remote_verifications(node_id, verification_type, node_name, posting_id, revision_id);
CREATE INDEX ON remote_verifications(node_id, verification_type, node_name, posting_id, owner_name);
DROP TABLE remote_comment_verifications;
DROP TABLE remote_posting_verifications;
DROP TABLE remote_reaction_verifications;
