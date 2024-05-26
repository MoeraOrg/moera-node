CREATE TABLE remote_connectivity (
    id uuid NOT NULL PRIMARY KEY,
    remote_node_name varchar(63) NOT NULL,
    status smallint NOT NULL,
    updated_at timestamp without time zone NOT NULL
);
CREATE UNIQUE INDEX remote_connectivity_remote_node_name_idx ON remote_connectivity(remote_node_name);
