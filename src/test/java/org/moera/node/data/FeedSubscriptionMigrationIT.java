package org.moera.node.data;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Uses an empty test database, or creates a disposable one when the supplied database is already migrated. */
@EnabledIfSystemProperty(named = "moera.test.database", matches = ".+")
class FeedSubscriptionMigrationIT {

    private static final UUID NODE_ID = UUID.randomUUID();
    private static final UUID CONTACT_ID = UUID.randomUUID();
    private static final String SOURCE = "source_0";

    private static JdbcTemplate jdbc;
    private static JdbcTemplate catalog;
    private static String temporaryDatabase;
    private static UUID originalSubscriptionId;
    private static UUID establishedRegistrationId;

    @BeforeAll
    static void upgradeDatabase() {
        var source = testDataSource();
        catalog = new JdbcTemplate(source);
        if (Boolean.TRUE.equals(catalog.queryForObject("SELECT to_regclass('flyway_schema_history') IS NOT NULL",
                Boolean.class))) {
            String name = "feed_subscription_it_" + UUID.randomUUID().toString().replace("-", "");
            catalog.execute("CREATE DATABASE " + name);
            temporaryDatabase = name;
            source = testDataSource();
            source.setDatabaseName(name);
        }
        jdbc = new JdbcTemplate(source);
        jdbc.execute("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\"");
        Flyway.configure().dataSource(source).target("330").load().migrate();

        jdbc.update("""
            INSERT INTO contacts (id, node_id, remote_node_name, created_at, updated_at, feed_subscription_count,
                                  view_feed_subscription_principal)
            VALUES (?, ?, ?, now(), now(), 3, 'subscribed')
            """, CONTACT_ID, NODE_ID, SOURCE);
        originalSubscriptionId = addFeed(SOURCE, Feed.NEWS, Feed.TIMELINE, "public");
        jdbc.update(
            "UPDATE user_subscriptions SET created_at = now() - interval '1 day' WHERE id = ?",
            originalSubscriptionId
        );
        UUID duplicate = addFeed(SOURCE, Feed.NEWS, Feed.TIMELINE, "private");
        jdbc.update("UPDATE user_subscriptions SET reason = 3 WHERE id = ?", duplicate);
        addFeed(SOURCE, Feed.EXPLORE, Feed.TIMELINE, "public");
        jdbc.update("""
            INSERT INTO subscribers (id, node_id, remote_node_name, subscription_type, feed_name, created_at)
            VALUES (?, ?, ?, 0, 'timeline', now())
            """, UUID.randomUUID(), NODE_ID, SOURCE);
        establishedRegistrationId = jdbc.queryForObject("""
            UPDATE subscriptions SET status = 0, remote_subscriber_id = 'established'
            WHERE node_id = ? AND remote_node_name = ? AND subscription_type = 0 RETURNING id
            """, UUID.class, NODE_ID, SOURCE);

        addFeed("missing_0", Feed.NEWS, Feed.TIMELINE, "public");
        jdbc.update("DELETE FROM subscriptions WHERE node_id = ? AND remote_node_name = 'missing_0'", NODE_ID);
        addFeed("retired_0", Feed.NEWS, Feed.TIMELINE, "public");
        jdbc.update("""
            UPDATE subscriptions SET usage_count = 0, status = 0, remote_subscriber_id = 'stale'
            WHERE node_id = ? AND remote_node_name = 'retired_0' AND subscription_type = 0
            """, NODE_ID);

        UUID legacy = addFeed("legacy_0", Feed.NEWS, Feed.TIMELINE, "public");
        jdbc.update("UPDATE user_subscriptions SET remote_entry_id = 'irrelevant-posting' WHERE id = ?", legacy);
        jdbc.update("""
            UPDATE subscriptions SET status = 0, remote_subscriber_id = 'working'
            WHERE node_id = ? AND remote_node_name = 'legacy_0' AND remote_entry_id = 'irrelevant-posting'
            """, NODE_ID);
        addFeed("legacy_0", Feed.EXPLORE, Feed.TIMELINE, "public");

        Flyway.configure().dataSource(source).target("331").load().migrate();
    }

    private static PGSimpleDataSource testDataSource() {
        var source = new PGSimpleDataSource();
        source.setURL(System.getProperty("moera.test.database"));
        source.setUser(System.getProperty("user.name"));
        return source;
    }

    @AfterAll
    static void removeTemporaryDatabase() {
        if (temporaryDatabase != null) {
            catalog.execute("DROP DATABASE " + temporaryDatabase);
        }
    }

    @Test
    void mergesDuplicateDestinationsAndKeepsTheWorkingRemoteRegistration() {
        Map<String, Object> news = jdbc.queryForMap("""
            SELECT id, view_principal, reason::integer FROM user_subscriptions
            WHERE node_id = ? AND remote_node_name = ? AND feed_name = 'news'
            """, NODE_ID, SOURCE);
        assertEquals(originalSubscriptionId, news.get("id"));
        assertEquals("private", news.get("view_principal"));
        assertEquals(3, news.get("reason"));
        Map<String, Object> contact = jdbc.queryForMap(
            "SELECT feed_subscription_count, view_feed_subscription_principal FROM contacts WHERE id = ?", CONTACT_ID
        );
        assertEquals(2, contact.get("feed_subscription_count"));
        assertEquals("private", contact.get("view_feed_subscription_principal"));

        Map<String, Object> feed = registration(SOURCE, 0);
        assertEquals(establishedRegistrationId, feed.get("id"));
        assertEquals("established", feed.get("remote_subscriber_id"));
        assertEquals(0, feed.get("status"));
        assertEquals(2, feed.get("usage_count"));
        // Both destination feeds, the contact and the incoming subscriber refer to the same remote profile.
        assertEquals(4, registration(SOURCE, 3).get("usage_count"));
    }

    @Test
    void restoresMissingAndRetiredRegistrationsThroughPendingWork() {
        for (String remote : List.of("missing_0", "retired_0")) {
            Map<String, Object> feed = registration(remote, 0);
            assertEquals(1, feed.get("status"));
            assertEquals(1, feed.get("usage_count"));
            assertNull(feed.get("remote_subscriber_id"));
            assertNull(feed.get("retry_at"));
        }
    }

    @Test
    void keepsTheEstablishedFeedRegistrationWhenLegacyRowsIncludeAPostingId() {
        Map<String, Object> registration = jdbc.queryForMap("""
            SELECT remote_subscriber_id, remote_entry_id, usage_count FROM subscriptions
            WHERE node_id = ? AND remote_node_name = 'legacy_0' AND subscription_type = 0 AND usage_count > 0
            """, NODE_ID);
        assertEquals("working", registration.get("remote_subscriber_id"));
        assertNull(registration.get("remote_entry_id"));
        assertEquals(2, registration.get("usage_count"));
    }

    @Test
    void onlyTheLastDestinationReleasesTheRemoteFeed() {
        String remote = "shared_0";
        UUID news = addFeed(remote, Feed.NEWS, Feed.TIMELINE, "public");
        assertThrows(DuplicateKeyException.class, () -> addFeed(remote, Feed.NEWS, Feed.TIMELINE, "public"));
        UUID explore = addFeed(remote, Feed.EXPLORE, Feed.TIMELINE, "public");
        assertEquals(2, registration(remote, 0).get("usage_count"));
        jdbc.update("DELETE FROM user_subscriptions WHERE id = ?", news);
        assertEquals(1, registration(remote, 0).get("usage_count"));
        jdbc.update("DELETE FROM user_subscriptions WHERE id = ?", explore);
        assertEquals(0, registration(remote, 0).get("usage_count"));
    }

    @Test
    void nullableFeedNamesUseTheSameIdentityAsTheRegistration() {
        addFeed("nullable_0", Feed.NEWS, null, "public");
        assertThrows(DuplicateKeyException.class, () -> addFeed("nullable_0", Feed.NEWS, null, "public"));
        assertThrows(DuplicateKeyException.class, () -> addFeed("nullable_0", Feed.NEWS, "", "public"));
    }

    private static UUID addFeed(String remote, String destination, String remoteFeed, String visibility) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
            INSERT INTO user_subscriptions (id, node_id, subscription_type, feed_name, remote_node_name,
                                            remote_feed_name, created_at, view_principal, contact_id)
            VALUES (?, ?, 0, ?, ?, ?, now(), ?, (SELECT id FROM contacts WHERE node_id = ? AND remote_node_name = ?))
            """, id, NODE_ID, destination, remote, remoteFeed, visibility, NODE_ID, remote);
        return id;
    }

    private static Map<String, Object> registration(String remote, int type) {
        return jdbc.queryForMap("""
            SELECT id, remote_subscriber_id, status::integer, retry_at, usage_count FROM subscriptions
            WHERE node_id = ? AND remote_node_name = ? AND subscription_type = ?
            """, NODE_ID, remote, type);
    }

}
