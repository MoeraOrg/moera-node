ALTER TABLE subscriptions RENAME TO user_subscriptions;
ALTER INDEX subscriptions_pkey RENAME TO user_subscriptions_pkey;
ALTER INDEX subscriptions_node_id_subscription_type_remote_node_name_re_idx
    RENAME TO user_subscriptions_node_id_type_remote_node_idx;
ALTER INDEX subscriptions_remote_avatar_media_file_id_idx RENAME TO user_subscriptions_remote_avatar_media_file_id_idx;
ALTER TABLE user_subscriptions
    RENAME CONSTRAINT subscriptions_remote_avatar_media_file_id_fkey
    TO user_subscriptions_remote_avatar_media_file_id_fkey;

CREATE TABLE subscriptions (
    id uuid NOT NULL PRIMARY KEY,
    node_id uuid NOT NULL,
    subscription_type smallint NOT NULL,
    remote_subscriber_id varchar(40),
    remote_node_name varchar(63) NOT NULL,
    remote_feed_name varchar(63),
    remote_entry_id varchar(40),
    created_at timestamp without time zone NOT NULL,
    status smallint NOT NULL DEFAULT 0,
    retry_at timestamp without time zone,
    usage_count integer NOT NULL DEFAULT 0
);
CREATE INDEX subscriptions_node_id_type_remote_node_idx
    ON subscriptions(node_id, subscription_type, remote_node_name, remote_subscriber_id);
CREATE UNIQUE INDEX subscriptions_node_id_type_remote_node_active_idx
    ON subscriptions(node_id, subscription_type, remote_node_name, coalesce(remote_feed_name, ''),
                     coalesce(remote_entry_id, ''))
    WHERE usage_count > 0;
CREATE INDEX subscriptions_status_usage_count_idx
    ON subscriptions(status, usage_count);

INSERT INTO subscriptions(id, node_id, subscription_type, remote_subscriber_id, remote_node_name, remote_feed_name,
                          remote_entry_id, created_at)
    SELECT uuid_generate_v4() as id, node_id, subscription_type, remote_subscriber_id, remote_node_name,
           remote_feed_name, remote_entry_id, created_at
    FROM user_subscriptions;
DELETE FROM user_subscriptions WHERE subscription_type = 1 OR subscription_type = 3;
ALTER TABLE user_subscriptions DROP COLUMN remote_subscriber_id;
UPDATE subscriptions SET usage_count = 1 WHERE subscription_type = 0 OR subscription_type = 2;

UPDATE subscriptions s
SET usage_count = p.total
FROM (
    SELECT node_id, receiver_name, receiver_entry_id, count(*) AS total
    FROM entries
    WHERE entry_type = 0 AND receiver_name IS NOT NULL AND deleted_at IS NULL
    GROUP BY node_id, receiver_name, receiver_entry_id
) AS p
WHERE s.subscription_type = 1 AND s.node_id = p.node_id AND s.remote_node_name = p.receiver_name
    AND remote_entry_id = p.receiver_entry_id;

UPDATE subscriptions s
SET usage_count = usage_count + 1
FROM (
    SELECT node_id, remote_node_name
    FROM user_subscriptions
    WHERE subscription_type = 0
) AS us
WHERE s.subscription_type = 3 AND s.node_id = us.node_id AND s.remote_node_name = us.remote_node_name;

UPDATE subscriptions s
SET usage_count = usage_count + 1
FROM (
    SELECT node_id, remote_node_name
    FROM subscribers
    WHERE subscription_type = 0
) AS sr
WHERE s.subscription_type = 3 AND s.node_id = sr.node_id AND s.remote_node_name = sr.remote_node_name;

UPDATE subscriptions s
SET usage_count = usage_count + 1
FROM contacts c
WHERE s.subscription_type = 3 AND s.node_id = c.node_id AND s.remote_node_name = c.remote_node_name;

CREATE OR REPLACE FUNCTION update_subscription_reference(s_node_id uuid, s_type integer,
    old_node_name varchar(63), old_feed_name varchar(63), old_entry_id varchar(40),
    new_node_name varchar(63), new_feed_name varchar(63), new_entry_id varchar(40)) RETURNS void AS $$
    BEGIN
        IF s_node_id IS NULL
           OR s_type IS NULL
           OR (
               old_node_name = new_node_name
               AND (old_feed_name IS NULL AND new_feed_name IS NULL OR old_feed_name = new_feed_name)
               AND (old_entry_id IS NULL AND new_entry_id IS NULL OR old_entry_id = new_entry_id)
           ) THEN
            RETURN;
        END IF;
        IF old_node_name IS NOT NULL THEN
            UPDATE subscriptions
            SET usage_count = usage_count - 1
            WHERE node_id = s_node_id AND subscription_type = s_type AND remote_node_name = old_node_name
                  AND (remote_feed_name IS NULL OR remote_feed_name = old_feed_name)
                  AND (remote_entry_id IS NULL OR remote_entry_id = old_entry_id) AND usage_count > 0;
        END IF;
        IF new_node_name IS NOT NULL THEN
            INSERT INTO subscriptions AS s(id, node_id, subscription_type, remote_node_name, remote_feed_name,
                                           remote_entry_id, created_at, status, usage_count)
            VALUES (uuid_generate_v4(), s_node_id, s_type, new_node_name, new_feed_name, new_entry_id, now(), 1, 1)
            ON CONFLICT (node_id, subscription_type, remote_node_name, coalesce(remote_feed_name, ''),
                         coalesce(remote_entry_id, ''))
            WHERE usage_count > 0
            DO UPDATE
            SET usage_count = s.usage_count + 1
            WHERE s.node_id = s_node_id AND s.subscription_type = s_type AND s.remote_node_name = old_node_name
                  AND (s.remote_feed_name IS NULL OR s.remote_feed_name = old_feed_name)
                  AND (s.remote_entry_id IS NULL OR s.remote_entry_id = old_entry_id) AND s.usage_count > 0;
        END IF;
    END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION update_user_subscription_remote_node() RETURNS trigger AS $$
    BEGIN
        IF TG_OP = 'DELETE' THEN
            PERFORM update_subscription_reference(
                OLD.node_id, OLD.subscription_type, OLD.remote_node_name, OLD.remote_feed_name, OLD.remote_entry_id,
                NULL, NULL, NULL
            );
            IF OLD.subscription_type = 0 THEN
                PERFORM update_subscription_reference(
                    OLD.node_id, 3, OLD.remote_node_name, NULL, NULL, NULL, NULL, NULL
                );
            END IF;
            RETURN OLD;
        ELSIF TG_OP = 'UPDATE' THEN
            PERFORM update_subscription_reference(
                NEW.node_id, NEW.subscription_type, OLD.remote_node_name, OLD.remote_feed_name, OLD.remote_entry_id,
                NEW.remote_node_name, NEW.remote_feed_name, NEW.remote_entry_id
            );
            IF NEW.subscription_type = 0 THEN
                PERFORM update_subscription_reference(
                    NEW.node_id, 3, OLD.remote_node_name, NULL, NULL, NEW.remote_node_name, NULL, NULL
                );
            END IF;
            RETURN NEW;
        ELSIF TG_OP = 'INSERT' THEN
            PERFORM update_subscription_reference(
                NEW.node_id, NEW.subscription_type, NULL, NULL, NULL,
                NEW.remote_node_name, NEW.remote_feed_name, NEW.remote_entry_id
            );
            IF NEW.subscription_type = 0 THEN
                PERFORM update_subscription_reference(
                    NEW.node_id, 3, NULL, NULL, NULL, NEW.remote_node_name, NULL, NULL
                );
            END IF;
            RETURN NEW;
        END IF;
        RETURN NULL;
    END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER update_remote_node AFTER INSERT OR DELETE OR UPDATE ON user_subscriptions FOR EACH ROW
EXECUTE FUNCTION update_user_subscription_remote_node();

CREATE OR REPLACE FUNCTION update_subscriber_remote_node() RETURNS trigger AS $$
    BEGIN
        IF TG_OP = 'DELETE' THEN
            IF OLD.subscription_type = 0 THEN
                PERFORM update_subscription_reference(
                    OLD.node_id, 3, OLD.remote_node_name, NULL, NULL, NULL, NULL, NULL
                );
            END IF;
            RETURN OLD;
        ELSIF TG_OP = 'UPDATE' THEN
            IF NEW.subscription_type = 0 THEN
                PERFORM update_subscription_reference(
                    NEW.node_id, 3, OLD.remote_node_name, NULL, NULL, NEW.remote_node_name, NULL, NULL
                );
            END IF;
            RETURN NEW;
        ELSIF TG_OP = 'INSERT' THEN
            IF NEW.subscription_type = 0 THEN
                PERFORM update_subscription_reference(
                    NEW.node_id, 3, NULL, NULL, NULL, NEW.remote_node_name, NULL, NULL
                );
            END IF;
            RETURN NEW;
        END IF;
        RETURN NULL;
    END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER update_remote_node AFTER INSERT OR DELETE OR UPDATE OF node_id, remote_node_name
ON subscribers FOR EACH ROW
EXECUTE FUNCTION update_subscriber_remote_node();

CREATE OR REPLACE FUNCTION update_contact_remote_node() RETURNS trigger AS $$
    BEGIN
        IF TG_OP = 'DELETE' THEN
            PERFORM update_subscription_reference(
                OLD.node_id, 3, OLD.remote_node_name, NULL, NULL, NULL, NULL, NULL
            );
            RETURN OLD;
        ELSIF TG_OP = 'UPDATE' THEN
            PERFORM update_subscription_reference(
                NEW.node_id, 3, OLD.remote_node_name, NULL, NULL, NEW.remote_node_name, NULL, NULL
            );
            RETURN NEW;
        ELSIF TG_OP = 'INSERT' THEN
            PERFORM update_subscription_reference(
                NEW.node_id, 3, NULL, NULL, NULL, NEW.remote_node_name, NULL, NULL
            );
            RETURN NEW;
        END IF;
        RETURN NULL;
    END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER update_remote_node AFTER INSERT OR DELETE OR UPDATE OF node_id, remote_node_name
ON contacts FOR EACH ROW
EXECUTE FUNCTION update_contact_remote_node();

CREATE OR REPLACE FUNCTION update_entry_remote_node() RETURNS trigger AS $$
    BEGIN
        IF TG_OP = 'DELETE' THEN
            IF OLD.receiver_name IS NOT NULL AND OLD.deleted_at IS NULL THEN
                PERFORM update_subscription_reference(
                    OLD.node_id, 3, OLD.receiver_name, NULL, NULL, NULL, NULL, NULL
                );
            END IF;
            RETURN OLD;
        ELSIF TG_OP = 'UPDATE' THEN
            DECLARE
                old_receiver_name varchar(63) := CASE WHEN OLD.deleted_at IS NULL THEN OLD.receiver_name ELSE NULL END;
                new_receiver_name varchar(63) := CASE WHEN NEW.deleted_at IS NULL THEN NEW.receiver_name ELSE NULL END;
            BEGIN
                IF old_receiver_name IS NULL AND new_receiver_name IS NULL OR old_receiver_name = new_receiver_name THEN
                    RETURN NEW;
                END IF;
                IF new_receiver_name IS NULL THEN
                    PERFORM update_subscription_reference(
                        NEW.node_id, 3, old_receiver_name, NULL, NULL, NULL, NULL, NULL
                    );
                ELSIF old_receiver_name IS NULL THEN
                    PERFORM update_subscription_reference(
                        NEW.node_id, 3, NULL, NULL, NULL, new_receiver_name, NULL, NULL
                    );
                ELSE
                    PERFORM update_subscription_reference(
                        NEW.node_id, 3, old_receiver_name, NULL, NULL, new_receiver_name, NULL, NULL
                    );
                END IF;
                RETURN NEW;
            END;
        ELSIF TG_OP = 'INSERT' THEN
            IF NEW.receiver_name IS NOT NULL AND NEW.deleted_at IS NULL THEN
                PERFORM update_subscription_reference(
                    NEW.node_id, 3, NULL, NULL, NULL, NEW.receiver_name, NULL, NULL
                );
            END IF;
            RETURN NEW;
        END IF;
        RETURN NULL;
    END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER update_remote_node AFTER INSERT OR DELETE OR UPDATE OF node_id, receiver_name, deleted_at
ON entries FOR EACH ROW
EXECUTE FUNCTION update_entry_remote_node();

DROP INDEX subscribers_node_id_feed_name_idx;
CREATE INDEX subscribers_node_id_type_feed_name_idx ON subscribers(node_id, subscription_type, feed_name);
CREATE INDEX subscribers_node_id_type_entry_id_idx ON subscribers(node_id, subscription_type, entry_id);
CREATE INDEX subscribers_node_id_remote_node_name_idx ON subscribers(node_id, remote_node_name);
