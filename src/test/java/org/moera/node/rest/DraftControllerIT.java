package org.moera.node.rest;

import java.security.KeyPairGenerator;
import java.security.Security;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.sql.DataSource;
import jakarta.persistence.EntityManager;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.flywaydb.core.Flyway;
import org.hibernate.Hibernate;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.moera.lib.node.types.DraftText;
import org.moera.lib.node.types.DraftType;
import org.moera.lib.node.types.Scope;
import org.moera.lib.node.types.SourceFormat;
import org.moera.lib.node.types.body.Body;
import org.moera.node.data.Draft;
import org.moera.node.data.DraftRepository;
import org.moera.node.data.EntryAttachment;
import org.moera.node.data.EntryAttachmentRepository;
import org.moera.node.data.MediaFile;
import org.moera.node.data.MediaFileOwner;
import org.moera.node.data.Posting;
import org.moera.node.data.PostingRepository;
import org.moera.node.data.RemoteMediaFile;
import org.moera.node.data.RemoteMediaFileRepository;
import org.moera.node.global.RequestContext;
import org.moera.node.liberin.model.DraftDeletedLiberin;
import org.moera.node.media.DirectServeOperations;
import org.moera.node.media.DirectServeOperations.DirectServePath;
import org.moera.node.media.LocalRemoteMedia;
import org.moera.node.media.MediaOperations;
import org.moera.node.model.ObjectNotFoundFailure;
import org.moera.node.option.Options;
import org.moera.node.text.TextConverter;
import org.moera.node.util.ExtendedDuration;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

/** Applies all migrations to an isolated PostgreSQL database supplied by moera.test.database. */
@EnabledIfSystemProperty(named = "moera.test.database", matches = ".+")
class DraftControllerIT {

    private static LocalContainerEntityManagerFactoryBean factoryBean;
    private static DataSource dataSource;

    private EntityManager entityManager;
    private TransactionTemplate transaction;
    private DraftRepository draftRepository;
    private DraftController draftController;
    private RequestContext requestContext;
    private MediaOperations mediaOperations;
    private TextConverter textConverter;
    private UUID nodeId;
    private UUID draftId;

    @BeforeAll
    static void openDatabase() {
        Security.addProvider(new BouncyCastleProvider());
        dataSource = new DriverManagerDataSource(
            System.getProperty("moera.test.database"), System.getProperty("user.name"), ""
        );
        new JdbcTemplate(dataSource).execute("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\"");
        Flyway.configure().dataSource(dataSource).load().migrate();
        factoryBean = new LocalContainerEntityManagerFactoryBean();
        factoryBean.setDataSource(dataSource);
        factoryBean.setPackagesToScan("org.moera.node.data", "org.moera.node.model.principal");
        factoryBean.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        factoryBean.setJpaPropertyMap(Map.of(
            "hibernate.physical_naming_strategy", "org.hibernate.boot.model.naming.PhysicalNamingStrategySnakeCaseImpl",
            "hibernate.generate_statistics", true
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
    void setUp() throws Exception {
        var factory = factoryBean.getObject();
        entityManager = SharedEntityManagerCreator.createSharedEntityManager(factory);
        transaction = new TransactionTemplate(new JpaTransactionManager(factory));
        var repositories = new JpaRepositoryFactory(entityManager);
        draftRepository = repositories.getRepository(DraftRepository.class);
        nodeId = UUID.randomUUID();
        draftId = UUID.randomUUID();

        Options options = mock(Options.class);
        when(options.getInt("posting.max-size")).thenReturn(1024);
        when(options.getDuration(anyString())).thenReturn(ExtendedDuration.NEVER);
        when(options.getPrivateKey("profile.signing-key"))
            .thenReturn(KeyPairGenerator.getInstance("EC", "BC").generateKeyPair().getPrivate());
        when(options.mediaGrantSalt()).thenReturn(new byte[8]);
        requestContext = mock(RequestContext.class);
        when(requestContext.nodeId()).thenReturn(nodeId);
        when(requestContext.getOptions()).thenReturn(options);
        when(requestContext.isAdmin(Scope.VIEW_CONTENT)).thenReturn(true);
        mediaOperations = mock(MediaOperations.class);
        textConverter = mock(TextConverter.class);
        draftController = new DraftController();
        ReflectionTestUtils.setField(draftController, "requestContext", requestContext);
        ReflectionTestUtils.setField(draftController, "draftRepository", draftRepository);
        ReflectionTestUtils.setField(draftController, "postingRepository",
            repositories.getRepository(PostingRepository.class));
        ReflectionTestUtils.setField(draftController, "entryAttachmentRepository",
            repositories.getRepository(EntryAttachmentRepository.class));
        ReflectionTestUtils.setField(draftController, "remoteMediaFileRepository",
            repositories.getRepository(RemoteMediaFileRepository.class));
        ReflectionTestUtils.setField(draftController, "mediaOperations", mediaOperations);
        ReflectionTestUtils.setField(draftController, "textConverter", textConverter);
        var directServe = mock(DirectServeOperations.class);
        when(directServe.directPath(any(), any())).thenReturn(DirectServePath.NONE);
        when(directServe.directPath(any(), any(), any())).thenReturn(DirectServePath.NONE);
        when(directServe.directDownloadPath(any(), any(), any())).thenReturn(DirectServePath.NONE);
        ReflectionTestUtils.setField(draftController, "directServeOperations", directServe);
        transaction.executeWithoutResult(status -> {
            Draft draft = new Draft();
            draft.setId(draftId);
            draft.setNodeId(nodeId);
            draft.setDraftType(DraftType.NEW_POSTING);
            draft.setReceiverName("local");
            entityManager.persist(draft);
        });
    }

    @Test
    void deletedDraftIsNotFoundBeforeValidatingAttachments() {
        transaction.executeWithoutResult(status -> draftRepository.deleteById(draftId));
        when(mediaOperations.validateAttachments(any(), any(), anyBoolean(), anyBoolean()))
            .thenThrow(new ObjectNotFoundFailure("media.not-found"));

        ObjectNotFoundFailure failure = assertThrows(ObjectNotFoundFailure.class,
            () -> transaction.execute(status -> draftController.put(draftId.toString(), text())));

        assertEquals("draft.not-found", failure.getErrorCode());
    }

    @Test
    void draftFromAnotherNodeIsNotFound() {
        transaction.executeWithoutResult(status ->
            draftRepository.findById(draftId).orElseThrow().setNodeId(UUID.randomUUID())
        );

        ObjectNotFoundFailure failure = assertThrows(ObjectNotFoundFailure.class,
            () -> transaction.execute(status -> draftController.put(draftId.toString(), text())));

        assertEquals("draft.not-found", failure.getErrorCode());
    }

    @Test
    void deletionLocksDraftBeforeFlushingAttachmentRemoval() throws Exception {
        transaction.executeWithoutResult(status -> {
            Draft draft = draftRepository.findById(nodeId, draftId).orElseThrow();
            entityManager.persist(new EntryAttachment(draft, null, 0));
        });
        try (var executor = Executors.newSingleThreadExecutor()) {
            doAnswer(invocation -> {
                // The DELETE controller has queued cascading removals but has not committed yet.
                assertTrue(executor.submit(this::tryConcurrentDeletion).get(5, TimeUnit.SECONDS),
                    "Deletion must lock the draft before flushing attachment removals");
                return null;
            }).when(requestContext).send(any(DraftDeletedLiberin.class));

            transaction.executeWithoutResult(status -> draftController.delete(draftId.toString()));
        }
        transaction.executeWithoutResult(status -> assertTrue(draftRepository.findById(nodeId, draftId).isEmpty()));
    }

    @Test
    void concurrentDeletionCannotRemoveDraftWhileSavingAttachments() throws Exception {
        RemoteMediaFile remote = new RemoteMediaFile();
        remote.setId(UUID.randomUUID());
        remote.setNodeId(nodeId);
        remote.setNodeName("remote");
        remote.setMediaId(UUID.randomUUID().toString());
        remote.setDigest(new byte[32]);
        when(mediaOperations.validateAttachments(any(), any(), anyBoolean(), anyBoolean()))
            .thenReturn(List.of(new LocalRemoteMedia(null, remote)));
        AtomicBoolean deletionBlocked = new AtomicBoolean();
        try (var executor = Executors.newSingleThreadExecutor()) {
            when(textConverter.toHtml(any(SourceFormat.class), any(Body.class))).thenAnswer(invocation -> {
                // Delete after the draft was read, but before its new attachments are persisted.
                deletionBlocked.set(executor.submit(this::tryConcurrentDeletion).get(5, TimeUnit.SECONDS));
                return new Body("{\"text\":\"Saved draft\"}");
            });

            var info = transaction.execute(status -> draftController.put(draftId.toString(), text()));

            assertEquals(draftId.toString(), info.getId());
            assertEquals("Saved draft", info.getBody().getText());
            assertEquals(1, info.getMedia().size());
            assertTrue(deletionBlocked.get(), "Concurrent deletion must wait for draft saving to finish");
        }
        transaction.executeWithoutResult(status -> {
            Draft saved = draftRepository.findById(nodeId, draftId).orElseThrow();
            assertEquals("Saved draft", new Body(saved.getBody()).getText());
            assertEquals(1, saved.getAttachments().size());
            assertEquals(remote.getId(), saved.getAttachments().iterator().next().getRemoteMediaFile().getId());
        });
        // The lock is released after saving, so deletion can proceed normally.
        assertFalse(tryConcurrentDeletion());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void writeResponseUsesCurrentAttachmentsAndStandaloneChildIdsWithoutLoadingPostings(boolean create) {
        var ownerId = UUID.randomUUID();
        var remoteId = UUID.randomUUID();
        var localPostingId = UUID.randomUUID();
        var remotePostingId = UUID.randomUUID();
        transaction.executeWithoutResult(status -> {
            var file = new MediaFile();
            file.setId(UUID.randomUUID().toString());
            file.setMimeType("image/png");
            file.setDigest(new byte[32]);
            entityManager.persist(file);
            var owner = new MediaFileOwner();
            owner.setId(ownerId);
            owner.setNodeId(nodeId);
            owner.setMediaFile(file);
            entityManager.persist(owner);
            var remote = new RemoteMediaFile();
            remote.setId(remoteId);
            remote.setNodeId(nodeId);
            remote.setNodeName("remote");
            remote.setMediaId(UUID.randomUUID().toString());
            remote.setDigest(new byte[32]);
            remote.setInvalid(true);
            entityManager.persist(remote);
            var parent = new Posting();
            parent.setId(UUID.randomUUID());
            parent.setNodeId(nodeId);
            entityManager.persist(parent);
            for (boolean local : List.of(true, false)) {
                for (boolean standalone : List.of(true, false)) {
                    var posting = new Posting();
                    posting.setId(standalone ? (local ? localPostingId : remotePostingId) : UUID.randomUUID());
                    posting.setNodeId(nodeId);
                    posting.setParentMediaEntry(standalone ? null : parent);
                    posting.setParentMedia(local ? owner : null);
                    posting.setParentRemoteMedia(local ? null : remote);
                    entityManager.persist(posting);
                }
            }
        });
        when(textConverter.toHtml(any(SourceFormat.class), any(Body.class)))
            .thenReturn(new Body("{\"text\":\"Saved draft\"}"));
        transaction.executeWithoutResult(status -> {
            var owner = entityManager.find(MediaFileOwner.class, ownerId);
            owner.setTitle("Current local title");
            var remote = entityManager.find(RemoteMediaFile.class, remoteId);
            remote.setTitle("Current remote title");
            when(mediaOperations.validateAttachments(any(), any(), anyBoolean(), anyBoolean()))
                .thenReturn(List.of(new LocalRemoteMedia(owner, null), new LocalRemoteMedia(null, remote)));
            var statistics = factoryBean.getObject().unwrap(SessionFactory.class).getStatistics();
            statistics.clear();

            var info = create
                ? draftController.post(text()).getBody()
                : draftController.put(draftId.toString(), text());

            assertEquals("Saved draft", info.getBody().getText());
            assertEquals(2, info.getMedia().size());
            assertEquals(localPostingId.toString(), info.getMedia().get(0).getPostingId());
            assertEquals("Current local title", info.getMedia().get(0).getMedia().getTitle());
            assertEquals(remotePostingId.toString(), info.getMedia().get(1).getPostingId());
            assertEquals("Current remote title", info.getMedia().get(1).getRemoteMedia().getTitle());
            assertEquals(0, statistics.getEntityStatistics(Posting.class.getName()).getLoadCount());
            assertFalse(Hibernate.isInitialized(owner.getPostings()));
            assertFalse(Hibernate.isInitialized(remote.getPostings()));
        });
    }

    private boolean tryConcurrentDeletion() throws SQLException {
        try (var connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try (var statement = connection.createStatement()) {
                statement.execute("SET LOCAL lock_timeout = '500ms'");
                statement.executeUpdate("DELETE FROM drafts WHERE id = '" + draftId + "'");
                connection.commit();
                return false;
            } catch (SQLException e) {
                connection.rollback();
                if ("55P03".equals(e.getSQLState())) {
                    return true;
                }
                throw e;
            }
        }
    }

    private static DraftText text() {
        DraftText text = new DraftText();
        text.setDraftType(DraftType.NEW_POSTING);
        text.setReceiverName("local");
        text.setBodySrcFormat(SourceFormat.PLAIN_TEXT);
        text.setBodySrc(new Body("{\"text\":\"Saved draft\"}"));
        return text;
    }

}
