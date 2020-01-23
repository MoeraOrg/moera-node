CREATE TABLE remote_reaction_verifications (
    id uuid NOT NULL PRIMARY KEY,
    node_id uuid NOT NULL,
    node_name character varying(63) NOT NULL,
    posting_id character varying(40) NOT NULL,
    reaction_owner_name character varying(63) NOT NULL,
    status smallint NOT NULL,
    error_code character varying(63),
    error_message character varying(255),
    deadline timestamp without time zone NOT NULL
);
CREATE INDEX ON remote_reaction_verifications(node_id, node_name, posting_id, reaction_owner_name);
CREATE INDEX ON remote_reaction_verifications(deadline);
