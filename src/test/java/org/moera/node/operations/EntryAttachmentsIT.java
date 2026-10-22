package org.moera.node.operations;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import jakarta.persistence.EntityManager;

import org.flywaydb.core.Flyway;
import org.hibernate.Hibernate;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.moera.lib.node.types.DraftType;
import org.moera.lib.node.types.MediaAttachment;
import org.moera.node.config.DirectServeSource;
import org.moera.node.data.Draft;
import org.moera.node.data.Comment;
import org.moera.node.data.CommentRepository;
import org.moera.node.data.DraftRepository;
import org.moera.node.data.EntryAttachment;
import org.moera.node.data.EntryAttachmentRepository;
import org.moera.node.data.EntryRevision;
import org.moera.node.data.EntryRevisionRepository;
import org.moera.node.data.MediaFile;
import org.moera.node.data.MediaFileOwner;
import org.moera.node.data.MediaFilePreview;
import org.moera.node.data.Posting;
import org.moera.node.data.PostingRepository;
import org.moera.node.data.RemoteMediaFile;
import org.moera.node.data.RemoteMediaFileRepository;
import org.moera.node.global.RequestContext;
import org.moera.node.global.UniversalContext;
import org.moera.node.media.DirectServeOperations.DirectServePath;
import org.moera.node.media.DirectServeOperations;
import org.moera.node.media.LocalRemoteMedia;
import org.moera.node.media.MediaOperations;
import org.moera.node.media.grant.MediaGrantSupplier;
import org.moera.node.model.DraftInfoUtil;
import org.moera.node.rest.DraftController;
import org.moera.node.rest.PostingRevisionController;
import org.moera.node.task.Jobs;
import org.moera.node.util.Transaction;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.SharedEntityManagerCreator;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Applies all migrations to an isolated PostgreSQL database supplied by moera.test.database. */
@EnabledIfSystemProperty(named = "moera.test.database", matches = ".+")
class EntryAttachmentsIT {

    private static LocalContainerEntityManagerFactoryBean factoryBean;

    private EntityManager entityManager;
    private TransactionTemplate transaction;
    private Transaction tx;
    private EntryAttachmentRepository entryAttachmentRepository;
    private EntryRevisionRepository entryRevisionRepository;
    private DraftRepository draftRepository;
    private PostingRepository postingRepository;
    private DirectServeOperations directServeOperations;
    private EntryOperations entryOperations;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private UUID nodeId;
    private UUID parentId;
    private UUID otherParentId;
    private UUID revisionId;
    private UUID ownerId;
    private UUID remoteId;

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
            "hibernate.generate_statistics", true,
            "hibernate.query.fail_on_pagination_over_collection_fetch", true
        ));
        factoryBean.afterPropertiesSet();
    }

    @AfterAll
    static void closeDatabase() {
        if (factoryBean != null) {
            factoryBean.destroy();
        }
    }

    @BeforeEach
    void setUp() {
        var factory = factoryBean.getObject();
        entityManager = SharedEntityManagerCreator.createSharedEntityManager(factory);
        var transactionManager = new JpaTransactionManager(factory);
        transaction = new TransactionTemplate(transactionManager);
        tx = new Transaction();
        ReflectionTestUtils.setField(tx, "txManager", transactionManager);
        ReflectionTestUtils.setField(tx, "universalContext", mock(UniversalContext.class));
        var repositories = new JpaRepositoryFactory(entityManager);
        entryAttachmentRepository = repositories.getRepository(EntryAttachmentRepository.class);
        entryRevisionRepository = repositories.getRepository(EntryRevisionRepository.class);
        draftRepository = repositories.getRepository(DraftRepository.class);
        postingRepository = repositories.getRepository(PostingRepository.class);
        directServeOperations = mock(DirectServeOperations.class);
        when(directServeOperations.directPath(any(), any())).thenReturn(DirectServePath.NONE);
        when(directServeOperations.directPath(any(), any(), any())).thenReturn(DirectServePath.NONE);
        when(directServeOperations.directDownloadPath(any(), any(), any())).thenReturn(DirectServePath.NONE);
        entryOperations = new EntryOperations();
        ReflectionTestUtils.setField(entryOperations, "entryAttachmentRepository", entryAttachmentRepository);
        ReflectionTestUtils.setField(entryOperations, "directServeOperations", directServeOperations);
        ReflectionTestUtils.setField(entryOperations, "objectMapper", objectMapper);
        ReflectionTestUtils.setField(entryOperations, "jobs", mock(Jobs.class));
        nodeId = UUID.randomUUID();
        transaction.executeWithoutResult(status -> {
            Posting parent = posting();
            parentId = parent.getId();
            otherParentId = posting().getId();
            EntryRevision revision = new EntryRevision();
            revisionId = UUID.randomUUID();
            revision.setId(revisionId);
            revision.setEntry(parent);
            entityManager.persist(revision);
            parent.setCurrentRevision(revision);

            MediaFile file = mediaFile();
            MediaFileOwner owner = new MediaFileOwner();
            ownerId = UUID.randomUUID();
            owner.setId(ownerId);
            owner.setNodeId(nodeId);
            owner.setMediaFile(file);
            entityManager.persist(owner);
            remoteId = remoteMedia().getId();
        });
    }

    @Test
    void localChildIsSelectedByParentWithoutLoadingPostingsOrDuplicatingPreviews() {
        UUID childId = child(parentId, true);
        child(otherParentId, true);
        child(null, true);
        attach(true, false, 0);
        transaction.executeWithoutResult(status -> {
            MediaFile file = entityManager.find(MediaFileOwner.class, ownerId).getMediaFile();
            for (int width : List.of(100, 200, 400)) {
                MediaFilePreview preview = new MediaFilePreview();
                preview.setId(UUID.randomUUID());
                preview.setOriginalMediaFile(file);
                preview.setMediaFile(mediaFile());
                preview.setWidth(width);
                entityManager.persist(preview);
            }
        });

        transaction.executeWithoutResult(status -> {
            var media = entryOperations.getMediaAttachments(revision(), null);

            assertEquals(1, media.size());
            assertEquals(childId.toString(), media.getFirst().getPostingId());
            assertEquals(3, media.getFirst().getMedia().getPreviews().size());
            assertLocalPostingsNotLoaded();
        });
    }

    @Test
    void remoteChildIsSelectedByParentWithoutLoadingPostings() {
        UUID childId = child(parentId, false);
        child(otherParentId, false);
        child(null, false);
        attach(false, true, 0);

        transaction.executeWithoutResult(status -> {
            var media = entryOperations.getMediaAttachments(revision(), null);

            assertEquals(1, media.size());
            assertEquals(childId.toString(), media.getFirst().getPostingId());
            assertFalse(Hibernate.isInitialized(entityManager.find(RemoteMediaFile.class, remoteId).getPostings()));
        });
    }

    @Test
    void downloadedRemoteMediaUsesLocalChild() {
        UUID localChildId = child(parentId, true);
        child(parentId, false);
        attach(true, true, 0);

        transaction.executeWithoutResult(status -> {
            var media = entryOperations.getMediaAttachments(revision(), null);

            assertEquals(localChildId.toString(), media.getFirst().getPostingId());
            assertLocalPostingsNotLoaded();
        });
    }

    @Test
    void downloadedRemoteMediaDoesNotFallBackToRemoteOrAnotherParentsChild() {
        child(otherParentId, true);
        child(parentId, false);
        attach(true, true, 0);

        transaction.executeWithoutResult(status -> {
            var media = entryOperations.getMediaAttachments(revision(), null);

            assertEquals(1, media.size());
            assertNull(media.getFirst().getPostingId());
            assertLocalPostingsNotLoaded();
        });
    }

    @Test
    void missingChildrenPreserveAttachmentOrderVisibilityAndEmbeddedFlag() {
        UUID localAttachmentId = attach(true, false, 2);
        attach(false, true, 0);
        transaction.executeWithoutResult(status -> {
            entityManager.find(EntryAttachment.class, localAttachmentId).setEmbedded(false);
            RemoteMediaFile invalid = remoteMedia();
            invalid.setInvalid(true);
            entityManager.persist(new EntryAttachment(revision(), null, invalid, 1));
        });

        transaction.executeWithoutResult(status -> {
            var media = entryOperations.getMediaAttachments(revision(), null);

            assertEquals(2, media.size());
            assertEquals(remoteId.toString(), media.getFirst().getRemoteMedia().getId());
            assertEquals(ownerId.toString(), media.getLast().getMedia().getId());
            assertNull(media.getFirst().getPostingId());
            assertNull(media.getLast().getPostingId());
            assertFalse(media.getLast().isEmbedded());
        });
    }

    @Test
    void cacheJobStoresSelectedChildWithoutLoadingPostings() {
        UUID childId = child(parentId, true);
        child(otherParentId, true);
        attach(true, false, 0);
        CacheMediaAttachmentsJob job = new CacheMediaAttachmentsJob();
        ReflectionTestUtils.setField(job, "parameters", new CacheMediaAttachmentsJob.Parameters(revisionId));
        ReflectionTestUtils.setField(job, "tx", tx);
        ReflectionTestUtils.setField(job, "entryRevisionRepository", entryRevisionRepository);
        ReflectionTestUtils.setField(job, "entryAttachmentRepository", entryAttachmentRepository);
        ReflectionTestUtils.setField(job, "directServeOperations", directServeOperations);
        ReflectionTestUtils.setField(job, "objectMapper", objectMapper);

        var statistics = factoryBean.getObject().unwrap(SessionFactory.class).getStatistics();
        statistics.clear();
        job.execute();

        assertEquals(0, statistics.getEntityStatistics(Posting.class.getName()).getLoadCount());
        transaction.executeWithoutResult(status -> {
            var cache = objectMapper.readValue(revision().getAttachmentsCache(), MediaAttachmentsCache.class);
            assertEquals(1, cache.getAttachments().size());
            assertEquals(childId.toString(), cache.getAttachments().getFirst().getPostingId());
            assertLocalPostingsNotLoaded();
        });
    }

    @Test
    void revisionBatchesUseTwoQueriesRegardlessOfSizeAndDoNotShareResponseObjects() {
        for (int size : List.of(1, 20)) {
            var fixtures = revisionBatch(size);
            transaction.executeWithoutResult(status -> {
                var revisions = fixtures.stream()
                    .map(fixture -> entityManager.find(EntryRevision.class, fixture.revisionId()))
                    .toList();
                var repeated = new ArrayList<>(revisions);
                repeated.add(revisions.getFirst());
                var statistics = factoryBean.getObject().unwrap(SessionFactory.class).getStatistics();
                statistics.clear();

                var provider = entryOperations.prepareMediaAttachments(repeated);
                for (int i = 0; i < size; i++) {
                    var attachments = provider.getMediaAttachments(revisions.get(i), null);
                    assertEquals(1, attachments.size());
                    assertEquals(fixtures.get(i).postingId().toString(), attachments.getFirst().getPostingId());
                    attachments.getFirst().setPostingId("changed by a different response");
                }
                assertEquals(
                    fixtures.getFirst().postingId().toString(),
                    provider.getMediaAttachments(revisions.getFirst(), null).getFirst().getPostingId()
                );
                assertEquals(2, statistics.getPrepareStatementCount());
                assertEquals(0, statistics.getEntityStatistics(Posting.class.getName()).getLoadCount());
                assertLocalPostingsNotLoaded();
                assertFalse(Hibernate.isInitialized(entityManager.find(RemoteMediaFile.class, remoteId).getPostings()));
            });
        }
    }

    @Test
    void mixedBatchUsesValidCacheAndFallsBackForInvalidAndWrongSourceCaches() {
        var fixtures = revisionBatch(4);
        transaction.executeWithoutResult(status -> {
            var revisions = fixtures.stream()
                .map(fixture -> entityManager.find(EntryRevision.class, fixture.revisionId()))
                .toList();
            revisions.get(0).setAttachmentsCache(cache("cached", null));
            revisions.get(1).setAttachmentsCache("invalid JSON");
            revisions.get(2).setAttachmentsCache(cache("wrong source", DirectServeSource.AWSS3));
            entityManager.flush();
            var statistics = factoryBean.getObject().unwrap(SessionFactory.class).getStatistics();
            statistics.clear();

            var provider = entryOperations.prepareMediaAttachments(revisions);

            var cached = provider.getMediaAttachments(revisions.get(0), null);
            assertEquals("cached", cached.getFirst().getPostingId());
            cached.getFirst().setPostingId("changed");
            assertEquals("cached", provider.getMediaAttachments(revisions.get(0), null).getFirst().getPostingId());
            for (int i = 1; i < revisions.size(); i++) {
                assertEquals(
                    fixtures.get(i).postingId().toString(),
                    provider.getMediaAttachments(revisions.get(i), null).getFirst().getPostingId()
                );
            }
            assertEquals(2, statistics.getPrepareStatementCount());
            assertFalse(Hibernate.isInitialized(revisions.get(0).getAttachments()));
        });
    }

    @Test
    void emptyAndCachedOnlyBatchesDoNotReadAttachments() {
        transaction.executeWithoutResult(status -> {
            var revision = revision();
            revision.setAttachmentsCache(cache("cached", null));
            entityManager.flush();
            var statistics = factoryBean.getObject().unwrap(SessionFactory.class).getStatistics();
            statistics.clear();

            entryOperations.prepareMediaAttachments(List.of());
            var provider = entryOperations.prepareMediaAttachments(List.of(revision));
            assertEquals("cached", provider.getMediaAttachments(revision, null).getFirst().getPostingId());
            assertEquals(0, statistics.getPrepareStatementCount());
        });
    }

    @Test
    void batchWithNoAttachmentsSkipsPostingIdQuery() {
        transaction.executeWithoutResult(status -> {
            var revision = revision();
            var statistics = factoryBean.getObject().unwrap(SessionFactory.class).getStatistics();
            statistics.clear();

            var provider = entryOperations.prepareMediaAttachments(List.of(revision));
            assertTrue(provider.getMediaAttachments(revision, null).isEmpty());
            assertTrue(provider.getMediaAttachments(revision, null).isEmpty());
            assertEquals(1, statistics.getPrepareStatementCount());
        });
    }

    @Test
    void repeatedRevisionBuildsIndependentGrantsFromDatabaseAndCache() {
        var fixtures = revisionBatch(2);
        for (boolean cached : List.of(false, true)) {
            transaction.executeWithoutResult(status -> {
                var revisions = fixtures.stream()
                    .map(fixture -> entityManager.find(EntryRevision.class, fixture.revisionId()))
                    .toList();
                if (cached) {
                    var loader = MediaAttachmentsProvider.database(entryAttachmentRepository, directServeOperations);
                    for (var revision : revisions) {
                        var cache = new MediaAttachmentsCache();
                        cache.setAttachments(loader.getMediaAttachments(revision, null));
                        revision.setAttachmentsCache(objectMapper.writeValueAsString(cache));
                    }
                    entityManager.flush();
                }
                var statistics = factoryBean.getObject().unwrap(SessionFactory.class).getStatistics();
                statistics.clear();
                var provider = entryOperations.prepareMediaAttachments(revisions);
                var firstLocal = provider.getMediaAttachments(revisions.get(0), grants("first")).getFirst();
                var secondLocal = provider.getMediaAttachments(revisions.get(0), grants("second")).getFirst();
                var firstRemote = provider.getMediaAttachments(revisions.get(1), grants("first")).getFirst();
                var secondRemote = provider.getMediaAttachments(revisions.get(1), grants("second")).getFirst();

                assertEquals("first", firstLocal.getMedia().getGrant());
                assertEquals("second", secondLocal.getMedia().getGrant());
                assertEquals("first", firstRemote.getRemoteMedia().getGrant());
                assertEquals("second", secondRemote.getRemoteMedia().getGrant());
                assertEquals(cached ? 0 : 2, statistics.getPrepareStatementCount());
            });
        }
    }

    @Test
    void jsonNullCacheFallsBackToDatabase() {
        attach(true, false, 0);
        transaction.executeWithoutResult(status -> {
            var revision = revision();
            revision.setAttachmentsCache("null");
            var provider = assertDoesNotThrow(() -> entryOperations.prepareMediaAttachments(List.of(revision)));
            assertEquals(1, provider.getMediaAttachments(revision, null).size());
        });
    }

    private MediaGrantSupplier grants(String grant) {
        var supplier = mock(MediaGrantSupplier.class);
        when(supplier.generateLocal(any(), any(), anyBoolean(), any())).thenReturn(grant);
        when(supplier.generateRemote(any(), any(), anyBoolean(), any())).thenReturn(grant);
        return supplier;
    }

    @Test
    void draftBatchPreservesNullParentPostingsOrderAndInvalidRemoteMedia() {
        UUID localChild = child(null, true);
        UUID remoteChild = child(null, false);
        child(parentId, true);
        child(parentId, false);
        var ids = transaction.execute(status -> {
            var first = draft();
            var local = new EntryAttachment(first, entityManager.getReference(MediaFileOwner.class, ownerId), 1);
            local.setEmbedded(false);
            entityManager.persist(local);
            var remote = new EntryAttachment(first, null, 0);
            remote.setRemoteMediaFile(entityManager.find(RemoteMediaFile.class, remoteId));
            remote.getRemoteMediaFile().setInvalid(true);
            entityManager.persist(remote);
            var second = draft();
            var downloaded = new EntryAttachment(second, entityManager.getReference(MediaFileOwner.class, ownerId), 0);
            downloaded.setRemoteMediaFile(remote.getRemoteMediaFile());
            entityManager.persist(downloaded);
            return List.of(first.getId(), second.getId(), draft().getId());
        });

        transaction.executeWithoutResult(status -> {
            var drafts = ids.stream().map(id -> entityManager.find(Draft.class, id)).toList();
            var statistics = factoryBean.getObject().unwrap(SessionFactory.class).getStatistics();
            statistics.clear();
            var provider = DraftAttachmentsProvider.database(entryAttachmentRepository, directServeOperations, drafts);
            var first = DraftInfoUtil.build(drafts.get(0), provider, directServeOperations, null).getMedia();
            var second = DraftInfoUtil.build(drafts.get(1), provider, directServeOperations, null).getMedia();

            assertEquals(2, first.size());
            assertEquals(remoteChild.toString(), first.get(0).getPostingId());
            assertEquals(localChild.toString(), first.get(1).getPostingId());
            assertFalse(first.get(1).isEmbedded());
            assertEquals(localChild.toString(), second.getFirst().getPostingId());
            assertTrue(provider.getMediaAttachments(drafts.get(2), null).isEmpty());
            assertEquals(2, statistics.getPrepareStatementCount());
            assertEquals(0, statistics.getEntityStatistics(Posting.class.getName()).getLoadCount());
            assertLocalPostingsNotLoaded();
            assertFalse(Hibernate.isInitialized(entityManager.find(RemoteMediaFile.class, remoteId).getPostings()));
        });
    }

    @Test
    void draftDoesNotFallBackToRemoteChildWhenLocalMediaHasOnlyParentSpecificChild() {
        child(parentId, true);
        child(null, false);
        UUID id = transaction.execute(status -> {
            var draft = draft();
            var attachment = new EntryAttachment(draft, entityManager.getReference(MediaFileOwner.class, ownerId), 0);
            attachment.setRemoteMediaFile(entityManager.getReference(RemoteMediaFile.class, remoteId));
            entityManager.persist(attachment);
            return draft.getId();
        });
        transaction.executeWithoutResult(status -> {
            var draft = entityManager.find(Draft.class, id);
            var provider = DraftAttachmentsProvider.database(
                entryAttachmentRepository, directServeOperations, List.of(draft)
            );
            assertNull(provider.getMediaAttachments(draft, null).getFirst().getPostingId());
            assertLocalPostingsNotLoaded();
        });
    }

    @Test
    void draftPaginationSelectsOnlyTheRequestedDraftBeforeFetchingAttachments() {
        transaction.executeWithoutResult(status -> {
            for (int i = 0; i < 3; i++) {
                entityManager.persist(new EntryAttachment(
                    draft(), entityManager.getReference(MediaFileOwner.class, ownerId), 0
                ));
            }
        });
        transaction.executeWithoutResult(status -> {
            var drafts = assertDoesNotThrow(() -> draftRepository.findAllNewPosting(
                nodeId, "owner", PageRequest.of(1, 1, Sort.by("id"))
            ));
            assertEquals(1, drafts.size());
            assertFalse(Hibernate.isInitialized(drafts.getFirst().getAttachments()));
            var provider = DraftAttachmentsProvider.database(entryAttachmentRepository, directServeOperations, drafts);
            assertEquals(1, provider.getMediaAttachments(drafts.getFirst(), null).size());
        });
    }

    @Test
    void draftReadEndpointsUseTheBatchLoader() {
        UUID child = child(null, true);
        transaction.executeWithoutResult(status -> {
            for (int i = 0; i < 3; i++) {
                entityManager.persist(new EntryAttachment(
                    draft(), entityManager.getReference(MediaFileOwner.class, ownerId), 0
                ));
            }
        });
        var context = mock(RequestContext.class);
        when(context.nodeId()).thenReturn(nodeId);
        when(context.nodeName()).thenReturn("owner");
        var controller = new DraftController();
        ReflectionTestUtils.setField(controller, "requestContext", context);
        ReflectionTestUtils.setField(controller, "draftRepository", draftRepository);
        ReflectionTestUtils.setField(controller, "entryAttachmentRepository", entryAttachmentRepository);
        ReflectionTestUtils.setField(controller, "directServeOperations", directServeOperations);
        String id = transaction.execute(status -> {
            var statistics = factoryBean.getObject().unwrap(SessionFactory.class).getStatistics();
            statistics.clear();
            var drafts = assertDoesNotThrow(() -> controller.getAll(DraftType.NEW_POSTING, "owner", null, null, 0, 2));
            assertEquals(2, drafts.size());
            assertEquals(child.toString(), drafts.getFirst().getMedia().getFirst().getPostingId());
            assertEquals(3, statistics.getPrepareStatementCount());
            assertEquals(2, statistics.getEntityStatistics(Draft.class.getName()).getLoadCount());
            assertLocalPostingsNotLoaded();
            return drafts.getFirst().getId();
        });
        transaction.executeWithoutResult(status -> {
            assertEquals(child.toString(), controller.get(id).getMedia().getFirst().getPostingId());
            assertLocalPostingsNotLoaded();
        });
    }

    @Test
    void writeResponseResolvesOnlyIdsForUnpersistedAttachmentsAndUsesCurrentMediaMetadata() {
        UUID localChild = child(parentId, true);
        UUID remoteChild = child(parentId, false);
        child(otherParentId, true);
        child(null, false);
        transaction.executeWithoutResult(status -> {
            var revision = revision();
            var owner = entityManager.find(MediaFileOwner.class, ownerId);
            Hibernate.initialize(owner.getMediaFile().getPreviews());
            owner.setTitle("Title changed in this transaction");
            revision.addAttachment(new EntryAttachment(revision, owner, 1));
            revision.addAttachment(new EntryAttachment(
                revision, null, entityManager.find(RemoteMediaFile.class, remoteId), 0
            ));
            var statistics = factoryBean.getObject().unwrap(SessionFactory.class).getStatistics();
            statistics.clear();

            var ids = MediaPostingIds.load(postingRepository, parentId, revision.getAttachments());
            var media = MediaAttachmentsProvider.relations(directServeOperations, ids)
                .getMediaAttachments(revision, null);

            assertEquals(remoteChild.toString(), media.getFirst().getPostingId());
            assertEquals(localChild.toString(), media.getLast().getPostingId());
            assertEquals("Title changed in this transaction", media.getLast().getMedia().getTitle());
            assertEquals(1, statistics.getPrepareStatementCount());
            assertEquals(0, statistics.getEntityStatistics(Posting.class.getName()).getLoadCount());
            assertLocalPostingsNotLoaded();
        });
    }

    @Test
    void draftWriteResponseDoesNotFlushPendingDraftOrAttachmentsToResolvePostingIds() {
        UUID childId = child(null, true);
        child(parentId, true);
        transaction.executeWithoutResult(status -> {
            var owner = entityManager.find(MediaFileOwner.class, ownerId);
            Hibernate.initialize(owner.getMediaFile().getPreviews());
            var draft = draft();
            var attachment = new EntryAttachment(draft, owner, 0);
            entityManager.persist(attachment);
            draft.addAttachment(attachment);
            var statistics = factoryBean.getObject().unwrap(SessionFactory.class).getStatistics();
            statistics.clear();

            var ids = MediaPostingIds.load(postingRepository, null, draft.getAttachments());
            var media = DraftAttachmentsProvider.relations(directServeOperations, ids).getMediaAttachments(draft, null);

            assertEquals(childId.toString(), media.getFirst().getPostingId());
            assertEquals(1, statistics.getPrepareStatementCount());
            assertEquals(0, statistics.getEntityInsertCount());
            assertEquals(0, statistics.getEntityStatistics(Posting.class.getName()).getLoadCount());
            assertLocalPostingsNotLoaded();
        });
    }

    @Test
    void postingCreationReturnsKnownIdsWithoutQueriesWhileBuildingTheResponse() {
        var operations = postingOperations();
        transaction.executeWithoutResult(status -> {
            var posting = posting();
            posting.setOwnerName("writer");
            var owner = entityManager.find(MediaFileOwner.class, ownerId);
            var remote = entityManager.find(RemoteMediaFile.class, remoteId);
            var result = operations.createOrUpdatePosting(
                posting,
                null,
                List.of(new LocalRemoteMedia(owner, null), new LocalRemoteMedia(null, remote)),
                null,
                null,
                r -> r.setBody("{\"text\":\"Attached media\"}"),
                null
            );
            Hibernate.initialize(owner.getMediaFile().getPreviews());
            var statistics = factoryBean.getObject().unwrap(SessionFactory.class).getStatistics();
            statistics.clear();

            var media = MediaAttachmentsProvider.relations(directServeOperations, result.mediaPostingIds())
                .getMediaAttachments(result.entry().getCurrentRevision(), null);

            assertEquals(2, media.size());
            assertNotNull(media.get(0).getPostingId());
            assertNotNull(media.get(1).getPostingId());
            assertNotEquals(media.get(0).getPostingId(), media.get(1).getPostingId());
            assertEquals(0, statistics.getPrepareStatementCount());
            for (var attachment : media) {
                var child = entityManager.find(Posting.class, UUID.fromString(attachment.getPostingId()));
                assertEquals(posting.getId(), child.getParentMediaEntry().getId());
            }
        });
    }

    @Test
    void postingRestorationReturnsKnownIdsForTheSelectedHistoricalRevision() {
        UUID childId = child(parentId, true);
        attach(true, false, 0);
        var operations = postingOperations();
        transaction.executeWithoutResult(status -> {
            var posting = entityManager.find(Posting.class, parentId);
            posting.setOwnerName("writer");
            var original = revision();
            original.setBody("{\"text\":\"Attached media\"}");
            original.setSignature(new byte[]{1});
            var owner = entityManager.find(MediaFileOwner.class, ownerId);

            var result = operations.createOrUpdatePosting(
                posting, original, List.of(new LocalRemoteMedia(owner, null)), null, null, null, null
            );

            assertNotEquals(original.getId(), result.entry().getCurrentRevision().getId());
            var provider = MediaAttachmentsProvider.relations(directServeOperations, result.mediaPostingIds());
            assertEquals(childId.toString(), provider.getMediaAttachments(original, null).getFirst().getPostingId());
            assertEquals(childId.toString(), provider.getMediaAttachments(
                result.entry().getCurrentRevision(), null
            ).getFirst().getPostingId());
        });
    }

    @Test
    void unchangedAndPickedPostingsResolveIdsWithoutLoadingPostingsCollections() {
        UUID childId = child(parentId, true);
        attach(true, false, 0);
        var operations = postingOperations();
        for (boolean picked : List.of(false, true)) {
            transaction.executeWithoutResult(status -> {
                var posting = entityManager.find(Posting.class, parentId);
                posting.setOwnerName("writer");
                if (picked) {
                    posting.setReceiverName("remote");
                }
                var revision = revision();
                var result = operations.createOrUpdatePosting(
                    posting, revision, List.of(), null, r -> true, null, null
                );

                assertEquals(revision.getId(), result.entry().getCurrentRevision().getId());
                var media = MediaAttachmentsProvider.relations(directServeOperations, result.mediaPostingIds())
                    .getMediaAttachments(revision, null);
                assertEquals(childId.toString(), media.getFirst().getPostingId());
                assertLocalPostingsNotLoaded();
            });
        }
    }

    @Test
    void commentCreationAndUnchangedUpdateReturnTheSameChildId() {
        var repositories = new JpaRepositoryFactory(entityManager);
        var operations = new CommentOperations();
        var context = mock(RequestContext.class);
        when(context.nodeName()).thenReturn("local");
        when(context.nodeId()).thenReturn(nodeId);
        ReflectionTestUtils.setField(operations, "requestContext", context);
        ReflectionTestUtils.setField(operations, "commentRepository", repositories.getRepository(CommentRepository.class));
        ReflectionTestUtils.setField(operations, "postingRepository", postingRepository);
        ReflectionTestUtils.setField(operations, "postingOperations", postingOperations());
        ReflectionTestUtils.setField(operations, "entryRevisionRepository", entryRevisionRepository);
        ReflectionTestUtils.setField(operations, "entryAttachmentRepository", entryAttachmentRepository);
        ReflectionTestUtils.setField(operations, "remoteMediaFileRepository",
            repositories.getRepository(RemoteMediaFileRepository.class));
        ReflectionTestUtils.setField(operations, "mediaOperations", mock(MediaOperations.class));
        ReflectionTestUtils.setField(operations, "jobs", mock(Jobs.class));
        transaction.executeWithoutResult(status -> {
            var parent = entityManager.find(Posting.class, parentId);
            var comment = new Comment();
            comment.setId(UUID.randomUUID());
            comment.setNodeId(nodeId);
            comment.setPosting(parent);
            comment.setOwnerName("writer");
            comment.setMoment(1L);
            entityManager.persist(comment);
            var media = List.of(new LocalRemoteMedia(entityManager.find(MediaFileOwner.class, ownerId), null));

            var created = operations.createOrUpdateComment(
                parent, comment, null, media, null, r -> r.setBody("{\"text\":\"Attached media\"}"), null
            );
            var revision = created.entry().getCurrentRevision();
            var first = MediaAttachmentsProvider.relations(directServeOperations, created.mediaPostingIds())
                .getMediaAttachments(revision, null).getFirst();
            assertNotNull(first.getPostingId());

            var unchanged = operations.createOrUpdateComment(parent, comment, revision, media, r -> true, null, null);
            var second = MediaAttachmentsProvider.relations(directServeOperations, unchanged.mediaPostingIds())
                .getMediaAttachments(revision, null).getFirst();
            assertEquals(first.getPostingId(), second.getPostingId());
        });
    }

    @Test
    void postingUpdateReusesTheStoredRemoteIdentityWhenValidationReturnsAFreshObject() {
        UUID childId = child(parentId, false);
        attach(false, true, 0);
        var operations = postingOperations();
        transaction.executeWithoutResult(status -> {
            var posting = entityManager.find(Posting.class, parentId);
            posting.setOwnerName("writer");
            var revision = revision();
            revision.setBody("{\"text\":\"Attached media\"}");
            revision.setSignature(new byte[]{1});
            var stored = entityManager.find(RemoteMediaFile.class, remoteId);
            var incoming = new RemoteMediaFile();
            incoming.setId(UUID.randomUUID());
            incoming.setNodeId(nodeId);
            incoming.setNodeName(stored.getNodeName());
            incoming.setMediaId(stored.getMediaId());
            incoming.setTitle("Updated remote title");

            var result = operations.createOrUpdatePosting(
                posting, revision, List.of(new LocalRemoteMedia(null, incoming)), null, null, null, null
            );

            var media = MediaAttachmentsProvider.relations(directServeOperations, result.mediaPostingIds())
                .getMediaAttachments(result.entry().getCurrentRevision(), null).getFirst();
            assertEquals(remoteId.toString(), media.getRemoteMedia().getId());
            assertEquals(childId.toString(), media.getPostingId());
            assertEquals("Updated remote title", media.getRemoteMedia().getTitle());
        });
    }

    @Test
    void restorationResponseUsesTheSelectedRevisionsRemoteIdentityWhenTheCurrentRevisionHasAnotherCopy() {
        UUID originalChildId = child(parentId, false);
        UUID selectedId = UUID.randomUUID();
        UUID currentRemoteId = transaction.execute(status -> {
            var parent = entityManager.find(Posting.class, parentId);
            parent.setOwnerName("writer");
            var originalRemote = entityManager.find(RemoteMediaFile.class, remoteId);
            var selected = new EntryRevision();
            selected.setId(selectedId);
            selected.setEntry(parent);
            selected.setBody("{\"text\":\"Selected revision\"}");
            selected.setBodySrc(selected.getBody());
            selected.setBodyPreview(selected.getBody());
            selected.setSignature(new byte[]{1});
            entityManager.persist(selected);
            entityManager.persist(new EntryAttachment(selected, null, originalRemote, 0));
            var currentRemote = remoteMedia();
            currentRemote.setNodeName(originalRemote.getNodeName());
            currentRemote.setMediaId(originalRemote.getMediaId());
            revision().setSignature(new byte[]{1});
            entityManager.persist(new EntryAttachment(revision(), null, currentRemote, 0));
            var child = posting();
            child.setParentMediaEntry(parent);
            child.setParentRemoteMedia(currentRemote);
            return currentRemote.getId();
        });
        var controller = new PostingRevisionController();
        var context = mock(RequestContext.class);
        when(context.nodeId()).thenReturn(nodeId);
        when(context.isPrincipal(any(), any())).thenReturn(true);
        ReflectionTestUtils.setField(controller, "requestContext", context);
        ReflectionTestUtils.setField(controller, "postingRepository", postingRepository);
        ReflectionTestUtils.setField(controller, "entryRevisionRepository", entryRevisionRepository);
        ReflectionTestUtils.setField(controller, "postingOperations", postingOperations());
        ReflectionTestUtils.setField(controller, "directServeOperations", directServeOperations);
        transaction.executeWithoutResult(status -> {
            var response = controller.restore(parentId, selectedId);

            assertEquals(selectedId.toString(), response.getId());
            assertEquals(remoteId.toString(), response.getMedia().getFirst().getRemoteMedia().getId());
            assertEquals(originalChildId.toString(), response.getMedia().getFirst().getPostingId());
            var current = entityManager.find(Posting.class, parentId).getCurrentRevision();
            assertEquals(currentRemoteId, current.getAttachments().iterator().next().getRemoteMediaFile().getId());
            assertFalse(Hibernate.isInitialized(entityManager.find(RemoteMediaFile.class, remoteId).getPostings()));
        });
    }

    private PostingOperations postingOperations() {
        var operations = new PostingOperations();
        var context = mock(UniversalContext.class);
        when(context.nodeName()).thenReturn("local");
        when(context.nodeId()).thenReturn(nodeId);
        ReflectionTestUtils.setField(operations, "universalContext", context);
        ReflectionTestUtils.setField(operations, "postingRepository", postingRepository);
        ReflectionTestUtils.setField(operations, "entryRevisionRepository", entryRevisionRepository);
        ReflectionTestUtils.setField(operations, "entryAttachmentRepository", entryAttachmentRepository);
        ReflectionTestUtils.setField(operations, "remoteMediaFileRepository",
            new JpaRepositoryFactory(entityManager).getRepository(RemoteMediaFileRepository.class));
        ReflectionTestUtils.setField(operations, "mediaOperations", mock(MediaOperations.class));
        ReflectionTestUtils.setField(operations, "storyOperations", mock(StoryOperations.class));
        ReflectionTestUtils.setField(operations, "jobs", mock(Jobs.class));
        return operations;
    }

    private Draft draft() {
        var draft = new Draft();
        draft.setId(UUID.randomUUID());
        draft.setNodeId(nodeId);
        draft.setDraftType(DraftType.NEW_POSTING);
        draft.setReceiverName("owner");
        draft.setBody("{}");
        draft.setBodySrc("{}");
        entityManager.persist(draft);
        return draft;
    }

    private String cache(String postingId, DirectServeSource source) {
        var attachment = new MediaAttachment();
        attachment.setPostingId(postingId);
        var cache = new MediaAttachmentsCache();
        cache.setAttachments(List.of(attachment));
        cache.setDirectServeSource(source);
        return objectMapper.writeValueAsString(cache);
    }

    private List<RevisionMedia> revisionBatch(int size) {
        return transaction.execute(status -> {
            List<RevisionMedia> fixtures = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                var parent = posting();
                var revision = new EntryRevision();
                revision.setId(UUID.randomUUID());
                revision.setEntry(parent);
                entityManager.persist(revision);
                parent.setCurrentRevision(revision);
                var owner = i % 2 == 0 ? entityManager.getReference(MediaFileOwner.class, ownerId) : null;
                var remote = i % 2 != 0 ? entityManager.getReference(RemoteMediaFile.class, remoteId) : null;
                entityManager.persist(new EntryAttachment(revision, owner, remote, 0));
                var child = posting();
                child.setParentMediaEntry(parent);
                child.setParentMedia(owner);
                child.setParentRemoteMedia(remote);
                fixtures.add(new RevisionMedia(revision.getId(), child.getId()));
            }
            return fixtures;
        });
    }

    private record RevisionMedia(UUID revisionId, UUID postingId) {
    }

    private EntryRevision revision() {
        return entityManager.find(EntryRevision.class, revisionId);
    }

    private Posting posting() {
        Posting posting = new Posting();
        posting.setId(UUID.randomUUID());
        posting.setNodeId(nodeId);
        entityManager.persist(posting);
        return posting;
    }

    private MediaFile mediaFile() {
        MediaFile file = new MediaFile();
        file.setId(UUID.randomUUID().toString());
        file.setMimeType("image/png");
        file.setSizeX(640);
        file.setSizeY(480);
        entityManager.persist(file);
        return file;
    }

    private RemoteMediaFile remoteMedia() {
        RemoteMediaFile remote = new RemoteMediaFile();
        remote.setId(UUID.randomUUID());
        remote.setNodeId(nodeId);
        remote.setNodeName("remote");
        remote.setMediaId(UUID.randomUUID().toString());
        remote.setMimeType("image/png");
        entityManager.persist(remote);
        return remote;
    }

    private UUID child(UUID parentEntryId, boolean local) {
        return transaction.execute(status -> {
            Posting child = posting();
            child.setParentMediaEntry(
                parentEntryId != null ? entityManager.getReference(Posting.class, parentEntryId) : null
            );
            if (local) {
                child.setParentMedia(entityManager.getReference(MediaFileOwner.class, ownerId));
            } else {
                child.setParentRemoteMedia(entityManager.getReference(RemoteMediaFile.class, remoteId));
            }
            return child.getId();
        });
    }

    private UUID attach(boolean local, boolean remote, int ordinal) {
        return transaction.execute(status -> {
            EntryAttachment attachment = new EntryAttachment(
                revision(),
                local ? entityManager.getReference(MediaFileOwner.class, ownerId) : null,
                remote ? entityManager.getReference(RemoteMediaFile.class, remoteId) : null,
                ordinal
            );
            entityManager.persist(attachment);
            return attachment.getId();
        });
    }

    private void assertLocalPostingsNotLoaded() {
        assertFalse(Hibernate.isInitialized(entityManager.find(MediaFileOwner.class, ownerId).getPostings()));
    }

}
