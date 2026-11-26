LOCK TABLE user_subscriptions, subscribers, contacts, friends, friend_ofs, blocked_users, blocked_by_users,
           subscriptions, own_comments, remote_connectivity, remote_user_list_items IN SHARE ROW EXCLUSIVE MODE;

CREATE TEMP TABLE contact_aliases ON COMMIT DROP AS
SELECT short.id AS short_id, expanded.id AS expanded_id, short.node_id,
       short.remote_node_name AS short_name, expanded.remote_node_name AS expanded_name
FROM contacts short
JOIN contacts expanded ON expanded.node_id = short.node_id
                      AND expanded.remote_node_name = short.remote_node_name || '_0'
WHERE short.remote_node_name <> '' AND strpos(short.remote_node_name, '_') = 0;

-- Keep the contact with the expanded name and preserve links from both contact rows.
UPDATE user_subscriptions SET contact_id = expanded_id FROM contact_aliases WHERE contact_id = short_id;
UPDATE subscribers SET contact_id = expanded_id FROM contact_aliases WHERE contact_id = short_id;
UPDATE friends SET contact_id = expanded_id FROM contact_aliases WHERE contact_id = short_id;
UPDATE friend_ofs SET contact_id = expanded_id FROM contact_aliases WHERE contact_id = short_id;
UPDATE blocked_users SET contact_id = expanded_id FROM contact_aliases WHERE contact_id = short_id;
UPDATE blocked_by_users SET contact_id = expanded_id FROM contact_aliases WHERE contact_id = short_id;

-- Conflicting restrictions that cannot be ordered safely reduce visibility to private.
CREATE FUNCTION pg_temp.merge_contact_view(first_view varchar, second_view varchar) RETURNS varchar AS $$
    SELECT CASE
        WHEN first_view = second_view OR second_view = 'public' THEN first_view
        WHEN first_view = 'public' THEN second_view
        WHEN first_view = 'private' OR second_view = 'private' THEN 'private'
        WHEN first_view = 'signed' THEN second_view
        WHEN second_view = 'signed' THEN first_view
        ELSE 'private'
    END;
$$ LANGUAGE sql IMMUTABLE;

UPDATE contacts expanded
SET created_at = least(expanded.created_at, short.created_at),
    updated_at = greatest(expanded.updated_at, short.updated_at),
    remote_full_name = coalesce(expanded.remote_full_name, short.remote_full_name),
    remote_avatar_media_file_id = coalesce(expanded.remote_avatar_media_file_id, short.remote_avatar_media_file_id),
    remote_avatar_shape = CASE WHEN expanded.remote_avatar_media_file_id IS NULL THEN short.remote_avatar_shape
                               ELSE expanded.remote_avatar_shape END,
    remote_gender = coalesce(expanded.remote_gender, short.remote_gender),
    remote_title = coalesce(expanded.remote_title, short.remote_title),
    remote_source_uri = coalesce(expanded.remote_source_uri, short.remote_source_uri),
    feed_subscription_count = expanded.feed_subscription_count + short.feed_subscription_count,
    feed_subscriber_count = expanded.feed_subscriber_count + short.feed_subscriber_count,
    friend_count = expanded.friend_count + short.friend_count,
    friend_of_count = expanded.friend_of_count + short.friend_of_count,
    blocked_user_count = expanded.blocked_user_count + short.blocked_user_count,
    blocked_user_posting_count = expanded.blocked_user_posting_count + short.blocked_user_posting_count,
    blocked_by_user_count = expanded.blocked_by_user_count + short.blocked_by_user_count,
    blocked_by_user_posting_count = expanded.blocked_by_user_posting_count + short.blocked_by_user_posting_count,
    visit_count = expanded.visit_count + short.visit_count,
    distance = least(expanded.distance, short.distance),
    view_feed_subscription_principal = pg_temp.merge_contact_view(
        expanded.view_feed_subscription_principal, short.view_feed_subscription_principal
    ),
    view_feed_subscriber_principal = pg_temp.merge_contact_view(
        expanded.view_feed_subscriber_principal, short.view_feed_subscriber_principal
    ),
    view_friend_principal = pg_temp.merge_contact_view(expanded.view_friend_principal, short.view_friend_principal)
FROM contact_aliases aliases
JOIN contacts short ON short.id = aliases.short_id
WHERE expanded.id = aliases.expanded_id;

-- Handle PROFILE reference changes after reconciling the remote registrations.
ALTER TABLE contacts DISABLE TRIGGER update_remote_node;
DELETE FROM contacts USING contact_aliases WHERE id = short_id;
UPDATE contacts SET remote_node_name = remote_node_name || '_0'
WHERE remote_node_name <> '' AND strpos(remote_node_name, '_') = 0;
ALTER TABLE contacts ENABLE TRIGGER update_remote_node;

-- A comment is identified by the remote node, posting and comment IDs.
DELETE FROM own_comments short USING own_comments expanded
WHERE short.remote_node_name <> '' AND strpos(short.remote_node_name, '_') = 0
  AND expanded.remote_node_name = short.remote_node_name || '_0'
  AND expanded.node_id = short.node_id
  AND expanded.remote_posting_id = short.remote_posting_id
  AND expanded.remote_comment_id = short.remote_comment_id;

UPDATE own_comments SET remote_node_name = remote_node_name || '_0'
WHERE remote_node_name <> '' AND strpos(remote_node_name, '_') = 0;

-- Keep the newest observation when both spellings have a cache entry.
DELETE FROM remote_connectivity
WHERE id IN (
    SELECT CASE WHEN short.updated_at <= expanded.updated_at THEN short.id ELSE expanded.id END
    FROM remote_connectivity short
    JOIN remote_connectivity expanded ON expanded.remote_node_name = short.remote_node_name || '_0'
    WHERE short.remote_node_name <> '' AND strpos(short.remote_node_name, '_') = 0
);

UPDATE remote_connectivity SET remote_node_name = remote_node_name || '_0'
WHERE remote_node_name <> '' AND strpos(remote_node_name, '_') = 0;

DELETE FROM remote_user_list_items
WHERE id IN (
    SELECT CASE WHEN short.cached_at <= expanded.cached_at THEN short.id ELSE expanded.id END
    FROM remote_user_list_items short
    JOIN remote_user_list_items expanded ON expanded.node_id = short.node_id
                                        AND expanded.list_node_name = short.list_node_name
                                        AND expanded.list_name = short.list_name
                                        AND expanded.node_name = short.node_name || '_0'
    WHERE short.node_name <> '' AND strpos(short.node_name, '_') = 0
);

UPDATE remote_user_list_items SET node_name = node_name || '_0'
WHERE node_name <> '' AND strpos(node_name, '_') = 0;

CREATE TEMP TABLE subscription_aliases ON COMMIT DROP AS
SELECT DISTINCT node_id, remote_node_name AS short_name, remote_node_name || '_0' AS expanded_name
FROM subscriptions
WHERE remote_node_name <> '' AND strpos(remote_node_name, '_') = 0;

CREATE TEMP TABLE duplicate_profile_references ON COMMIT DROP AS
SELECT a.node_id, a.expanded_name
FROM contact_aliases a
JOIN subscriptions s ON s.node_id = a.node_id AND s.remote_node_name = a.short_name
                    AND s.subscription_type = 3 AND s.usage_count > 0
                    AND coalesce(s.remote_feed_name, '') = '' AND coalesce(s.remote_entry_id, '') = '';

-- Preserve an established registration and carry forward both sets of local references.
CREATE TEMP TABLE combined_subscriptions ON COMMIT DROP AS
SELECT array_agg(s.id ORDER BY (s.status = 0 AND s.remote_subscriber_id IS NOT NULL) DESC,
                               (s.remote_node_name = a.expanded_name) DESC, s.created_at, s.id) AS ids,
       sum(s.usage_count)::integer AS total
FROM subscriptions s
JOIN subscription_aliases a ON a.node_id = s.node_id AND s.remote_node_name IN (a.short_name, a.expanded_name)
WHERE s.usage_count > 0
GROUP BY s.node_id, a.expanded_name, s.subscription_type, coalesce(s.remote_feed_name, ''),
         coalesce(s.remote_entry_id, '')
HAVING count(*) > 1;

-- Distinct remote IDs remain eligible for the usual unsubscribe workflow.
UPDATE subscriptions SET usage_count = 0, retry_at = NULL
WHERE id IN (SELECT unnest(ids[2:]) FROM combined_subscriptions);

UPDATE subscriptions s SET usage_count = c.total
FROM combined_subscriptions c WHERE s.id = c.ids[1];

UPDATE subscriptions s SET remote_node_name = a.expanded_name
FROM subscription_aliases a WHERE s.node_id = a.node_id AND s.remote_node_name = a.short_name;

-- Each deleted contact removes one PROFILE reference from the surviving registration.
UPDATE subscriptions s SET usage_count = s.usage_count - 1
FROM duplicate_profile_references d
WHERE s.node_id = d.node_id AND s.remote_node_name = d.expanded_name
  AND s.subscription_type = 3 AND s.usage_count > 0
  AND coalesce(s.remote_feed_name, '') = '' AND coalesce(s.remote_entry_id, '') = '';

-- A stale row must not unsubscribe a remote ID still used by a live row.
DELETE FROM subscriptions stale
USING subscriptions live, subscription_aliases a
WHERE stale.usage_count = 0 AND live.usage_count > 0
  AND stale.node_id = a.node_id AND stale.remote_node_name = a.expanded_name
  AND live.node_id = stale.node_id AND live.remote_node_name = stale.remote_node_name
  AND (live.remote_subscriber_id = stale.remote_subscriber_id OR (
      live.status = 1 AND stale.subscription_type = live.subscription_type
      AND coalesce(stale.remote_feed_name, '') = coalesce(live.remote_feed_name, '')
      AND coalesce(stale.remote_entry_id, '') = coalesce(live.remote_entry_id, '')
  ));
