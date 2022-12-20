ALTER TABLE contacts ADD COLUMN view_feed_subscription_principal varchar(70) NOT NULL DEFAULT 'public';
ALTER TABLE contacts ADD COLUMN view_feed_subscriber_principal varchar(70) NOT NULL DEFAULT 'public';
ALTER TABLE contacts ADD COLUMN view_friend_principal varchar(70) NOT NULL DEFAULT 'public';

UPDATE contacts
SET view_feed_subscription_principal = COALESCE((
    SELECT view_principal
    FROM user_subscriptions
    WHERE user_subscriptions.contact_id = contacts.id AND subscription_type = 0
    LIMIT 1
), 'public'),
view_feed_subscriber_principal = COALESCE((
    SELECT COALESCE(NULLIF(admin_view_principal, 'unset'), view_principal)
    FROM subscribers
    WHERE subscribers.contact_id = contacts.id AND subscription_type = 0
    LIMIT 1
), 'public'),
view_friend_principal = COALESCE((
    SELECT view_principal
    FROM friends
    WHERE friends.contact_id = contacts.id
    LIMIT 1
), 'public');
