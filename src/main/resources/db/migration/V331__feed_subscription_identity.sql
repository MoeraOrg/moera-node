-- Keep the source rows and their derived counts consistent throughout the Flyway transaction.
LOCK TABLE user_subscriptions, entries, subscribers, contacts, friends, friend_ofs, blocked_users,
           blocked_by_users, subscriptions IN SHARE ROW EXCLUSIVE MODE;

CREATE OR REPLACE FUNCTION update_subscription_reference(s_node_id uuid, s_type integer,
    old_node_name varchar, old_feed_name varchar, old_entry_id varchar,
    new_node_name varchar, new_feed_name varchar, new_entry_id varchar) RETURNS void AS $$
BEGIN
    IF s_type = 0 THEN
        old_entry_id := NULL;
        new_entry_id := NULL;
    END IF;
    IF s_node_id IS NULL OR s_type IS NULL OR (
        old_node_name IS NOT DISTINCT FROM new_node_name
        AND coalesce(old_feed_name, '') = coalesce(new_feed_name, '')
        AND coalesce(old_entry_id, '') = coalesce(new_entry_id, '')
    ) THEN
        RETURN;
    END IF;
    IF old_node_name IS NOT NULL THEN
        UPDATE subscriptions
        SET usage_count = usage_count - 1
        WHERE node_id = s_node_id AND subscription_type = s_type AND remote_node_name = old_node_name
          AND coalesce(remote_feed_name, '') = coalesce(old_feed_name, '')
          AND coalesce(remote_entry_id, '') = coalesce(old_entry_id, '') AND usage_count > 0;
    END IF;
    IF new_node_name IS NOT NULL THEN
        INSERT INTO subscriptions AS s(id, node_id, subscription_type, remote_node_name, remote_feed_name,
                                       remote_entry_id, created_at, status, usage_count)
        VALUES (uuid_generate_v4(), s_node_id, s_type, new_node_name, new_feed_name, new_entry_id, now(), 1, 1)
        ON CONFLICT (node_id, subscription_type, remote_node_name, coalesce(remote_feed_name, ''),
                     coalesce(remote_entry_id, '')) WHERE usage_count > 0
        DO UPDATE SET usage_count = s.usage_count + 1;
    END IF;
END;
$$ LANGUAGE plpgsql;

-- Prefer an existing restricted view to signed or public. Keep the first of equally ranked choices,
-- including incomparable groups of friends.
CREATE FUNCTION pg_temp.common_subscription_view(principals varchar[]) RETURNS varchar AS $$
    SELECT p FROM unnest(principals) WITH ORDINALITY AS principal(p, position)
    ORDER BY CASE p WHEN 'private' THEN 0 WHEN 'signed' THEN 2 WHEN 'public' THEN 3 ELSE 1 END, position
    LIMIT 1;
$$ LANGUAGE sql IMMUTABLE;

-- Keep the oldest public API ID and retain AUTO when present so the configured forced follow still applies.
CREATE TEMP TABLE duplicate_feed_subscriptions ON COMMIT DROP AS
SELECT node_id, remote_node_name, array_agg(id ORDER BY created_at, id) AS ids,
       pg_temp.common_subscription_view(array_agg(view_principal ORDER BY created_at, id)) AS view_principal,
       bool_or(reason = 3) AS auto
FROM user_subscriptions
WHERE subscription_type = 0
GROUP BY node_id, coalesce(feed_name, ''), remote_node_name, coalesce(remote_feed_name, '')
HAVING count(*) > 1;

-- Recount below instead of decrementing an already-undercounted, possibly established registration.
ALTER TABLE user_subscriptions DISABLE TRIGGER update_remote_node;
UPDATE user_subscriptions s
SET view_principal = d.view_principal, reason = CASE WHEN d.auto THEN 3 ELSE s.reason END
FROM duplicate_feed_subscriptions d
WHERE s.id = d.ids[1] AND (s.view_principal <> d.view_principal OR d.auto AND s.reason <> 3);
DELETE FROM user_subscriptions
WHERE id IN (SELECT unnest(ids[2:]) FROM duplicate_feed_subscriptions);
UPDATE user_subscriptions SET remote_entry_id = NULL WHERE subscription_type = 0 AND remote_entry_id IS NOT NULL;
ALTER TABLE user_subscriptions ENABLE TRIGGER update_remote_node;

CREATE UNIQUE INDEX user_subscriptions_feed_identity_idx
    ON user_subscriptions(node_id, coalesce(feed_name, ''), remote_node_name, coalesce(remote_feed_name, ''))
    WHERE subscription_type = 0;

WITH updated_views AS (
    SELECT c.id, pg_temp.common_subscription_view(
        array_prepend(c.view_feed_subscription_principal, array_agg(d.view_principal ORDER BY d.ids[1]))
    ) AS view_principal
    FROM duplicate_feed_subscriptions d
    JOIN contacts c USING (node_id, remote_node_name)
    WHERE c.view_feed_subscription_principal IN ('public', 'signed')
       OR (c.view_feed_subscription_principal = 'subscribed' AND d.view_principal = 'private')
    GROUP BY c.id
)
UPDATE contacts c SET view_feed_subscription_principal = v.view_principal
FROM updated_views v WHERE c.id = v.id AND c.view_feed_subscription_principal <> v.view_principal;

WITH feed_counts AS (
    SELECT c.id, count(s.id) AS total
    FROM contacts c
    LEFT JOIN user_subscriptions s ON s.node_id = c.node_id AND s.remote_node_name = c.remote_node_name
                                      AND s.subscription_type = 0
    GROUP BY c.id
)
UPDATE contacts c SET feed_subscription_count = f.total
FROM feed_counts f WHERE c.id = f.id AND c.feed_subscription_count <> f.total;

-- Mirror the update_remote_node triggers. Each live picked entry contributes a POSTING reference.
-- Incoming FEED subscribers and local social relationships also contribute PROFILE references.
CREATE TEMP TABLE subscription_repair ON COMMIT DROP AS
WITH profile_references AS (
    SELECT node_id, remote_node_name FROM user_subscriptions WHERE subscription_type = 0
    UNION ALL SELECT node_id, remote_node_name FROM subscribers WHERE subscription_type = 0
    UNION ALL SELECT node_id, remote_node_name FROM contacts
    UNION ALL SELECT node_id, remote_node_name FROM friends
    UNION ALL SELECT node_id, remote_node_name FROM friend_ofs
    UNION ALL SELECT node_id, remote_node_name FROM blocked_users
    UNION ALL SELECT node_id, remote_node_name FROM blocked_by_users
), refs AS (
    SELECT node_id, subscription_type, remote_node_name, remote_feed_name, remote_entry_id FROM user_subscriptions
    UNION ALL SELECT node_id, 1, receiver_name, NULL, receiver_entry_id FROM entries
        WHERE receiver_name IS NOT NULL AND deleted_at IS NULL
    UNION ALL SELECT node_id, 3, remote_node_name, NULL, NULL FROM profile_references
), counts AS (
    SELECT node_id, subscription_type, remote_node_name, coalesce(remote_feed_name, '') AS feed_key,
           coalesce(remote_entry_id, '') AS entry_key, count(*)::integer AS total
    FROM refs GROUP BY node_id, subscription_type, remote_node_name, feed_key, entry_key
), active AS (
    -- Several legacy FEED rows can differ only by an irrelevant posting ID. Prefer a working registration.
    SELECT id, node_id, subscription_type, remote_node_name, coalesce(remote_feed_name, '') AS feed_key,
           CASE WHEN subscription_type = 0 THEN '' ELSE coalesce(remote_entry_id, '') END AS entry_key,
           status = 0 AND remote_subscriber_id IS NOT NULL AS registered, remote_entry_id IS NULL AS no_entry, created_at
    FROM subscriptions WHERE usage_count > 0
)
SELECT DISTINCT ON (node_id, subscription_type, remote_node_name, feed_key, entry_key) counts.*, active.id
FROM counts LEFT JOIN active USING (node_id, subscription_type, remote_node_name, feed_key, entry_key)
ORDER BY node_id, subscription_type, remote_node_name, feed_key, entry_key,
         active.registered DESC, active.no_entry DESC, active.created_at, active.id;

-- Release obsolete keys first so the chosen FEED row can drop its extraneous posting ID under the active index.
UPDATE subscriptions s SET usage_count = 0, retry_at = NULL
WHERE usage_count > 0 AND NOT EXISTS (SELECT FROM subscription_repair r WHERE r.id = s.id);

UPDATE subscriptions s
SET usage_count = r.total, remote_entry_id = CASE WHEN s.subscription_type = 0 THEN NULL ELSE s.remote_entry_id END
FROM subscription_repair r
WHERE s.id = r.id AND (s.usage_count <> r.total OR s.subscription_type = 0 AND s.remote_entry_id IS NOT NULL);

-- A zero-count row may already have been unsubscribed remotely. Start a fresh PENDING registration
-- if no active row covers a surviving reference.
INSERT INTO subscriptions(id, node_id, subscription_type, remote_node_name, remote_feed_name, remote_entry_id,
                          created_at, status, usage_count)
SELECT uuid_generate_v4(), node_id, subscription_type, remote_node_name, nullif(feed_key, ''), nullif(entry_key, ''),
       now(), 1, total FROM subscription_repair WHERE id IS NULL;

UPDATE subscriptions SET status = 1, remote_subscriber_id = NULL, retry_at = NULL, created_at = now()
WHERE usage_count > 0 AND status = 0 AND remote_subscriber_id IS NULL;

-- Remote subscription creation is idempotent. An obsolete unsubscribe for a referenced key must
-- never delete the live ID or race a PENDING registration which might receive that same ID.
-- Unreferenced, distinct registrations stay at zero for SubscriptionManager to unsubscribe.
DELETE FROM subscriptions stale
USING subscriptions live
WHERE stale.usage_count = 0 AND live.usage_count > 0 AND stale.node_id = live.node_id
  AND stale.remote_node_name = live.remote_node_name
  AND (stale.remote_subscriber_id = live.remote_subscriber_id OR (
      live.status = 1 AND stale.subscription_type = live.subscription_type
      AND coalesce(stale.remote_feed_name, '') = coalesce(live.remote_feed_name, '')
      AND (live.subscription_type = 0 OR coalesce(stale.remote_entry_id, '') = coalesce(live.remote_entry_id, ''))
  ));
