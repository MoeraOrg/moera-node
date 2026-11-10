package org.moera.node.data;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.moera.lib.node.types.StoryInfo;
import org.moera.lib.node.types.StoryType;
import org.moera.lib.node.types.principal.Principal;
import org.moera.node.global.RequestContext;
import org.moera.node.model.ObjectNotFoundFailure;
import org.moera.node.operations.EntryOperations;
import org.moera.node.operations.PostingInfoOperations;
import org.moera.node.operations.MediaAttachmentsProvider;
import org.moera.node.rest.FeedController;
import org.moera.node.rest.StoryController;
import org.moera.node.userlist.SheriffUserListOperations;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.SharedEntityManagerCreator;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@EnabledIfSystemProperty(named = "moera.test.database", matches = ".+")
class StoryVisibilityIT {

    private static LocalContainerEntityManagerFactoryBean factoryBean;
    private static EntityManagerFactory factory;

    private EntityManager entityManager;
    private TransactionTemplate transaction;
    private StoryRepository storyRepository;
    private RequestContext requestContext;
    private FeedController feedController;
    private StoryController storyController;
    private UUID nodeId;

    @BeforeAll
    static void openDatabase() {
        var source = new DriverManagerDataSource(
            System.getProperty("moera.test.database"), System.getProperty("user.name"), ""
        );
        new JdbcTemplate(source).execute("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\"");
        Flyway.configure().dataSource(source).load().migrate();
        factoryBean = new LocalContainerEntityManagerFactoryBean();
        factoryBean.setDataSource(source);
        factoryBean.setPackagesToScan("org.moera.node.data", "org.moera.node.model.principal");
        factoryBean.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        factoryBean.setJpaPropertyMap(Map.of(
            "hibernate.physical_naming_strategy", "org.hibernate.boot.model.naming.PhysicalNamingStrategySnakeCaseImpl"
        ));
        factoryBean.afterPropertiesSet();
        factory = factoryBean.getObject();
    }

    @AfterAll
    static void closeDatabase() {
        if (factoryBean != null) {
            factoryBean.destroy();
        }
    }

    @BeforeEach
    void setUp() {
        nodeId = UUID.randomUUID();
        entityManager = SharedEntityManagerCreator.createSharedEntityManager(factory);
        transaction = new TransactionTemplate(new JpaTransactionManager(factory));
        var repositories = new JpaRepositoryFactory(entityManager);
        storyRepository = repositories.getRepository(StoryRepository.class);
        requestContext = mock(RequestContext.class);
        when(requestContext.nodeId()).thenReturn(nodeId);
        var postingInfoOperations = mock(PostingInfoOperations.class);
        when(postingInfoOperations.withLinkedPostings(any())).thenAnswer(invocation -> invocation.getArgument(0));
        var entryOperations = mock(EntryOperations.class);
        when(entryOperations.prepareMediaAttachments(any())).thenReturn(MediaAttachmentsProvider.NONE);
        feedController = new FeedController();
        ReflectionTestUtils.setField(feedController, "requestContext", requestContext);
        ReflectionTestUtils.setField(feedController, "entityManager", entityManager);
        ReflectionTestUtils.setField(feedController, "storyRepository", storyRepository);
        ReflectionTestUtils.setField(feedController, "entryOperations", entryOperations);
        ReflectionTestUtils.setField(feedController, "postingInfoOperations", postingInfoOperations);
        ReflectionTestUtils.setField(feedController, "reactionRepository", mock(ReactionRepository.class));
        ReflectionTestUtils.setField(
            feedController, "blockedUserOperations", mock(org.moera.node.operations.BlockedUserOperations.class)
        );
        ReflectionTestUtils.setField(
            feedController, "sheriffUserListOperations", mock(SheriffUserListOperations.class)
        );
        storyController = new StoryController();
        ReflectionTestUtils.setField(storyController, "requestContext", requestContext);
        ReflectionTestUtils.setField(storyController, "storyRepository", storyRepository);
        ReflectionTestUtils.setField(storyController, "entryOperations", entryOperations);
        ReflectionTestUtils.setField(storyController, "postingInfoOperations", postingInfoOperations);
    }

    @ParameterizedTest
    @CsvSource({
        "unset, public, , false, false, , true, true",
        "unset, private, , false, false, , false, false",
        "unset, private, author_0, false, false, , true, true",
        "public, private, , false, false, , true, false",
        "public, private, author_0, false, false, , true, true",
        "private, public, author_0, false, false, , false, false",
        "admin, public, , false, false, , false, false",
        "signed, public, , false, false, , false, false",
        "signed, admin, reader_0, false, false, , true, false",
        "subscribed, public, reader_0, false, false, , false, false",
        "subscribed, admin, reader_0, false, true, , true, false",
        "f:friends, public, reader_0, false, false, others, false, false",
        "f:friends, admin, reader_0, false, false, friends, true, false",
        "private, private, , true, false, , true, true",
        "admin, public, , true, false, , true, true"
    })
    void feedAndDirectReadRespectIndependentPermissions(
        String storyView,
        String postingView,
        String clientName,
        boolean admin,
        boolean subscribed,
        String group,
        boolean visible,
        boolean postingVisible
    ) {
        client(clientName, admin, subscribed, group);
        transaction.executeWithoutResult(status -> {
            status.setRollbackOnly();
            Story story = story(posting(true, postingView), Feed.TIMELINE, StoryType.POSTING_ADDED, storyView, 1);
            entityManager.flush();
            entityManager.clear();

            var slice = feedController.getStories(Feed.TIMELINE, null, null, 10);

            assertEquals(visible ? 1 : 0, slice.getStories().size());
            if (visible) {
                for (StoryInfo info : List.of(
                    slice.getStories().getFirst(), storyController.get(story.getId().toString())
                )) {
                    assertEquals(postingVisible, info.getPosting() != null);
                    assertEquals("remote_0", info.getRemoteNodeName());
                    assertEquals("remote-posting", info.getRemotePostingId());
                    assertEquals(new Principal(storyView), info.getOperations().getView());
                }
            } else {
                assertThrows(ObjectNotFoundFailure.class, () -> storyController.get(story.getId().toString()));
            }
        });
    }

    @Test
    void explicitPermissionsWorkWithoutAnEntryAndAcrossPageBoundaries() {
        client(null, false, false, null);
        transaction.executeWithoutResult(status -> {
            status.setRollbackOnly();
            Story first = story(posting(true, "admin"), Feed.TIMELINE, StoryType.POSTING_ADDED, "public", 1);
            Story second = story(null, Feed.TIMELINE, StoryType.FRIEND_ADDED, "public", 2);
            Story hidden = story(null, Feed.TIMELINE, StoryType.FRIEND_ADDED, "admin", 3);
            entityManager.flush();
            entityManager.clear();

            var newest = feedController.getStories(Feed.TIMELINE, null, null, 1);
            assertEquals(
                List.of(second.getId().toString()), newest.getStories().stream().map(StoryInfo::getId).toList()
            );
            var older = feedController.getStories(Feed.TIMELINE, newest.getAfter(), null, 1);
            assertEquals(List.of(first.getId().toString()), older.getStories().stream().map(StoryInfo::getId).toList());
            var ascending = feedController.getStories(Feed.TIMELINE, null, 0L, 1);
            assertEquals(
                List.of(first.getId().toString()), ascending.getStories().stream().map(StoryInfo::getId).toList()
            );
            assertNotNull(storyController.get(second.getId().toString()));
            assertThrows(ObjectNotFoundFailure.class, () -> storyController.get(hidden.getId().toString()));
        });
    }

    @Test
    void backfillCopiesOnlyTimelinePermissionsAndAllCopiedPostingIdentities() {
        transaction.executeWithoutResult(status -> {
            status.setRollbackOnly();
            Posting copy = posting(true, "signed");
            Story timeline = story(copy, Feed.TIMELINE, StoryType.POSTING_ADDED, "unset", 1);
            Story news = story(copy, Feed.NEWS, StoryType.POSTING_ADDED, "unset", 2);
            Story original = story(posting(false, "private"), Feed.TIMELINE, StoryType.POSTING_ADDED, "unset", 3);
            Story otherType = story(copy, Feed.TIMELINE, StoryType.POSTING_UPDATED, "unset", 4);
            Story noEntry = story(null, Feed.TIMELINE, StoryType.FRIEND_ADDED, "unset", 5);
            Comment comment = new Comment();
            comment.setId(UUID.randomUUID());
            comment.setNodeId(nodeId);
            comment.setParent(copy);
            comment.setReceiverName("comment-node");
            comment.setReceiverEntryId("comment-id");
            comment.setViewPrincipal(Principal.SIGNED);
            entityManager.persist(comment);
            Story commentStory = story(comment, Feed.TIMELINE, StoryType.POSTING_ADDED, "unset", 6);
            for (Story story : List.of(timeline, news, original, otherType, noEntry, commentStory)) {
                story.setRemoteNodeName(null);
                story.setRemotePostingId(null);
            }
            entityManager.flush();
            entityManager.unwrap(org.hibernate.Session.class).doWork(connection -> ScriptUtils.executeSqlScript(
                connection,
                new ClassPathResource("db/migration/V327__backfill_story_permissions_and_remote_postings.sql")
            ));
            entityManager.refresh(timeline);
            assertEquals(Principal.SIGNED, timeline.getViewPrincipal());
            for (Story story : List.of(news, original, otherType, noEntry, commentStory)) {
                entityManager.refresh(story);
                assertEquals(Principal.UNSET, story.getViewPrincipal());
            }
            for (Story story : List.of(timeline, news)) {
                assertEquals("remote_0", story.getRemoteNodeName());
                assertEquals("remote-posting", story.getRemotePostingId());
            }
            for (Story story : List.of(original, otherType, noEntry, commentStory)) {
                assertNull(story.getRemoteNodeName());
                assertNull(story.getRemotePostingId());
            }
        });
    }

    @Test
    void pendingPickRetainsStoryPermissionInDatabase() {
        transaction.executeWithoutResult(status -> {
            status.setRollbackOnly();
            Pick pick = new Pick();
            pick.setId(UUID.randomUUID());
            pick.setNodeId(nodeId);
            pick.setRemoteNodeName("remote_0");
            pick.setRemotePostingId("remote-posting");
            pick.setStoryViewPrincipal(Principal.SIGNED);
            entityManager.persist(pick);
            entityManager.flush();
            entityManager.clear();

            Pick stored = entityManager.find(Pick.class, pick.getId());

            assertEquals(Principal.SIGNED, stored.getStoryViewPrincipal());
        });
    }

    private void client(String name, boolean admin, boolean subscribed, String group) {
        String[] groups = group != null ? new String[]{group} : null;
        when(requestContext.isAdmin(any())).thenReturn(admin);
        when(requestContext.getClientName(any())).thenReturn(name);
        when(requestContext.isSubscribedToClient(any())).thenReturn(subscribed);
        when(requestContext.getFriendGroups(any())).thenReturn(groups);
        when(requestContext.isPrincipal(any(), any())).thenAnswer(invocation ->
            ((org.moera.lib.node.types.principal.PrincipalFilter) invocation.getArgument(0))
                .includes(admin, name, subscribed, groups)
        );
    }

    private Posting posting(boolean remote, String view) {
        Posting posting = new Posting();
        posting.setId(UUID.randomUUID());
        posting.setNodeId(nodeId);
        posting.setOwnerName("author_0");
        posting.setViewPrincipal(new Principal(view));
        if (remote) {
            posting.setReceiverName("remote_0");
            posting.setReceiverEntryId("remote-posting");
        }
        entityManager.persist(posting);
        EntryRevision revision = new EntryRevision();
        revision.setId(UUID.randomUUID());
        revision.setEntry(posting);
        revision.setBody("{\"text\":\"posting\"}");
        revision.setBodyPreview("{\"text\":\"posting\"}");
        entityManager.persist(revision);
        posting.setCurrentRevision(revision);
        return posting;
    }

    private Story story(Entry entry, String feed, StoryType type, String view, long moment) {
        Story story = new Story(UUID.randomUUID(), nodeId, type);
        story.setEntry(entry);
        story.setFeedName(feed);
        story.setViewPrincipal(new Principal(view));
        story.setMoment(moment);
        if (entry != null && !entry.isOriginal()) {
            story.setRemoteNodeName(entry.getReceiverName());
            story.setRemotePostingId(entry.getReceiverEntryId());
        }
        entityManager.persist(story);
        return story;
    }
}
