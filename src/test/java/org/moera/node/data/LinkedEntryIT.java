package org.moera.node.data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import org.flywaydb.core.Flyway;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.moera.lib.node.types.FeedSliceInfo;
import org.moera.lib.node.types.PostingInfo;
import org.moera.lib.node.types.StoryType;
import org.moera.lib.node.types.principal.AccessCheckers;
import org.moera.lib.node.types.principal.Principal;
import org.moera.lib.node.types.notifications.Notification;
import org.moera.lib.node.types.notifications.PostingUnsharedNotification;
import org.moera.node.global.UniversalContext;
import org.moera.node.global.RequestContext;
import org.moera.node.config.DirectServeSource;
import org.moera.node.liberin.receptor.PostingReceptor;
import org.moera.node.media.DirectServeOperations;
import org.moera.node.media.DirectServeOperations.DirectServePath;
import org.moera.node.media.MediaOperations;
import org.moera.node.model.PostingInfoUtil;
import org.moera.node.notification.send.Direction;
import org.moera.node.operations.FavorOperations;
import org.moera.node.operations.EntryOperations;
import org.moera.node.operations.BlockedUserOperations;
import org.moera.node.operations.BlockedByUserOperations;
import org.moera.node.operations.LinkedEntryOperations;
import org.moera.node.operations.MediaAttachmentsProvider;
import org.moera.node.operations.PostingOperations;
import org.moera.node.operations.PostingInfoOperations;
import org.moera.node.operations.StoryOperations;
import org.moera.node.picker.PickerPool;
import org.moera.node.rest.FeedController;
import org.moera.node.task.Jobs;
import org.moera.node.userlist.SheriffUserListOperations;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.SharedEntityManagerCreator;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@EnabledIfSystemProperty(named = "moera.test.database", matches = ".+")
class LinkedEntryIT {

    private static LocalContainerEntityManagerFactoryBean factoryBean;
    private static EntityManagerFactory factory;

    private EntityManager entityManager;
    private TransactionTemplate transaction;
    private LinkedEntryRepository linkedEntryRepository;
    private PostingRepository postingRepository;
    private LinkedEntryOperations operations;
    private PostingInfoOperations postingInfoOperations;
    private EntryOperations entryOperations;
    private RequestContext requestContext;
    private final DirectServeOperations directServeOperations = mock(DirectServeOperations.class);
    private final PickerPool pickerPool = mock(PickerPool.class);
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
            "hibernate.physical_naming_strategy", "org.hibernate.boot.model.naming.PhysicalNamingStrategySnakeCaseImpl",
            "hibernate.generate_statistics", "true"
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
        linkedEntryRepository = repositories.getRepository(LinkedEntryRepository.class);
        postingRepository = repositories.getRepository(PostingRepository.class);
        nodeId = UUID.randomUUID();
        UniversalContext context = mock(UniversalContext.class);
        when(context.nodeId()).thenReturn(nodeId);
        when(context.nodeName()).thenReturn("local_0");
        operations = new LinkedEntryOperations();
        ReflectionTestUtils.setField(operations, "universalContext", context);
        ReflectionTestUtils.setField(operations, "linkedEntryRepository", linkedEntryRepository);
        ReflectionTestUtils.setField(operations, "postingRepository", postingRepository);
        ReflectionTestUtils.setField(operations, "pickerPool", pickerPool);
        doCallRealMethod().when(pickerPool).pickAfterCommit(any(Pick.class));

        requestContext = mock(RequestContext.class);
        when(requestContext.nodeId()).thenReturn(nodeId);
        when(directServeOperations.source()).thenReturn(DirectServeSource.AWSS3);
        when(requestContext.isPrincipal(any(), any())).thenAnswer(invocation ->
            AccessCheckers.PUBLIC.isPrincipal(invocation.getArgument(0), invocation.getArgument(1))
        );
        entryOperations = new EntryOperations();
        ReflectionTestUtils.setField(entryOperations, "entryAttachmentRepository",
            repositories.getRepository(EntryAttachmentRepository.class));
        ReflectionTestUtils.setField(entryOperations, "directServeOperations", directServeOperations);
        ReflectionTestUtils.setField(entryOperations, "objectMapper", new ObjectMapper());
        ReflectionTestUtils.setField(entryOperations, "jobs", mock(Jobs.class));
        postingInfoOperations = new PostingInfoOperations();
        ReflectionTestUtils.setField(postingInfoOperations, "requestContext", requestContext);
        ReflectionTestUtils.setField(postingInfoOperations, "linkedEntryRepository", linkedEntryRepository);
        ReflectionTestUtils.setField(postingInfoOperations, "postingRepository", postingRepository);
        ReflectionTestUtils.setField(
            postingInfoOperations, "storyRepository", repositories.getRepository(StoryRepository.class)
        );
        ReflectionTestUtils.setField(postingInfoOperations, "entryOperations", entryOperations);
        ReflectionTestUtils.setField(postingInfoOperations, "directServeOperations", directServeOperations);
        ReflectionTestUtils.setField(
            postingInfoOperations, "reactionRepository", repositories.getRepository(ReactionRepository.class)
        );
        ReflectionTestUtils.setField(
            postingInfoOperations, "ownReactionRepository", repositories.getRepository(OwnReactionRepository.class)
        );
        ReflectionTestUtils.setField(postingInfoOperations, "blockedUserOperations", mock(BlockedUserOperations.class));
        ReflectionTestUtils.setField(
            postingInfoOperations, "blockedByUserOperations", mock(BlockedByUserOperations.class)
        );
        ReflectionTestUtils.setField(
            postingInfoOperations, "sheriffUserListOperations", mock(SheriffUserListOperations.class)
        );
    }

    @Test
    void referencedPostingSurvivesCleanupUntilSourceIsRemoved() {
        UUID[] ids = transaction.execute(status -> {
            Posting source = posting(nodeId);
            Posting target = posting(nodeId);
            LinkedEntry link = link(source, target);
            Pick pick = new Pick();
            pick.setId(UUID.randomUUID());
            pick.setNodeId(nodeId);
            pick.setRemoteNodeName("remote_0");
            pick.setRemotePostingId("target");
            pick.setLinkedEntry(link);
            entityManager.persist(pick);
            return new UUID[]{source.getId(), target.getId(), link.getId(), pick.getId()};
        });

        transaction.executeWithoutResult(status -> {
            assertFalse(postingRepository.findUnlinked(nodeId).stream().anyMatch(p -> p.getId().equals(ids[1])));
            assertTrue(linkedEntryRepository.findByNodeIdAndId(UUID.randomUUID(), ids[2]).isEmpty());
            entityManager.createNativeQuery("DELETE FROM entries WHERE id = ?1").setParameter(1, ids[0])
                .executeUpdate();
        });
        transaction.executeWithoutResult(status -> {
            assertTrue(linkedEntryRepository.findById(ids[2]).isEmpty());
            assertNull(entityManager.find(Pick.class, ids[3]));
            assertEquals(ids[1], postingRepository.findUnlinked(nodeId).getFirst().getId());
        });
    }

    @Test
    void deletingOriginalTargetClearsResolutionWithoutDeletingTheReference() {
        UUID[] ids = transaction.execute(status -> {
            Posting source = posting(nodeId);
            Posting target = posting(nodeId);
            LinkedEntry link = link(source, target);
            return new UUID[]{target.getId(), link.getId()};
        });

        transaction.executeWithoutResult(status -> entityManager.createNativeQuery("DELETE FROM entries WHERE id = ?1")
            .setParameter(1, ids[0]).executeUpdate());

        transaction.executeWithoutResult(status -> {
            LinkedEntry link = linkedEntryRepository.findById(ids[1]).orElseThrow();
            assertNull(link.getLinkedEntry());
            assertEquals(ids[0].toString(), link.getReceiverEntryId());
        });
    }

    @Test
    void localReferencesCannotResolvePostingsFromAnotherTenant() {
        transaction.executeWithoutResult(status -> {
            Posting source = posting(nodeId);
            Posting target = posting(UUID.randomUUID());
            source.getCurrentRevision().setBody(
                "{\"text\":\"<a href='https://moera.page/@local/~/post/" + target.getId() + "'>post</a>\"}"
            );

            operations.create(source.getCurrentRevision());
            entityManager.flush();

            assertEquals(1, source.getCurrentRevision().getLinkedEntries().size());
            assertNull(source.getCurrentRevision().getLinkedEntries().iterator().next().getLinkedEntry());
        });
    }

    @Test
    void pickedTargetIsPersistedAndReturnedByTheApi() {
        var pickTransaction = new TransactionTemplate(transaction.getTransactionManager());
        pickTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        doAnswer(invocation -> {
            Pick pick = invocation.getArgument(0);
            pickTransaction.executeWithoutResult(status -> {
                assertTrue(linkedEntryRepository.findById(pick.getLinkedEntry().getId()).isPresent());
                pick.setId(UUID.randomUUID());
                entityManager.persist(pick);
            });
            return null;
        }).when(pickerPool).pick(any(Pick.class));

        UUID sourceId = transaction.execute(status -> {
            Posting source = posting(nodeId);
            source.getCurrentRevision().setBody(
                "{\"text\":\"<a href='https://moera.page/@remote/~/post/target'>post</a>\"}"
            );
            operations.create(source.getCurrentRevision());
            verify(pickerPool, never()).pick(any(Pick.class));
            return source.getId();
        });

        ArgumentCaptor<Pick> pickCaptor = ArgumentCaptor.forClass(Pick.class);
        verify(pickerPool).pickAfterCommit(pickCaptor.capture());
        Pick pick = pickCaptor.getValue();
        verify(pickerPool).pick(pick);
        assertEquals(nodeId, pick.getNodeId());
        assertEquals("remote_0", pick.getRemoteNodeName());
        assertEquals("target", pick.getRemotePostingId());
        assertNull(pick.getFeedName());

        UUID targetId = transaction.execute(status -> {
            assertEquals(
                pick.getLinkedEntry().getId(), entityManager.find(Pick.class, pick.getId()).getLinkedEntry().getId()
            );
            Posting target = posting(nodeId);
            target.setReceiverName("remote_0");
            target.setReceiverEntryId("target");
            operations.picked(pick, target);
            return target.getId();
        });

        transaction.executeWithoutResult(status -> {
            Posting source = postingRepository.findById(sourceId).orElseThrow();
            var info = PostingInfoUtil.build(
                source, MediaAttachmentsProvider.NONE, AccessCheckers.PUBLIC, null, mock(DirectServeOperations.class)
            );
            postingInfoOperations.withLinkedPostings(info);
            assertEquals(targetId.toString(), info.getLinkedPostings().getFirst().getId());
            assertFalse(postingRepository.findUnlinked(nodeId).stream().anyMatch(p -> p.getId().equals(targetId)));
        });
        verifyNoMoreInteractions(pickerPool);
    }

    @Test
    void rolledBackRevisionDoesNotSubmitLinkedPicks() {
        UUID revisionId = transaction.execute(status -> {
            Posting source = posting(nodeId);
            source.getCurrentRevision().setBody(
                "{\"text\":\"<a href='https://moera.page/@remote/~/post/target'>post</a>\"}"
            );
            operations.create(source.getCurrentRevision());
            entityManager.flush();
            verify(pickerPool, never()).pick(any(Pick.class));
            status.setRollbackOnly();
            return source.getCurrentRevision().getId();
        });

        verify(pickerPool).pickAfterCommit(any(Pick.class));
        verify(pickerPool, never()).pick(any(Pick.class));
        transaction.executeWithoutResult(status ->
            assertTrue(linkedEntryRepository.findByRevisionId(nodeId, revisionId).isEmpty())
        );
    }

    @Test
    void localRevisionCreationAndRestorationStoreFinalBodyReferences() {
        transaction.executeWithoutResult(status -> {
            Posting source = posting(nodeId);
            source.getCurrentRevision().setSignature(new byte[]{1});
            Posting target = posting(nodeId);
            var postingOperations = new PostingOperations();
            ReflectionTestUtils.setField(
                postingOperations, "universalContext", ReflectionTestUtils.getField(operations, "universalContext")
            );
            ReflectionTestUtils.setField(postingOperations, "postingRepository", postingRepository);
            ReflectionTestUtils.setField(postingOperations, "entryRevisionRepository",
                new JpaRepositoryFactory(entityManager).getRepository(EntryRevisionRepository.class));
            ReflectionTestUtils.setField(postingOperations, "mediaOperations", mock(MediaOperations.class));
            ReflectionTestUtils.setField(postingOperations, "storyOperations", mock(StoryOperations.class));
            ReflectionTestUtils.setField(postingOperations, "linkedEntryOperations", operations);
            String body = "{\"text\":\"<a href='https://moera.page/@local/~/post/" + target.getId() + "'>post</a>\"}";

            postingOperations.createOrUpdatePosting(
                source, source.getCurrentRevision(), List.of(), List.of(), null,
                revision -> revision.setBody(body), null
            );
            EntryRevision saved = source.getCurrentRevision();
            saved.setSignature(new byte[]{1});
            assertEquals(target.getId(), saved.getLinkedEntries().iterator().next().getLinkedEntry().getId());

            postingOperations.createOrUpdatePosting(source, saved, List.of(), List.of(), null, null, null);
            entityManager.flush();

            EntryRevision restored = source.getCurrentRevision();
            assertFalse(saved.getId().equals(restored.getId()));
            assertEquals(1, restored.getLinkedEntries().size());
            assertEquals(target.getId(), restored.getLinkedEntries().iterator().next().getLinkedEntry().getId());
        });
    }

    @Test
    void referenceNotificationsReadRevisionsAfterTheProducingTransactionHasClosed() {
        UUID sourceId = transaction.execute(status -> {
            Posting source = posting(nodeId);
            source.getCurrentRevision().setSignature(new byte[]{1});
            link(source, posting(nodeId));
            return source.getId();
        });
        Posting source = transaction.execute(status -> {
            Posting posting = postingRepository.findById(sourceId).orElseThrow();
            posting.getCurrentRevision().getSignature();
            return posting;
        });
        var notifications = new ArrayList<Notification>();
        PostingReceptor receptor = new PostingReceptor() {
            @Override
            protected void send(Direction direction, Notification notification) {
                notifications.add(notification);
            }
        };
        ReflectionTestUtils.setField(receptor, "favorOperations", mock(FavorOperations.class));
        ReflectionTestUtils.setField(receptor, "linkedEntryRepository", linkedEntryRepository);

        transaction.executeWithoutResult(status -> ReflectionTestUtils.invokeMethod(
            receptor, "notifyReferences", source, null, Principal.PUBLIC, source.getCurrentRevision(), Principal.PUBLIC
        ));

        assertEquals(1, notifications.size());
        assertInstanceOf(PostingUnsharedNotification.class, notifications.getFirst());
    }

    @ParameterizedTest
    @EnumSource(LinkedTargetKind.class)
    void feedSliceLoadsDistinctLinkedTargetsWithAConstantNumberOfQueries(LinkedTargetKind kind) {
        if (kind == LinkedTargetKind.OWN_REACTION) {
            when(requestContext.isOwner()).thenReturn(true);
            when(requestContext.nodeName()).thenReturn("local_0");
            when(requestContext.isAdmin(any())).thenReturn(true);
            doReturn(true).when(requestContext).isPrincipal(any(), any());
        }
        if (kind == LinkedTargetKind.COMPRESSED_ATTACHMENT) {
            when(directServeOperations.directPath(any(), any(), any())).thenReturn(DirectServePath.NONE);
            when(directServeOperations.directDownloadPath(any(), any(), any())).thenReturn(DirectServePath.NONE);
        }
        transaction.executeWithoutResult(status -> {
            for (int i = 1; i <= 20; i++) {
                Posting source = posting(nodeId);
                story(source, Feed.TIMELINE, i);
                Posting target = posting(nodeId);
                target.setOwnerAvatarMediaFile(avatar());
                target.setReceiverAvatarMediaFile(avatar());
                EntrySource entrySource = new EntrySource();
                entrySource.setId(UUID.randomUUID());
                entrySource.setEntry(target);
                entrySource.setRemoteAvatarMediaFile(avatar());
                entityManager.persist(entrySource);
                if (kind == LinkedTargetKind.MEDIA) {
                    Posting parent = posting(nodeId);
                    parent.setOwnerAvatarMediaFile(avatar());
                    parent.setReceiverAvatarMediaFile(avatar());
                    target.setParentMediaEntry(parent);
                    RemoteMediaFile media = new RemoteMediaFile();
                    media.setId(UUID.randomUUID());
                    media.setNodeId(nodeId);
                    media.setNodeName("remote_0");
                    media.setMediaId("media-" + i);
                    entityManager.persist(media);
                    target.setParentRemoteMedia(media);
                } else if (kind == LinkedTargetKind.COMPRESSED_ATTACHMENT) {
                    MediaFileOwner compressed = mediaOwner();
                    MediaFileOwner original = mediaOwner();
                    original.setCompressedOwner(compressed);
                    EntryAttachment attachment = new EntryAttachment();
                    attachment.setId(UUID.randomUUID());
                    attachment.setEntryRevision(target.getCurrentRevision());
                    attachment.setMediaFileOwner(original);
                    entityManager.persist(attachment);
                } else if (kind == LinkedTargetKind.OWN_REACTION) {
                    target.setReceiverName("remote_0");
                    target.setReceiverEntryId("remote-" + i);
                    OwnReaction reaction = new OwnReaction();
                    reaction.setId(UUID.randomUUID());
                    reaction.setNodeId(nodeId);
                    reaction.setRemoteNodeName("remote_0");
                    reaction.setRemotePostingId(target.getReceiverEntryId());
                    reaction.setRemoteAvatarMediaFile(avatar());
                    reaction.setEmoji(123);
                    entityManager.persist(reaction);
                }
                story(target, Feed.NEWS, i);
                link(source, target);
                if (kind == LinkedTargetKind.RECURSIVE || kind == LinkedTargetKind.CYCLIC) {
                    Posting previous = target;
                    for (int depth = 2; depth <= 3; depth++) {
                        Posting nested = posting(nodeId);
                        nested.getCurrentRevision().setHeading("level-" + depth);
                        nested.setOwnerAvatarMediaFile(avatar());
                        nested.setReceiverAvatarMediaFile(avatar());
                        story(nested, Feed.NEWS, i);
                        link(previous, nested);
                        previous = nested;
                    }
                    if (kind == LinkedTargetKind.CYCLIC) {
                        link(previous, target);
                    }
                }
            }
        });

        long singleQueries = feedSliceQueries(1, kind);
        long manyQueries = feedSliceQueries(20, kind);

        org.slf4j.LoggerFactory.getLogger(LinkedEntryIT.class).info(
            "Feed SQL queries ({}): one posting = {}, twenty postings = {}", kind, singleQueries, manyQueries
        );
        assertEquals(singleQueries, manyQueries, "Query count must not grow with the number of linked postings");
        int maxQueries = kind == LinkedTargetKind.RECURSIVE || kind == LinkedTargetKind.CYCLIC ? 12 : 10;
        assertTrue(manyQueries <= maxQueries, "Unexpected query count: " + manyQueries);
    }

    @Test
    void responseLoadingDoesNotIncludeDeletedOrForeignTenantTargets() {
        List<PostingInfo> infos = transaction.execute(status -> {
            Posting source = posting(nodeId);
            Posting deleted = posting(nodeId);
            deleted.setDeletedAt(org.moera.node.util.Util.now());
            link(source, deleted);
            Posting foreign = posting(UUID.randomUUID());
            link(source, foreign);
            Posting visible = posting(nodeId);
            link(source, visible);
            link(visible, deleted);
            link(visible, foreign);
            var info = new PostingInfo();
            info.setId(source.getId().toString());
            info.setRevisionId(source.getCurrentRevision().getId().toString());
            return List.of(info);
        });

        transaction.executeWithoutResult(status -> postingInfoOperations.fillLinkedPostings(infos));

        assertEquals(1, infos.getFirst().getLinkedPostings().size());
        assertTrue(infos.getFirst().getLinkedPostings().getFirst().getLinkedPostings().isEmpty());
    }

    private long feedSliceQueries(int count, LinkedTargetKind kind) {
        return transaction.execute(status -> {
            FeedController controller = new FeedController();
            ReflectionTestUtils.setField(controller, "requestContext", requestContext);
            ReflectionTestUtils.setField(controller, "entityManager", entityManager);
            ReflectionTestUtils.setField(controller, "entryOperations", entryOperations);
            ReflectionTestUtils.setField(controller, "postingInfoOperations", postingInfoOperations);
            ReflectionTestUtils.setField(controller, "directServeOperations", directServeOperations);
            var repositories = new JpaRepositoryFactory(entityManager);
            ReflectionTestUtils.setField(controller, "reactionRepository",
                repositories.getRepository(ReactionRepository.class));
            ReflectionTestUtils.setField(controller, "ownReactionRepository",
                repositories.getRepository(OwnReactionRepository.class));
            ReflectionTestUtils.setField(
                controller, "sheriffUserListOperations", mock(SheriffUserListOperations.class)
            );
            FeedSliceInfo slice = new FeedSliceInfo();
            slice.setAfter(0L);
            slice.setBefore((long) count);
            slice.setStories(new ArrayList<>());
            var statistics = factory.unwrap(SessionFactory.class).getStatistics();
            statistics.clear();

            ReflectionTestUtils.invokeMethod(controller, "fillSlice", slice, Feed.TIMELINE);

            assertEquals(count, slice.getStories().size());
            slice.getStories().forEach(story -> {
                assertEquals(1, story.getPosting().getLinkedPostings().size());
                var target = story.getPosting().getLinkedPostings().getFirst();
                assertEquals(1, target.getSources().size());
                if (kind == LinkedTargetKind.MEDIA) {
                    assertNotNull(target.getParentMedia());
                    assertEquals("remote_0", target.getParentMedia().getNodeName());
                    assertNull(target.getLinkedPostings());
                } else if (kind == LinkedTargetKind.COMPRESSED_ATTACHMENT) {
                    assertEquals(1, target.getMedia().size());
                    assertNotNull(target.getMedia().getFirst().getMedia().getCompressedMediaId());
                } else if (kind == LinkedTargetKind.OWN_REACTION) {
                    assertEquals(123, target.getClientReaction().getEmoji());
                }
                if (kind == LinkedTargetKind.RECURSIVE || kind == LinkedTargetKind.CYCLIC) {
                    for (int depth = 2; depth <= 3; depth++) {
                        assertEquals(1, target.getLinkedPostings().size());
                        target = target.getLinkedPostings().getFirst();
                        assertEquals("level-" + depth, target.getHeading());
                    }
                }
                if (kind != LinkedTargetKind.MEDIA) {
                    assertTrue(target.getLinkedPostings().isEmpty());
                }
            });
            return statistics.getPrepareStatementCount();
        });
    }

    private void story(Posting posting, String feedName, long moment) {
        Story story = new Story();
        story.setId(UUID.randomUUID());
        story.setNodeId(nodeId);
        story.setEntry(posting);
        story.setFeedName(feedName);
        story.setMoment(moment);
        story.setStoryType(StoryType.POSTING_ADDED);
        entityManager.persist(story);
    }

    private MediaFile avatar() {
        MediaFile media = new MediaFile();
        media.setId(UUID.randomUUID().toString());
        media.setMimeType("image/png");
        entityManager.persist(media);
        return media;
    }

    private MediaFileOwner mediaOwner() {
        MediaFileOwner owner = new MediaFileOwner();
        owner.setId(UUID.randomUUID());
        owner.setNodeId(nodeId);
        owner.setMediaFile(avatar());
        entityManager.persist(owner);
        return owner;
    }

    private enum LinkedTargetKind {
        NORMAL,
        RECURSIVE,
        CYCLIC,
        MEDIA,
        COMPRESSED_ATTACHMENT,
        OWN_REACTION
    }

    private Posting posting(UUID tenantId) {
        Posting posting = new Posting();
        posting.setId(UUID.randomUUID());
        posting.setNodeId(tenantId);
        posting.setOwnerName("owner_0");
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

    private LinkedEntry link(Posting source, Posting target) {
        LinkedEntry link = new LinkedEntry();
        link.setId(UUID.randomUUID());
        link.setReceiverNodeName("local_0");
        link.setReceiverEntryId(target.getId().toString());
        link.setLinkedEntry(target);
        source.getCurrentRevision().addLinkedEntry(link);
        return linkedEntryRepository.save(link);
    }

}
