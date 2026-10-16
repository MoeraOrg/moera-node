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
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.moera.lib.node.types.DraftText;
import org.moera.lib.node.types.DraftType;
import org.moera.lib.node.types.Scope;
import org.moera.lib.node.types.SourceFormat;
import org.moera.lib.node.types.body.Body;
import org.moera.node.data.Draft;
import org.moera.node.data.DraftRepository;
import org.moera.node.data.EntryAttachment;
import org.moera.node.data.EntryAttachmentRepository;
import org.moera.node.data.RemoteMediaFile;
import org.moera.node.data.RemoteMediaFileRepository;
import org.moera.node.global.RequestContext;
import org.moera.node.liberin.model.DraftDeletedLiberin;
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
            "hibernate.physical_naming_strategy", "org.hibernate.boot.model.naming.PhysicalNamingStrategySnakeCaseImpl"
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
        ReflectionTestUtils.setField(draftController, "entryAttachmentRepository",
            repositories.getRepository(EntryAttachmentRepository.class));
        ReflectionTestUtils.setField(draftController, "remoteMediaFileRepository",
            repositories.getRepository(RemoteMediaFileRepository.class));
        ReflectionTestUtils.setField(draftController, "mediaOperations", mediaOperations);
        ReflectionTestUtils.setField(draftController, "textConverter", textConverter);
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
