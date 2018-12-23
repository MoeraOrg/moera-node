CREATE SEQUENCE hibernate_sequence START WITH 1 INCREMENT BY 1;
CREATE TABLE options (
    id bigint NOT NULL PRIMARY KEY,
    node_id uuid NOT NULL,
    name character varying(128) NOT NULL,
    value character varying(4096)
);
CREATE INDEX ON options(node_id);
CREATE INDEX ON options(name);
