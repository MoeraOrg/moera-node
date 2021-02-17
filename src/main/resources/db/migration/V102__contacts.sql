CREATE TABLE contacts (
    id uuid NOT NULL PRIMARY KEY,
    node_id uuid NOT NULL,
    remote_node_name varchar(63) NOT NULL,
    remote_full_name varchar(96),
    closeness_base real NOT NULL DEFAULT 0,
    closeness real NOT NULL DEFAULT 1,
    created_at timestamp without time zone NOT NULL,
    updated_at timestamp without time zone NOT NULL
);
CREATE UNIQUE INDEX ON contacts(node_id, remote_node_name);
CREATE INDEX ON contacts(node_id, closeness);
CREATE INDEX ON contacts(updated_at);
INSERT INTO contacts(id, node_id, remote_node_name, created_at, updated_at)
(
    SELECT DISTINCT ON (node_id, remote_node_name) uuid_generate_v4(), node_id, remote_node_name, now(), now()
    FROM subscriptions
    WHERE subscription_type = 0
);
UPDATE contacts
SET closeness =
    (
        SELECT COUNT(*)
        FROM subscriptions
        WHERE node_id = contacts.node_id AND remote_node_name = contacts.remote_node_name
    )
    + (
        SELECT COUNT(*)
        FROM own_comments
        WHERE node_id = contacts.node_id AND remote_node_name = contacts.remote_node_name
      )
    + (
        SELECT COUNT(*)
        FROM own_reactions
        WHERE node_id = contacts.node_id AND remote_node_name = contacts.remote_node_name
      ) * 0.25
    + (
        SELECT COUNT(*)
        FROM entries
        WHERE node_id = contacts.node_id AND owner_name = contacts.remote_node_name AND deleted_at IS NULL
            AND receiver_name IS NULL
      )
    + (
        SELECT COUNT(*)
        FROM entries
        WHERE node_id = contacts.node_id AND replied_to_name = contacts.remote_node_name
            AND owner_name = (SELECT value FROM options WHERE node_id = contacts.node_id AND name = 'profile.node-name')
            AND deleted_at IS NULL AND receiver_name IS NULL
      );
CREATE INDEX ON entries(node_id, owner_name, replied_to_name);
