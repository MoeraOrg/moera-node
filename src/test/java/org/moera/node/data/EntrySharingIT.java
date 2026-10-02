package org.moera.node.data;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.moera.lib.node.types.SharingType;
import org.moera.lib.node.types.AvatarImage;
import org.moera.lib.node.types.SharingInfo;
import org.moera.lib.node.types.StoryInfo;
import org.moera.lib.node.types.StoryType;
import org.moera.lib.node.types.notifications.PostingSharedNotification;
import org.moera.lib.node.types.notifications.PostingUnsharedNotification;
import org.moera.node.global.UniversalContext;
import org.moera.node.instant.PostingSharingInstants;
import org.moera.node.liberin.Liberin;
import org.moera.node.liberin.model.PostingSharingAddedLiberin;
import org.moera.node.liberin.model.PostingSharingDeletedLiberin;
import org.moera.node.liberin.receptor.PostingSharingReceptor;
import org.moera.node.media.DirectServeOperations;
import org.moera.node.media.MediaManager;
import org.moera.node.model.AvatarImageUtil;
import org.moera.node.model.ObjectNotFoundFailure;
import org.moera.node.model.event.Event;
import org.moera.node.model.StoryInfoUtil;
import org.moera.node.operations.SharingOperations;
import org.moera.node.operations.ContactOperations;
import org.moera.node.rest.notification.PostingSharingProcessor;
import org.moera.node.task.Jobs;
import org.moera.node.util.Transaction;
import org.moera.node.option.Options;
import org.moera.node.util.SafeInteger;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.SharedEntityManagerCreator;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/** Applies all migrations to an isolated PostgreSQL database supplied by moera.test.database. */
@EnabledIfSystemProperty(named = "moera.test.database", matches = ".+")
class EntrySharingIT {

    private static LocalContainerEntityManagerFactoryBean factoryBean;
    private static EntityManagerFactory factory;

    private EntityManager entityManager;
    private TransactionTemplate transaction;
    private EntrySharingRepository sharings;
    private PostingRepository postings;
    private StoryRepository stories;
    private SharingOperations operations;
    private UniversalContext context;
    private UUID nodeId;
    private UUID postingId;
    private UUID revisionId;

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
        entityManager = SharedEntityManagerCreator.createSharedEntityManager(factory);
        transaction = new TransactionTemplate(new JpaTransactionManager(factory));
        JpaRepositoryFactory repositories = new JpaRepositoryFactory(entityManager);
        sharings = repositories.getRepository(EntrySharingRepository.class);
        postings = repositories.getRepository(PostingRepository.class);
        stories = repositories.getRepository(StoryRepository.class);
        nodeId = UUID.randomUUID();
        postingId = UUID.randomUUID();
        revisionId = UUID.randomUUID();
        context = mock(UniversalContext.class);
        when(context.nodeId()).thenReturn(nodeId);
        when(context.nodeName()).thenReturn("local");
        when(context.getOptions()).thenReturn(mock(Options.class));
        operations = new SharingOperations();
        ReflectionTestUtils.setField(operations, "universalContext", context);
        ReflectionTestUtils.setField(operations, "postingRepository", postings);
        ReflectionTestUtils.setField(operations, "entrySharingRepository", sharings);
        ReflectionTestUtils.setField(operations, "storyRepository", stories);
        ReflectionTestUtils.setField(operations, "entryRevisionRepository",
            repositories.getRepository(EntryRevisionRepository.class));
        transaction.executeWithoutResult(status -> {
            Posting posting = new Posting();
            posting.setId(postingId);
            posting.setNodeId(nodeId);
            posting.setOwnerName("local");
            entityManager.persist(posting);
            EntryRevision revision = new EntryRevision();
            revision.setId(revisionId);
            revision.setEntry(posting);
            revision.setHeading("Shared posting");
            revision.setBody("{\"text\":\"Shared posting\"}");
            revision.setBodyPreview("{\"text\":\"Shared posting\"}");
            entityManager.persist(revision);
            posting.setCurrentRevision(revision);
        });
    }

    @Test
    void duplicateNotificationsAndConcurrentRetriesCountOnce() throws Exception {
        PostingSharedNotification notification = shared("alice", null, null, null, null, "copy");
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> add(notification));
            var second = executor.submit(() -> add(notification));
            first.get();
            second.get();
        }
        add(notification);
        transaction.executeWithoutResult(status -> {
            assertEquals(1, sharings.countByEntryId(postingId));
            assertEquals(1, postings.findByNodeIdAndId(nodeId, postingId).orElseThrow().getTotalSharings());
        });
    }

    @Test
    void storyIdentityIgnoresFeedAndMomentForRetriesAndUnsharing() {
        add(shared("alice", null, "timeline", "story", 12L, "copy"));
        add(shared("alice", null, "news", "story", 99L, "copy"));
        transaction.executeWithoutResult(status -> assertEquals(1, sharings.countByEntryId(postingId)));
        transaction.executeWithoutResult(status ->
            operations.unshared(unshared("alice", null, "news", "story", 200L, "copy"))
        );
        transaction.executeWithoutResult(status -> {
            assertEquals(0, sharings.countByEntryId(postingId));
            assertEquals(0, postings.findByNodeIdAndId(nodeId, postingId).orElseThrow().getTotalSharings());
        });
    }

    @Test
    void unsharingMatchesNullsSenderRevisionStoryAndRemotePosting() {
        add(shared("alice", null, null, null, null, "copy"));
        add(shared("bob", null, null, null, null, "copy"));
        add(shared("alice", revisionId.toString(), null, null, null, "copy"));
        add(shared("alice", null, "timeline", "story", 12L, "copy"));
        add(shared("alice", null, "timeline", "story", 13L, "copy"));
        add(shared("alice", null, "timeline", "other", 12L, "copy"));
        add(shared("alice", null, "news", "story", 12L, "copy"));
        add(shared("alice", null, "timeline", "story", 12L, "other-copy"));
        transaction.executeWithoutResult(status ->
            operations.unshared(unshared("alice", null, null, null, null, "copy"))
        );
        transaction.executeWithoutResult(status -> {
            assertEquals(5, sharings.countByEntryId(postingId));
            assertEquals(5, postings.findByNodeIdAndId(nodeId, postingId).orElseThrow().getTotalSharings());
            assertTrue(sharings.findMatching(postingId, SharingType.REPUBLISH,
                "alice", null, null, "copy").isEmpty());
        });
        transaction.executeWithoutResult(status ->
            operations.unshared(unshared("alice", null, "timeline", "story", 12L, "copy"))
        );
        transaction.executeWithoutResult(status -> assertEquals(4, sharings.countByEntryId(postingId)));
    }

    @Test
    void unsharingRemovesAllExactDuplicatesIgnoringDisplayMetadata() {
        add(shared("alice", null, "timeline", "story", 0L, null));
        transaction.executeWithoutResult(status -> {
            EntrySharing duplicate = new EntrySharing();
            duplicate.setId(UUID.randomUUID());
            duplicate.setEntry(postings.findByNodeIdAndId(nodeId, postingId).orElseThrow());
            duplicate.setSharingType(SharingType.REPUBLISH);
            duplicate.setRemoteNodeName("alice");
            duplicate.setRemoteFullName("Another display name");
            duplicate.setRemoteFeedName("news");
            duplicate.setRemoteStoryId("story");
            duplicate.setRemoteStoryMoment(100L);
            duplicate.setMoment(1L);
            sharings.saveAndFlush(duplicate);
        });
        transaction.executeWithoutResult(status ->
            operations.unshared(unshared("alice", null, "timeline", "story", 0L, null))
        );
        transaction.executeWithoutResult(status -> {
            assertEquals(0, sharings.countByEntryId(postingId));
            assertEquals(0, postings.findByNodeIdAndId(nodeId, postingId).orElseThrow().getTotalSharings());
        });
    }

    @Test
    void listUsesMomentOrderAndAnotherTenantCannotMutateThePosting() {
        add(shared("alice", null, null, null, null, "first"));
        add(shared("bob", null, null, null, null, "second"));
        transaction.executeWithoutResult(status -> {
            for (EntrySharing sharing : sharings.findByEntryId(postingId)) {
                boolean first = sharing.getRemoteNodeName().equals("alice");
                sharing.setMoment(first ? 10000L : 20000L);
                sharing.setCreatedAt(Timestamp.from(Instant.ofEpochSecond(first ? 20 : 10)));
            }
        });
        transaction.executeWithoutResult(status -> {
            List<EntrySharing> list = sharings.findByEntryId(postingId);
            assertEquals("alice", list.getFirst().getRemoteNodeName());
            assertEquals("bob", list.getLast().getRemoteNodeName());
        });
        when(context.nodeId()).thenReturn(UUID.randomUUID());
        assertThrows(ObjectNotFoundFailure.class, () -> add(shared("eve", null, null, null, null, "copy")));
    }

    @Test
    void slicesTraverseEqualTimestampsWithoutSkippingOrRepeatingSharings() {
        add(shared("alice", null, null, null, null, "first"));
        add(shared("bob", null, null, null, null, "second"));
        add(shared("carol", null, null, null, null, "third"));
        transaction.executeWithoutResult(status -> {
            for (EntrySharing sharing : sharings.findByEntryId(postingId)) {
                sharing.setCreatedAt(Timestamp.from(Instant.ofEpochSecond(20)));
                sharing.setMoment(switch (sharing.getRemoteNodeName()) {
                    case "alice" -> 20001L;
                    case "bob" -> 20002L;
                    default -> 20003L;
                });
            }
        });
        transaction.executeWithoutResult(status -> {
            var first = operations.getBefore(postingId, SafeInteger.MAX_VALUE, 2);
            assertEquals(List.of("carol", "bob"),
                first.getSharings().stream().map(SharingInfo::getRemoteNodeName).toList());
            assertEquals(20001L, first.getAfter());
            var last = operations.getBefore(postingId, first.getAfter(), 2);
            assertEquals(List.of("alice"), last.getSharings().stream().map(SharingInfo::getRemoteNodeName).toList());
            assertEquals(20001L, last.getBefore());
            assertEquals(SafeInteger.MIN_VALUE, last.getAfter());
            assertTrue(operations.getBefore(postingId, last.getAfter(), 2).getSharings().isEmpty());
        });
    }

    @Test
    void sharingMomentsAreUniqueAndPaginationIsIsolatedByPosting() {
        for (int i = 0; i < 5; i++) {
            add(shared("alice", null, null, null, null, "copy-" + i));
        }
        transaction.executeWithoutResult(status -> {
            var slice = operations.getBefore(postingId, SafeInteger.MAX_VALUE, 10);
            assertEquals(5, slice.getSharings().stream().map(SharingInfo::getMoment).distinct().count());
            assertTrue(operations.getBefore(UUID.randomUUID(), SafeInteger.MAX_VALUE, 10).getSharings().isEmpty());
            assertTrue(operations.getBefore(postingId, SafeInteger.MAX_VALUE, 0).getSharings().isEmpty());
        });
    }

    @Test
    void groupedInstantsRemoveOnlyTheirSharingAndDeleteAnEmptyGroup() {
        TestInstants instants = new TestInstants();
        ReflectionTestUtils.setField(instants, "universalContext", context);
        ReflectionTestUtils.setField(instants, "storyRepository", stories);
        ReflectionTestUtils.setField(instants, "directServeOperations", mock(DirectServeOperations.class));
        add(shared("alice", null, null, null, null, "copy1"));
        add(shared("alice", null, null, null, null, "copy2"));
        add(shared("bob", null, null, null, null, "copy3"));
        transaction.executeWithoutResult(status -> {
            Posting posting = postings.findByNodeIdAndId(nodeId, postingId).orElseThrow();
            List<EntrySharing> list = sharings.findByEntryId(postingId);
            list.forEach(sharing -> instants.added(posting, sharing));
            instants.added(posting, list.getFirst());
            Story story = stories.findFullByFeedAndTypeAndEntryId(
                nodeId, Feed.INSTANT, StoryType.POSTING_SHARED, postingId
            ).getFirst();
            assertEquals(3, story.getSubstories().size());
            assertEquals(3, story.getSummaryData().getTotalSharings());
            assertEquals(2, story.getSummaryData().getSharings().size());
            assertEquals("Shared posting", story.getSummaryData().getPosting().getHeading());
            StoryInfo info = StoryInfoUtil.build(story, true, ignored -> null, mock(DirectServeOperations.class));
            assertEquals(postingId.toString(), info.getPostingId());
            assertNotNull(info.getSummaryNodeName());
            instants.deleted(postingId, stories.findSubIdsBySharingId(nodeId, list.getFirst().getId()));
            assertEquals(2, story.getSubstories().size());
            instants.deleted(postingId, stories.findSubIdsBySharingId(nodeId, list.get(1).getId()));
            instants.deleted(postingId, stories.findSubIdsBySharingId(nodeId, list.get(2).getId()));
            assertTrue(stories.findFullByFeedAndTypeAndEntryId(
                nodeId, Feed.INSTANT, StoryType.POSTING_SHARED, postingId
            ).isEmpty());
            assertFalse(instants.deleted.isEmpty());
        });
    }

    @Test
    void sharingRelationshipIsClearedBeforeTheReceptorRemovesItsSubstory() {
        List<Liberin> liberins = new ArrayList<>();
        captureLiberins(liberins);
        TestInstants instants = sharingInstants();
        TestSharingReceptor receptor = sharingReceptor(instants);
        add(shared("alice", null, "timeline", "alice-story", 12L, "alice-copy"));
        add(shared("bob", null, "timeline", "bob-story", 13L, "bob-copy"));
        transaction.executeWithoutResult(status -> {
            Posting posting = postings.findByNodeIdAndId(nodeId, postingId).orElseThrow();
            List<EntrySharing> list = sharings.findByEntryId(postingId);
            EntrySharing bob = list.stream().filter(sharing -> sharing.getRemoteNodeName().equals("bob"))
                .findFirst().orElseThrow();
            instants.added(posting, bob);
            Story group = stories.findFullByFeedAndTypeAndEntryId(
                nodeId, Feed.INSTANT, StoryType.POSTING_SHARED, postingId
            ).getFirst();
            group.getSubstories().iterator().next().setCreatedAt(Timestamp.from(Instant.ofEpochSecond(10)));
            EntrySharing alice = list.stream().filter(sharing -> sharing.getRemoteNodeName().equals("alice"))
                .findFirst().orElseThrow();
            instants.added(posting, alice);
        });
        transaction.executeWithoutResult(status -> {
            Story group = stories.findFullByFeedAndTypeAndEntryId(
                nodeId, Feed.INSTANT, StoryType.POSTING_SHARED, postingId
            ).getFirst();
            assertEquals(List.of("alice", "bob"), group.getSubstories().stream().map(Story::getSharing)
                .map(EntrySharing::getRemoteNodeName).sorted().toList());
            assertNotNull(group.getSharing());
            assertEquals("alice", group.getSharing().getRemoteNodeName());
            assertEquals(12L, StoryInfoUtil.build(group, true, ignored -> null, null).getRemoteStoryMoment());
            operations.unshared(unshared("alice", null, "timeline", "alice-story", 12L, "alice-copy"));
        });
        PostingSharingDeletedLiberin deleted = liberins.stream().filter(PostingSharingDeletedLiberin.class::isInstance)
            .map(PostingSharingDeletedLiberin.class::cast).findFirst().orElseThrow();
        assertEquals(1, deleted.getSubstoryIds().size());
        transaction.executeWithoutResult(status -> {
            Story substory = stories.findByNodeIdAndId(nodeId, deleted.getSubstoryIds().getFirst()).orElseThrow();
            assertNull(substory.getSharing());
            receptor.deleted(deleted);
        });
        transaction.executeWithoutResult(status -> {
            Story group = stories.findFullByFeedAndTypeAndEntryId(
                nodeId, Feed.INSTANT, StoryType.POSTING_SHARED, postingId
            ).getFirst();
            assertEquals(1, group.getSubstories().size());
            assertEquals("bob", group.getSubstories().iterator().next().getSharing().getRemoteNodeName());
            assertEquals(1, group.getSummaryData().getTotalSharings());
            assertNotNull(group.getSharing());
            assertEquals("bob", group.getSharing().getRemoteNodeName());
            assertEquals(13L, StoryInfoUtil.build(group, true, ignored -> null, null).getRemoteStoryMoment());
            operations.unshared(unshared("bob", null, "timeline", "bob-story", 13L, "bob-copy"));
        });
        PostingSharingDeletedLiberin last = liberins.stream().filter(PostingSharingDeletedLiberin.class::isInstance)
            .map(PostingSharingDeletedLiberin.class::cast).toList().getLast();
        transaction.executeWithoutResult(status -> {
            receptor.deleted(last);
            assertTrue(stories.findFullByFeedAndTypeAndEntryId(
                nodeId, Feed.INSTANT, StoryType.POSTING_SHARED, postingId
            ).isEmpty());
        });
    }

    @Test
    void delayedAddedLiberinDoesNotCreateAnInstantForADeletedSharing() {
        List<Liberin> liberins = new ArrayList<>();
        captureLiberins(liberins);
        add(shared("alice", null, "timeline", "story", 12L, "copy"));
        PostingSharingAddedLiberin added = liberins.stream().filter(PostingSharingAddedLiberin.class::isInstance)
            .map(PostingSharingAddedLiberin.class::cast).findFirst().orElseThrow();
        transaction.executeWithoutResult(status ->
            operations.unshared(unshared("alice", null, "timeline", "story", 12L, "copy"))
        );
        TestSharingReceptor receptor = sharingReceptor(sharingInstants());
        transaction.executeWithoutResult(status -> {
            receptor.added(added);
            assertTrue(stories.findFullByFeedAndTypeAndEntryId(
                nodeId, Feed.INSTANT, StoryType.POSTING_SHARED, postingId
            ).isEmpty());
        });
    }

    private void captureLiberins(List<Liberin> liberins) {
        doAnswer(invocation -> {
            Liberin liberin = invocation.getArgument(0);
            liberin.setNodeId(nodeId);
            liberins.add(liberin);
            return null;
        }).when(context).send(any(Liberin.class));
    }

    @Test
    void sharingWithoutAvatarIsAnnouncedOnlyByCompletion() {
        List<Liberin> liberins = new ArrayList<>();
        captureLiberins(liberins);
        PostingSharedNotification notification = shared("alice", null, "timeline", "story", 12L, "copy");
        EntrySharing sharing = transaction.execute(status -> operations.shared(notification));
        assertTrue(liberins.stream().noneMatch(PostingSharingAddedLiberin.class::isInstance));

        transaction.executeWithoutResult(status -> operations.completeShared(postingId, sharing.getId(), null));

        transaction.executeWithoutResult(status -> {
            EntrySharing stored = sharings.findByEntryIdAndId(postingId, sharing.getId()).orElseThrow();
            assertNull(stored.getRemoteAvatarMediaFile());
            assertNull(stored.getRemoteAvatarShape());
        });
        assertEquals(1, liberins.stream().filter(PostingSharingAddedLiberin.class::isInstance).count());
    }

    @Test
    void avatarCompletionUpdatesTheExistingSharingAndEmitsAddedAfterCommit() {
        List<Liberin> liberins = new ArrayList<>();
        captureLiberins(liberins);
        PostingSharedNotification notification = shared("alice", null, "timeline", "story", 12L, "copy");
        AvatarImage avatar = new AvatarImage();
        avatar.setMediaId(UUID.randomUUID().toString());
        avatar.setShape("circle");
        notification.setSenderAvatar(avatar);
        EntrySharing sharing = transaction.execute(status -> operations.shared(notification));
        assertTrue(liberins.stream().noneMatch(PostingSharingAddedLiberin.class::isInstance));

        transaction.executeWithoutResult(status -> {
            MediaFile mediaFile = new MediaFile();
            mediaFile.setId(avatar.getMediaId());
            mediaFile.setMimeType("image/png");
            mediaFile.setExposed(true);
            entityManager.persist(mediaFile);
            AvatarImageUtil.setMediaFile(avatar, mediaFile);
            operations.completeShared(postingId, sharing.getId(), avatar);
        });

        transaction.executeWithoutResult(status -> {
            List<EntrySharing> list = sharings.findByEntryId(postingId);
            assertEquals(1, list.size());
            assertEquals(sharing.getId(), list.getFirst().getId());
            assertEquals(avatar.getMediaId(), list.getFirst().getRemoteAvatarMediaFile().getId());
            assertEquals("circle", list.getFirst().getRemoteAvatarShape());
            assertEquals(1, postings.findByNodeIdAndId(nodeId, postingId).orElseThrow().getTotalSharings());
        });
        assertEquals(1, liberins.stream().filter(PostingSharingAddedLiberin.class::isInstance).count());
    }

    @Test
    void unsharingBeforeAvatarJobRunsCannotResurrectTheSharing() {
        List<Liberin> liberins = new ArrayList<>();
        captureLiberins(liberins);
        Jobs jobs = mock(Jobs.class);
        List<Object> scheduled = new ArrayList<>();
        doAnswer(invocation -> {
            scheduled.add(invocation.getArgument(0));
            scheduled.add(invocation.getArgument(1));
            return UUID.randomUUID();
        }).when(jobs).runAfterCommit(any(), any(), any());
        Transaction tx = new Transaction();
        ReflectionTestUtils.setField(tx, "txManager", new JpaTransactionManager(factory));
        ReflectionTestUtils.setField(tx, "universalContext", context);
        PostingSharingProcessor processor = new PostingSharingProcessor();
        ReflectionTestUtils.setField(processor, "universalContext", context);
        ReflectionTestUtils.setField(processor, "jobs", jobs);
        ReflectionTestUtils.setField(processor, "sharingOperations", operations);
        ReflectionTestUtils.setField(processor, "tx", tx);
        PostingSharedNotification notification = shared("alice", null, "timeline", "story", 12L, "copy");
        AvatarImage avatar = new AvatarImage();
        avatar.setMediaId("avatar");
        notification.setSenderAvatar(avatar);

        processor.shared(notification);
        transaction.executeWithoutResult(status -> assertEquals(1, sharings.countByEntryId(postingId)));
        assertTrue(liberins.stream().noneMatch(PostingSharingAddedLiberin.class::isInstance));
        processor.unshared(unshared("alice", null, "timeline", "story", 12L, "copy"));

        Class<?> jobClass = (Class<?>) scheduled.getFirst();
        Object job = BeanUtils.instantiateClass(jobClass);
        ReflectionTestUtils.setField(job, "parameters", scheduled.getLast());
        ReflectionTestUtils.setField(job, "tx", tx);
        ReflectionTestUtils.setField(job, "sharingOperations", operations);
        ReflectionTestUtils.setField(job, "mediaManager", mock(MediaManager.class));
        ReflectionTestUtils.setField(job, "contactOperations", mock(ContactOperations.class));
        ReflectionTestUtils.invokeMethod(job, "execute");

        transaction.executeWithoutResult(status -> {
            assertEquals(0, sharings.countByEntryId(postingId));
            assertEquals(0, postings.findByNodeIdAndId(nodeId, postingId).orElseThrow().getTotalSharings());
        });
        assertTrue(liberins.stream().noneMatch(PostingSharingAddedLiberin.class::isInstance));
    }

    private TestInstants sharingInstants() {
        TestInstants instants = new TestInstants();
        ReflectionTestUtils.setField(instants, "universalContext", context);
        ReflectionTestUtils.setField(instants, "storyRepository", stories);
        ReflectionTestUtils.setField(instants, "directServeOperations", mock(DirectServeOperations.class));
        return instants;
    }

    private TestSharingReceptor sharingReceptor(TestInstants instants) {
        TestSharingReceptor receptor = new TestSharingReceptor();
        ReflectionTestUtils.setField(receptor, "postingSharingInstants", instants);
        ReflectionTestUtils.setField(receptor, "postingRepository", postings);
        ReflectionTestUtils.setField(receptor, "entrySharingRepository", sharings);
        return receptor;
    }

    @Test
    void viewedOldGroupsExpireWhileUnreadGroupsKeepCollecting() {
        TestInstants instants = new TestInstants();
        ReflectionTestUtils.setField(instants, "universalContext", context);
        ReflectionTestUtils.setField(instants, "storyRepository", stories);
        ReflectionTestUtils.setField(instants, "directServeOperations", mock(DirectServeOperations.class));
        add(shared("alice", null, null, null, null, "first"));
        transaction.executeWithoutResult(status -> {
            Posting posting = postings.findByNodeIdAndId(nodeId, postingId).orElseThrow();
            instants.added(posting, sharings.findByEntryId(postingId).getFirst());
            Story group = stories.findFullByFeedAndTypeAndEntryId(
                nodeId, Feed.INSTANT, StoryType.POSTING_SHARED, postingId
            ).getFirst();
            group.setCreatedAt(Timestamp.from(Instant.now().minusSeconds(7 * 3600)));
            group.setRead(true);
            group.setViewed(true);
        });
        add(shared("bob", null, null, null, null, "second"));
        transaction.executeWithoutResult(status -> {
            Posting posting = postings.findByNodeIdAndId(nodeId, postingId).orElseThrow();
            instants.added(posting, sharings.findByEntryId(postingId).getLast());
            List<Story> groups = stories.findFullByFeedAndTypeAndEntryId(
                nodeId, Feed.INSTANT, StoryType.POSTING_SHARED, postingId
            );
            assertEquals(2, groups.size());
            assertFalse(groups.getFirst().isRead());
            assertFalse(groups.getFirst().isViewed());
            assertTrue(groups.getLast().isViewed());
            groups.getFirst().setCreatedAt(Timestamp.from(Instant.now().minusSeconds(7 * 3600)));
        });
        add(shared("charlie", null, null, null, null, "third"));
        transaction.executeWithoutResult(status -> {
            Posting posting = postings.findByNodeIdAndId(nodeId, postingId).orElseThrow();
            instants.added(posting, sharings.findByEntryId(postingId).getLast());
            List<Story> groups = stories.findFullByFeedAndTypeAndEntryId(
                nodeId, Feed.INSTANT, StoryType.POSTING_SHARED, postingId
            );
            assertEquals(2, groups.size());
            assertEquals(2, groups.getFirst().getSummaryData().getTotalSharings());
        });
    }

    private void add(PostingSharedNotification notification) {
        transaction.executeWithoutResult(status -> {
            EntrySharing sharing = operations.shared(notification);
            if (sharing != null) {
                operations.completeShared(postingId, sharing.getId(), notification.getSenderAvatar());
            }
        });
    }

    private PostingSharedNotification shared(
        String sender, String revision, String feed, String story, Long moment, String posting
    ) {
        PostingSharedNotification notification = new PostingSharedNotification();
        notification.setSenderNodeName(sender);
        notification.setSharingType(SharingType.REPUBLISH);
        notification.setPostingId(postingId.toString());
        notification.setPostingRevisionId(revision);
        notification.setRemoteFeedName(feed);
        notification.setRemoteStoryId(story);
        notification.setRemoteStoryMoment(moment);
        notification.setRemotePostingId(posting);
        return notification;
    }

    private PostingUnsharedNotification unshared(
        String sender, String revision, String feed, String story, Long moment, String posting
    ) {
        PostingUnsharedNotification notification = new PostingUnsharedNotification();
        notification.setSenderNodeName(sender);
        notification.setSharingType(SharingType.REPUBLISH);
        notification.setPostingId(postingId.toString());
        notification.setPostingRevisionId(revision);
        notification.setRemoteFeedName(feed);
        notification.setRemoteStoryId(story);
        notification.setRemoteStoryMoment(moment);
        notification.setRemotePostingId(posting);
        return notification;
    }

    private static class TestSharingReceptor extends PostingSharingReceptor {

        @Override
        protected void send(Liberin liberin, Event event) {
        }

    }

    private static class TestInstants extends PostingSharingInstants {

        private final List<Story> deleted = new ArrayList<>();
        private long moment;

        @Override
        protected boolean isBlocked(StoryType type, UUID entry, String node, String posting, String owner) {
            return false;
        }

        @Override
        protected void updateMoment(Story story) {
            story.setMoment(++moment);
        }

        @Override
        protected void storyDeleted(Story story) {
            deleted.add(story);
        }

    }

}
