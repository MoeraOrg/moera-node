INSERT INTO contacts(id, node_id, remote_node_name, closeness_base, closeness, created_at, updated_at)
    SELECT uuid_generate_v4() AS id, node_id, remote_node_name, 0 AS closeness_base, -1 AS closeness,
           now() AS created_at, now() AS updated_at
    FROM user_subscriptions
    WHERE NOT EXISTS(
        SELECT *
        FROM contacts cnt
        WHERE cnt.node_id = user_subscriptions.node_id AND cnt.remote_node_name = user_subscriptions.remote_node_name
    )
    GROUP BY node_id, remote_node_name;
INSERT INTO contacts(id, node_id, remote_node_name, closeness_base, closeness, created_at, updated_at)
    SELECT uuid_generate_v4() AS id, node_id, remote_node_name, 0 AS closeness_base, -1 AS closeness,
           now() AS created_at, now() AS updated_at
    FROM subscribers
    WHERE NOT EXISTS(
        SELECT *
        FROM contacts cnt
        WHERE cnt.node_id = subscribers.node_id AND cnt.remote_node_name = subscribers.remote_node_name
    )
    GROUP BY node_id, remote_node_name;
INSERT INTO contacts(id, node_id, remote_node_name, closeness_base, closeness, created_at, updated_at)
    SELECT uuid_generate_v4() AS id, node_id, remote_node_name, 0 AS closeness_base, -1 AS closeness,
           now() AS created_at, now() AS updated_at
    FROM own_comments
    WHERE NOT EXISTS(
        SELECT *
        FROM contacts cnt
        WHERE cnt.node_id = own_comments.node_id AND cnt.remote_node_name = own_comments.remote_node_name
    )
    GROUP BY node_id, remote_node_name;
INSERT INTO contacts(id, node_id, remote_node_name, closeness_base, closeness, created_at, updated_at)
    SELECT uuid_generate_v4() AS id, node_id, remote_replied_to_name AS remote_node_name, 0 AS closeness_base,
           -1 AS closeness, now() AS created_at, now() AS updated_at
    FROM own_comments
    WHERE NOT EXISTS(
        SELECT *
        FROM contacts cnt
        WHERE cnt.node_id = own_comments.node_id AND cnt.remote_node_name = own_comments.remote_replied_to_name
    ) AND remote_replied_to_name IS NOT NULL
    GROUP BY node_id, remote_replied_to_name;
INSERT INTO contacts(id, node_id, remote_node_name, closeness_base, closeness, created_at, updated_at)
    SELECT uuid_generate_v4() AS id, node_id, remote_node_name, 0 AS closeness_base, -1 AS closeness,
           now() AS created_at, now() AS updated_at
    FROM own_reactions
    WHERE NOT EXISTS(
        SELECT *
        FROM contacts cnt
        WHERE cnt.node_id = own_reactions.node_id AND cnt.remote_node_name = own_reactions.remote_node_name
    )
    GROUP BY node_id, remote_node_name;
INSERT INTO contacts(id, node_id, remote_node_name, closeness_base, closeness, created_at, updated_at)
    SELECT uuid_generate_v4() AS id, node_id, owner_name AS remote_node_name, 0 AS closeness_base, -1 AS closeness,
           now() AS created_at, now() AS updated_at
    FROM entries
    WHERE NOT EXISTS(
        SELECT *
        FROM contacts cnt
        WHERE cnt.node_id = entries.node_id AND cnt.remote_node_name = entries.owner_name
    ) AND entry_type = 1 AND deleted_at IS NULL
    GROUP BY node_id, owner_name;
INSERT INTO contacts(id, node_id, remote_node_name, closeness_base, closeness, created_at, updated_at)
    SELECT uuid_generate_v4() AS id, node_id, replied_to_name AS remote_node_name, 0 AS closeness_base, -1 AS closeness,
           now() AS created_at, now() AS updated_at
    FROM entries
    WHERE NOT EXISTS(
        SELECT *
        FROM contacts cnt
        WHERE cnt.node_id = entries.node_id AND cnt.remote_node_name = entries.replied_to_name
    ) AND entry_type = 1 AND deleted_at IS NULL AND replied_to_name IS NOT NULL
    GROUP BY node_id, replied_to_name;

CREATE TABLE contact_upgrades (
    id bigint NOT NULL PRIMARY KEY,
    node_id uuid NOT NULL,
    upgrade_type smallint NOT NULL,
    remote_node_name varchar(63) NOT NULL
);
CREATE INDEX contact_upgrades_upgrade_type_node_id_idx ON contact_upgrades(upgrade_type, node_id);
INSERT INTO contact_upgrades
SELECT nextval('hibernate_sequence') AS id, node_id, 6 AS upgrade_type, remote_node_name
FROM contacts
WHERE closeness < 0;

UPDATE contacts
SET closeness = (
        SELECT count(*)
        FROM own_comments
        WHERE own_comments.node_id = contacts.node_id AND own_comments.remote_node_name = contacts.remote_node_name
    )
    + (
        SELECT count(*)
        FROM own_reactions
        WHERE own_reactions.node_id = contacts.node_id AND own_reactions.remote_node_name = contacts.remote_node_name
    ) * 0.25
    + (
        SELECT count(*)
        FROM entries
        WHERE entries.node_id = contacts.node_id AND entries.owner_name = contacts.remote_node_name AND entry_type = 1
              AND entries.deleted_at IS NULL
    )
    + (
        SELECT count(*)
        FROM entries
        WHERE entries.node_id = contacts.node_id AND entries.replied_to_name = contacts.remote_node_name
              AND entry_type = 1 AND entries.deleted_at IS NULL AND entries.replied_to_name IS NOT NULL
    )
WHERE closeness < 0;
