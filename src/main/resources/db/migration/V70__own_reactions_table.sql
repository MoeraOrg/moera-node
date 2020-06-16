CREATE TABLE own_reactions (
    id uuid NOT NULL PRIMARY KEY,
    node_id uuid NOT NULL,
    remote_node_name character varying(63) NOT NULL,
    remote_posting_id character varying(40) NOT NULL,
    negative boolean NOT NULL,
    emoji integer NOT NULL,
    created_at timestamp without time zone NOT NULL
);
CREATE UNIQUE INDEX ON own_reactions(node_id, remote_node_name, remote_posting_id);
