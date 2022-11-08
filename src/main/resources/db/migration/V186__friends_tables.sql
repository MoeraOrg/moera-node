CREATE TABLE friend_groups (
    id uuid NOT NULL PRIMARY KEY,
    node_id uuid NOT NULL,
    title varchar(63) NOT NULL,
    visible boolean NOT NULL
);
CREATE INDEX friend_groups_node_id_idx ON friend_groups(node_id);

INSERT INTO friend_groups(id, node_id, title, visible)
SELECT uuid_generate_v4() AS id, domains.node_id AS node_id, 't:friends' AS title, true AS visible
FROM domains;

CREATE TABLE friends (
    id uuid NOT NULL PRIMARY KEY,
    node_name varchar(63) NOT NULL,
    friend_group_id uuid NOT NULL
);
CREATE INDEX friends_friend_group_id_idx ON friends(friend_group_id);
CREATE INDEX friends_node_name_idx ON friends(node_name);
ALTER TABLE friends ADD FOREIGN KEY (friend_group_id) REFERENCES friend_groups(id) ON UPDATE CASCADE ON DELETE CASCADE;
