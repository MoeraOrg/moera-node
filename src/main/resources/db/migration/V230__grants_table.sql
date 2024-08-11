CREATE TABLE grants (
    id uuid NOT NULL PRIMARY KEY,
    node_id uuid NOT NULL,
    node_name varchar(63) NOT NULL,
    auth_scope bigint NOT NULL,
    updated_at timestamp without time zone NOT NULL
);
CREATE UNIQUE INDEX grants_node_id_name_idx ON grants(node_id, node_name);

CREATE TABLE remote_grants (
    id uuid NOT NULL PRIMARY KEY,
    node_id uuid NOT NULL,
    remote_node_name varchar(63) NOT NULL,
    auth_scope bigint NOT NULL,
    updated_at timestamp without time zone NOT NULL
);
CREATE UNIQUE INDEX remote_grants_node_id_name_idx ON remote_grants(node_id, remote_node_name);
