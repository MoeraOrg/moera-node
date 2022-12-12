ALTER TABLE subscribers ADD COLUMN contact_id uuid;
CREATE INDEX subscribers_contact_id_idx ON subscribers(contact_id);
ALTER TABLE subscribers ADD FOREIGN KEY (contact_id) REFERENCES contacts(id)
    ON UPDATE CASCADE ON DELETE SET NULL;

ALTER TABLE user_subscriptions ADD COLUMN contact_id uuid;
CREATE INDEX user_subscriptions_contact_id_idx ON user_subscriptions(contact_id);
ALTER TABLE user_subscriptions ADD FOREIGN KEY (contact_id) REFERENCES contacts(id)
    ON UPDATE CASCADE ON DELETE SET NULL;

ALTER TABLE friends ADD COLUMN contact_id uuid;
CREATE INDEX friends_contact_id_idx ON friends(contact_id);
ALTER TABLE friends ADD FOREIGN KEY (contact_id) REFERENCES contacts(id)
    ON UPDATE CASCADE ON DELETE SET NULL;

ALTER TABLE friend_ofs ADD COLUMN contact_id uuid;
CREATE INDEX friend_ofs_contact_id_idx ON friend_ofs(contact_id);
ALTER TABLE friend_ofs ADD FOREIGN KEY (contact_id) REFERENCES contacts(id)
    ON UPDATE CASCADE ON DELETE SET NULL;

UPDATE subscribers
SET contact_id = (
    SELECT id
    FROM contacts
    WHERE contacts.node_id = subscribers.node_id AND contacts.remote_node_name = subscribers.remote_node_name
);

UPDATE user_subscriptions
SET contact_id = (
    SELECT id
    FROM contacts
    WHERE contacts.node_id = user_subscriptions.node_id
          AND contacts.remote_node_name = user_subscriptions.remote_node_name
);

UPDATE friends
SET contact_id = (
    SELECT id
    FROM contacts
    WHERE contacts.node_id = friends.node_id AND contacts.remote_node_name = friends.remote_node_name
);

UPDATE friend_ofs
SET contact_id = (
    SELECT id
    FROM contacts
    WHERE contacts.node_id = friend_ofs.node_id AND contacts.remote_node_name = friend_ofs.remote_node_name
);
