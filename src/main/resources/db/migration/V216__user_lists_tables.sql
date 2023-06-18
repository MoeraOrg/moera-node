CREATE TABLE user_list_items (
    id uuid NOT NULL PRIMARY KEY,
    node_id uuid NOT NULL,
    list_name varchar(63) NOT NULL,
    node_name varchar(63) NOT NULL,
    created_at timestamp without time zone NOT NULL,
    moment bigint NOT NULL
);
CREATE UNIQUE INDEX user_list_items_name_idx ON user_list_items(node_id, list_name, node_name);
CREATE INDEX user_list_items_moment_idx ON user_list_items(node_id, list_name, moment);

CREATE TABLE remote_user_list_items (
    id uuid NOT NULL PRIMARY KEY,
    node_id uuid NOT NULL,
    list_node_name varchar(63) NOT NULL,
    list_name varchar(63) NOT NULL,
    node_name varchar(63) NOT NULL,
    absent boolean DEFAULT false NOT NULL,
    cached_at timestamp without time zone NOT NULL,
    deadline timestamp without time zone NOT NULL
);
CREATE UNIQUE INDEX remote_user_list_items_name_idx
    ON remote_user_list_items(node_id, list_node_name, list_name, node_name);
CREATE INDEX remote_user_list_items_deadline_idx ON remote_user_list_items(deadline);

ALTER TABLE entries ADD COLUMN sheriff_user_list_referred boolean NOT NULL DEFAULT false;
