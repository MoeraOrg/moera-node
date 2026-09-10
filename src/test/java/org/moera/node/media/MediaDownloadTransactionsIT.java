package org.moera.node.media;

import java.io.ByteArrayInputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPairGenerator;
import java.security.Security;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import org.flywaydb.core.Flyway;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;
import org.moera.lib.http.Response;
import org.moera.lib.node.MoeraNode;
import org.moera.lib.node.NodeApiClient.ResponseConsumer;
import org.moera.lib.node.types.BodyFormat;
import org.moera.lib.node.types.MediaAttachment;
import org.moera.lib.node.types.MediaLeaseInfo;
import org.moera.lib.node.types.PostingInfo;
import org.moera.lib.node.types.PrivateMediaFileInfo;
import org.moera.lib.node.types.Scope;
import org.moera.lib.node.types.SourceFormat;
import org.moera.lib.node.types.body.Body;
import org.moera.node.api.node.NodeApi;
import org.moera.node.data.EntryAttachmentRepository;
import org.moera.node.data.EntryRevisionRepository;
import org.moera.node.data.EntrySourceRepository;
import org.moera.node.data.MediaFile;
import org.moera.node.data.MediaFileOwner;
import org.moera.node.data.MediaFileOwnerRepository;
import org.moera.node.data.MediaFilePreview;
import org.moera.node.data.MediaFileRepository;
import org.moera.node.data.Pick;
import org.moera.node.data.Posting;
import org.moera.node.data.PostingRepository;
import org.moera.node.data.ReactionTotalRepository;
import org.moera.node.data.RemoteMediaCacheRepository;
import org.moera.node.data.RemoteMediaError;
import org.moera.node.data.RemoteMediaFileRepository;
import org.moera.node.data.StoryRepository;
import org.moera.node.global.UniversalContext;
import org.moera.node.media.DirectServeOperations.DirectServePath;
import org.moera.node.operations.ReactionTotalOperations;
import org.moera.node.option.Options;
import org.moera.node.picker.Picker;
import org.moera.node.picker.PickerPool;
import org.moera.node.rest.task.RemoteMediaDownloadJob;
import org.moera.node.util.DigestingOutputStream;
import org.moera.node.util.Transaction;
import org.moera.node.util.Util;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.SharedEntityManagerCreator;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.moera.node.util.MediaTransactionTestSupport.assertInsideTransaction;
import static org.moera.node.util.MediaTransactionTestSupport.assertOutsideTransaction;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** Run against an isolated, empty PostgreSQL database; applies the project's migrations. */
@EnabledIfSystemProperty(named = "moera.test.database", matches = ".+")
class MediaDownloadTransactionsIT {

    private static LocalContainerEntityManagerFactoryBean factoryBean;
    private static EntityManagerFactory factory;

    @TempDir
    Path directory;

    private EntityManager entityManager;
    private Transaction tx;
    private UUID nodeId;
    private UniversalContext context;
    private MediaManager manager;
    private MediaOperations mediaOperations;
    private RemoteMediaCacheOperations cacheOperations;
    private RemoteMediaCacheRepository caches;
    private PostingRepository postings;
    private MediaFileOwnerRepository owners;
    private MoeraNode remote;
    private Picker picker;
    private PickerPool pool;
    private PostingInfo postingInfo;
    private Pick pick;
    private PrivateMediaFileInfo mediaInfo;
    private DirectServeOperations directServe;

    @BeforeAll
    static void openDatabase() {
        Security.addProvider(new BouncyCastleProvider());
        var dataSource = new DriverManagerDataSource(
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
        factory = factoryBean.getObject();
    }

    @AfterAll
    static void closeDatabase() {
        if (factoryBean != null) {
            factoryBean.destroy();
        }
    }

    @BeforeEach
    void setUp() throws Exception {
        entityManager = SharedEntityManagerCreator.createSharedEntityManager(factory);
        JpaRepositoryFactory repositories = new JpaRepositoryFactory(entityManager);
        postings = repositories.getRepository(PostingRepository.class);
        owners = repositories.getRepository(MediaFileOwnerRepository.class);
        caches = repositories.getRepository(RemoteMediaCacheRepository.class);
        nodeId = UUID.randomUUID();
        context = mock(UniversalContext.class);
        when(context.nodeId()).thenReturn(nodeId);
        when(context.nodeName()).thenReturn("local");
        Options options = mock(Options.class);
        when(options.nodeName()).thenReturn("local");
        when(options.getInt(anyString())).thenReturn(1024);
        when(options.getLong(anyString())).thenReturn(1024L);
        when(options.getPrivateKey("profile.signing-key"))
            .thenReturn(KeyPairGenerator.getInstance("EC").generateKeyPair().getPrivate());
        when(options.mediaGrantSalt()).thenReturn(new byte[8]);
        when(context.getOptions()).thenReturn(options);
        tx = new Transaction();
        ReflectionTestUtils.setField(tx, "txManager", new JpaTransactionManager(factory));
        ReflectionTestUtils.setField(tx, "universalContext", context);

        cacheOperations = new RemoteMediaCacheOperations();
        ReflectionTestUtils.setField(cacheOperations, "tx", tx);
        ReflectionTestUtils.setField(cacheOperations, "remoteMediaCacheRepository", caches);
        mediaOperations = mock(MediaOperations.class);
        when(mediaOperations.own(any(), any())).thenAnswer(invocation -> {
            assertInsideTransaction();
            MediaFileOwner owner = new MediaFileOwner();
            owner.setId(UUID.randomUUID());
            owner.setNodeId(nodeId);
            owner.setMediaFile(invocation.getArgument(0));
            owner.setTitle(invocation.getArgument(1));
            entityManager.persist(owner);
            return owner;
        });
        NodeApi nodeApi = mock(NodeApi.class);
        remote = mock(MoeraNode.class);
        when(nodeApi.at(eq("remote"), any())).thenReturn(remote);
        manager = new MediaManager();
        ReflectionTestUtils.setField(manager, "tx", tx);
        ReflectionTestUtils.setField(manager, "entityManager", entityManager);
        ReflectionTestUtils.setField(manager, "universalContext", context);
        ReflectionTestUtils.setField(manager, "nodeApi", nodeApi);
        ReflectionTestUtils.setField(manager, "mediaOperations", mediaOperations);
        ReflectionTestUtils.setField(manager, "mediaFileOwnerRepository", owners);
        ReflectionTestUtils.setField(manager, "mediaFileRepository", repositories.getRepository(MediaFileRepository.class));
        ReflectionTestUtils.setField(manager, "remoteMediaCacheRepository", caches);
        ReflectionTestUtils.setField(manager, "remoteMediaCacheOperations", cacheOperations);

        RemoteMediaOperations remoteMediaOperations = new RemoteMediaOperations();
        ReflectionTestUtils.setField(remoteMediaOperations, "universalContext", context);
        ReflectionTestUtils.setField(remoteMediaOperations, "remoteMediaFileRepository",
            repositories.getRepository(RemoteMediaFileRepository.class));
        directServe = mock(DirectServeOperations.class);
        when(directServe.directPath(any(), any())).thenReturn(DirectServePath.NONE);
        when(directServe.directPath(any(), any(), any())).thenReturn(DirectServePath.NONE);
        when(directServe.directDownloadPath(any(), any(), any())).thenReturn(DirectServePath.NONE);
        pool = mock(PickerPool.class);
        picker = new Picker(pool, "remote") {
            @Override
            protected String generateCarte(String targetNodeName, Scope clientScope) {
                return "carte";
            }
        };
        picker.setNodeId(nodeId);
        ReflectionTestUtils.setField(picker, "tx", tx);
        ReflectionTestUtils.setField(picker, "entityManager", entityManager);
        ReflectionTestUtils.setField(picker, "universalContext", context);
        ReflectionTestUtils.setField(picker, "nodeApi", nodeApi);
        ReflectionTestUtils.setField(picker, "mediaManager", manager);
        ReflectionTestUtils.setField(picker, "mediaOperations", mediaOperations);
        ReflectionTestUtils.setField(picker, "remoteMediaOperations", remoteMediaOperations);
        ReflectionTestUtils.setField(picker, "directServeOperations", directServe);
        ReflectionTestUtils.setField(picker, "postingRepository", postings);
        ReflectionTestUtils.setField(picker, "entryRevisionRepository",
            repositories.getRepository(EntryRevisionRepository.class));
        ReflectionTestUtils.setField(picker, "entryAttachmentRepository",
            repositories.getRepository(EntryAttachmentRepository.class));
        ReflectionTestUtils.setField(picker, "entrySourceRepository", repositories.getRepository(EntrySourceRepository.class));
        ReflectionTestUtils.setField(picker, "storyRepository", repositories.getRepository(StoryRepository.class));
        ReflectionTestUtils.setField(picker, "reactionTotalRepository",
            repositories.getRepository(ReactionTotalRepository.class));
        ReactionTotalOperations reactionTotals = mock(ReactionTotalOperations.class);
        when(reactionTotals.isSame(any(), any())).thenReturn(true);
        ReflectionTestUtils.setField(picker, "reactionTotalOperations", reactionTotals);

        postingInfo = new PostingInfo();
        postingInfo.setId(UUID.randomUUID().toString());
        postingInfo.setRevisionId(UUID.randomUUID().toString());
        postingInfo.setOwnerName("remote");
        postingInfo.setTotalComments(0);
        postingInfo.setCreatedAt(100L);
        postingInfo.setEditedAt(100L);
        postingInfo.setRevisionCreatedAt(100L);
        postingInfo.setBody(new Body("{\"text\":\"body\"}"));
        postingInfo.setBodyPreview(new Body("{\"text\":\"preview\"}"));
        postingInfo.setBodyFormat(BodyFormat.MESSAGE);
        postingInfo.setBodySrcFormat(SourceFormat.PLAIN_TEXT);
        postingInfo.setBodySrcHash(new byte[32]);
        postingInfo.setHeading("Heading");
        postingInfo.setDescription("");
        postingInfo.setMedia(List.of());
        when(remote.getPosting(postingInfo.getId(), false)).thenAnswer(invocation -> {
            assertOutsideTransaction();
            return postingInfo;
        });
        pick = new Pick();
        pick.setRemotePostingId(postingInfo.getId());
        pick.setRemoteFeedName("timeline");
        mediaInfo = cachedMedia();
    }

    private PrivateMediaFileInfo cachedMedia() {
        MediaFile file = new MediaFile();
        file.setId(UUID.randomUUID().toString());
        file.setMimeType("text/plain");
        file.setDigest(new byte[32]);
        file.setFileSize(4);
        tx.executeWrite(() -> entityManager.persist(file));
        PrivateMediaFileInfo info = new PrivateMediaFileInfo();
        info.setId(UUID.randomUUID().toString());
        info.setHash(file.getId());
        info.setDigest(Util.base64encode(file.getDigest()));
        info.setMimeType(file.getMimeType());
        info.setSize(4L);
        info.setTitle("Attachment");
        cacheOperations.store(null, "remote", info.getId(), file.getDigest(), file);
        return info;
    }

    private MediaAttachment attachment(PrivateMediaFileInfo info) {
        MediaAttachment attachment = new MediaAttachment();
        attachment.setMedia(info);
        return attachment;
    }

    private void downloadPick() {
        ReflectionTestUtils.invokeMethod(picker, "download", pick);
        assertOutsideTransaction();
    }

    private Posting posting() {
        return postings.findByReceiverId(nodeId, "remote", postingInfo.getId()).orElseThrow();
    }

    @Test
    void newPostingAndUpdateReuseAttachmentsWithFreshJpaEntities() {
        postingInfo.setMedia(List.of(attachment(mediaInfo)));
        downloadPick();
        UUID oldRevision = tx.executeRead(() -> posting().getCurrentRevision().getId());
        UUID owner = tx.executeRead(() -> posting().getCurrentRevision().getAttachments().iterator().next()
            .getMediaFileOwner().getId());
        PrivateMediaFileInfo second = cachedMedia();
        postingInfo.setEditedAt(200L);
        postingInfo.setRevisionId(UUID.randomUUID().toString());
        postingInfo.setMedia(List.of(attachment(mediaInfo), attachment(second)));

        downloadPick();

        tx.executeRead(() -> {
            Posting saved = posting();
            assertNotEquals(oldRevision, saved.getCurrentRevision().getId());
            assertEquals(2, saved.getTotalRevisions());
            assertEquals(2, saved.getCurrentRevision().getAttachments().size());
            assertTrue(saved.getCurrentRevision().getAttachments().stream()
                .anyMatch(attachment -> owner.equals(attachment.getMediaFileOwner().getId())));
            assertEquals(1, saved.getSources().size());
        });
    }

    @Test
    void finalFailureRollsBackPostingRevisionAttachmentsAndOwner() {
        postingInfo.setMedia(List.of(attachment(mediaInfo)));
        doAnswer(invocation -> {
            assertInsideTransaction();
            throw new IllegalStateException("Cannot update permissions");
        }).when(mediaOperations).updatePermissions(any(Posting.class));

        assertThrows(IllegalStateException.class, this::downloadPick);

        tx.executeRead(() -> {
            assertTrue(postings.findByReceiverId(nodeId, "remote", postingInfo.getId()).isEmpty());
            assertTrue(owners.findByFile(nodeId, mediaInfo.getHash()).isEmpty());
        });
        verify(context, never()).send(any());
        verifyNoInteractions(pool);
    }

    @Test
    void staleSnapshotDoesNotOverwriteConcurrentPostingUpdate() throws Exception {
        downloadPick();
        postingInfo.setEditedAt(200L);
        postingInfo.setRevisionId(UUID.randomUUID().toString());
        postingInfo.setMedia(List.of(attachment(mediaInfo)));
        MediaManager intercepted = mock(MediaManager.class);
        ReflectionTestUtils.setField(picker, "mediaManager", intercepted);
        when(intercepted.preparePrivateMedia(any(), any(), any(), anyInt(), any())).thenAnswer(invocation -> {
            assertOutsideTransaction();
            tx.executeWrite(() -> posting().setEditedAt(Util.toTimestamp(300L)));
            return manager.preparePrivateMedia("remote", "carte", mediaInfo, 1024, null);
        });

        assertThrows(RuntimeException.class, this::downloadPick);

        tx.executeRead(() -> {
            assertEquals(300L, Util.toEpochSecond(posting().getEditedAt()));
            assertEquals(1, posting().getTotalRevisions());
            assertTrue(owners.findByFile(nodeId, mediaInfo.getHash()).isEmpty());
        });
    }

    @Test
    void disappearedParentAbortsPick() {
        MediaFileOwner deletedParent = new MediaFileOwner();
        deletedParent.setId(UUID.randomUUID());
        pick.setMediaFileOwner(deletedParent);

        assertThrows(RuntimeException.class, this::downloadPick);

        tx.executeRead(() -> assertTrue(postings.findByReceiverId(nodeId, "remote", postingInfo.getId()).isEmpty()));
    }

    @Test
    void jobBuildsLazyPreviewsInFinalTransaction() {
        tx.executeWrite(() -> {
            MediaFile file = entityManager.find(MediaFile.class, mediaInfo.getHash());
            file.setSizeX(100);
            file.setSizeY(100);
            MediaFilePreview preview = new MediaFilePreview();
            preview.setId(UUID.randomUUID());
            preview.setOriginalMediaFile(file);
            preview.setMediaFile(file);
            preview.setWidth(100);
            entityManager.persist(preview);
        });
        RemoteMediaDownloadJob job = new RemoteMediaDownloadJob() {
            @Override
            protected String generateCarte(String targetNodeName, Scope clientScope) {
                return "carte";
            }
        };
        RemoteMediaDownloadJob.State state = new RemoteMediaDownloadJob.State();
        state.setMediaInfo(mediaInfo);
        ReflectionTestUtils.setField(job, "state", state);
        ReflectionTestUtils.setField(job, "parameters",
            new RemoteMediaDownloadJob.Parameters("remote", mediaInfo.getId(), null));
        ReflectionTestUtils.setField(job, "tx", tx);
        ReflectionTestUtils.setField(job, "mediaManager", manager);
        ReflectionTestUtils.setField(job, "directServeOperations", directServe);
        ReflectionTestUtils.setField(job, "universalContext", context);

        ReflectionTestUtils.invokeMethod(job, "execute");

        assertNotNull(state.getDownloadedMediaInfo());
        assertEquals(1, state.getDownloadedMediaInfo().getPreviews().size());
        assertOutsideTransaction();
    }

    @Test
    void hashMismatchStoresErrorAndNeverCreatesOwner() throws Exception {
        mediaInfo.setId(UUID.randomUUID().toString());
        Path temporary = directory.resolve("download");
        when(mediaOperations.tmpFile()).thenReturn(new TemporaryFile(temporary, Files.newOutputStream(temporary)));
        when(mediaOperations.transfer(any(), any(), any(), any())).thenAnswer(invocation -> {
            assertOutsideTransaction();
            var digest = new DigestingOutputStream(invocation.getArgument(1, OutputStream.class));
            digest.write("wrong data".getBytes(StandardCharsets.UTF_8));
            digest.close();
            return digest;
        });
        Response response = mock(Response.class);
        when(response.contentType()).thenReturn("text/plain");
        when(response.contentLength()).thenReturn(10L);
        when(response.bodyStream()).thenReturn(new ByteArrayInputStream(new byte[10]));
        doAnswer(invocation -> {
            assertOutsideTransaction();
            invocation.getArgument(5, ResponseConsumer.class).accept(response);
            return null;
        }).when(remote).getPrivateMedia(eq(mediaInfo.getId()), isNull(), isNull(), isNull(), isNull(), any());

        assertNull(manager.preparePrivateMedia("remote", "carte", mediaInfo, 1024, null));

        tx.executeRead(() -> {
            assertEquals(RemoteMediaError.DIGEST_INCORRECT,
                caches.findByMediaWithoutNode("remote", mediaInfo.getId()).iterator().next().getError());
            assertTrue(owners.findByFile(nodeId, mediaInfo.getHash()).isEmpty());
        });
        assertFalse(Files.exists(temporary));
    }

    @Test
    void leaseIsRequestedOutsideTransactionAndAttachedWithoutLocalOwner() throws Exception {
        mediaInfo.setSize(2048L);
        postingInfo.setMedia(List.of(attachment(mediaInfo)));
        MediaLeaseInfo lease = new MediaLeaseInfo();
        lease.setId("lease");
        when(remote.createMediaLease(any())).thenAnswer(invocation -> {
            assertOutsideTransaction();
            return lease;
        });

        downloadPick();

        tx.executeRead(() -> {
            var attachment = posting().getCurrentRevision().getAttachments().iterator().next();
            assertNull(attachment.getMediaFileOwner());
            assertEquals("lease", attachment.getRemoteMediaFile().getLeaseId());
        });
        verify(mediaOperations, never()).own(any(), any());
    }

    @Test
    void ownerReboundDuringPreparationIsNotUsedForWrongFile() throws Exception {
        MediaFileOwner oldOwner = tx.executeWriteWithExceptions(() ->
            mediaOperations.own(entityManager.find(MediaFile.class, mediaInfo.getHash()), "Old owner")
        );
        var prepared = manager.preparePrivateMedia("remote", "carte", mediaInfo, 1024, null);
        PrivateMediaFileInfo other = cachedMedia();
        tx.executeWrite(() -> entityManager.find(MediaFileOwner.class, oldOwner.getId())
            .setMediaFile(entityManager.find(MediaFile.class, other.getHash())));

        MediaFileOwner owner = tx.executeWriteWithExceptions(() -> manager.ownPreparedPrivateMedia(prepared, null));

        assertNotEquals(oldOwner.getId(), owner.getId());
        assertEquals(mediaInfo.getHash(), owner.getMediaFile().getId());
    }

    @Test
    void downloadedFileIsStreamedBeforePostingWriteTransaction() throws Exception {
        byte[] content = ("downloaded content " + UUID.randomUUID()).getBytes(StandardCharsets.UTF_8);
        var hash = new DigestingOutputStream(null);
        hash.write(content);
        mediaInfo.setId(UUID.randomUUID().toString());
        mediaInfo.setHash(hash.getHash());
        mediaInfo.setDigest(Util.base64encode(hash.getDigest()));
        mediaInfo.setSize((long) content.length);
        mediaInfo.setTextContent("Recognized text");
        postingInfo.setMedia(List.of(attachment(mediaInfo)));
        Path temporary = directory.resolve("download");
        when(mediaOperations.tmpFile()).thenReturn(new TemporaryFile(temporary, Files.newOutputStream(temporary)));
        when(mediaOperations.transfer(any(), any(), any(), any())).thenCallRealMethod();
        Response response = mock(Response.class);
        when(response.contentType()).thenReturn("text/plain");
        when(response.contentLength()).thenReturn((long) content.length);
        when(response.bodyStream()).thenReturn(new ByteArrayInputStream(content) {
            @Override
            public long transferTo(OutputStream out) throws java.io.IOException {
                assertOutsideTransaction();
                return super.transferTo(out);
            }
        });
        doAnswer(invocation -> {
            assertOutsideTransaction();
            invocation.getArgument(5, ResponseConsumer.class).accept(response);
            return null;
        }).when(remote).getPrivateMedia(eq(mediaInfo.getId()), isNull(), isNull(), isNull(), isNull(), any());
        when(mediaOperations.putInPlace(any(), any(), any(), any(), eq(false))).thenAnswer(invocation ->
            tx.executeWrite(() -> {
                MediaFile file = new MediaFile();
                file.setId(invocation.getArgument(0));
                file.setMimeType(invocation.getArgument(1));
                file.setFileSize(content.length);
                file.setDigest(Util.base64decode(mediaInfo.getDigest()));
                entityManager.persist(file);
                return file;
            })
        );

        downloadPick();

        tx.executeRead(() -> {
            var attachment = posting().getCurrentRevision().getAttachments().iterator().next();
            assertEquals(mediaInfo.getHash(), attachment.getMediaFileOwner().getMediaFile().getId());
            assertEquals("Recognized text", attachment.getMediaFileOwner().getMediaFile().getRecognizedText());
        });
        assertFalse(Files.exists(temporary));
    }

}
