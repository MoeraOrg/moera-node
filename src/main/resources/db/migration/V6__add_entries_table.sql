CREATE TABLE entries (
    id uuid NOT NULL PRIMARY KEY,
    node_id uuid NOT NULL,
    entry_type smallint NOT NULL,
    owner_name character varying(127),
    owner_generation integer,
    body_src text NOT NULL,
    body_html text NOT NULL,
    created timestamp without time zone NOT NULL,
    moment bigint NOT NULL,
    signature bytea
);
CREATE INDEX ON entries(node_id);
CREATE INDEX ON entries(entry_type);
CREATE INDEX ON entries(moment);
