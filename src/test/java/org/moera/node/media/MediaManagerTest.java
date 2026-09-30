package org.moera.node.media;

import java.lang.reflect.Proxy;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.moera.lib.node.exception.MoeraNodeException;
import org.moera.lib.node.types.PrivateMediaFileInfo;
import org.moera.node.data.MediaFile;
import org.moera.node.data.MediaFileOwner;
import org.moera.node.data.MediaFileOwnerRepository;
import org.moera.node.data.MediaFileRepository;
import org.moera.node.data.PendingJob;
import org.moera.node.data.RemoteMediaCache;
import org.moera.node.data.RemoteMediaCacheRepository;
import org.moera.node.data.RemoteMediaError;
import org.moera.node.api.node.MoeraNodeConcurrencyException;
import org.moera.node.media.video.VideoCompressionJob;
import org.moera.node.task.Job;
import org.moera.node.task.Jobs;
import org.moera.node.util.Transaction;
import org.moera.node.util.ParametrizedLock;
import org.moera.node.global.UniversalContext;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MediaManagerTest {

    @Test
    void disappearedPreparedFileIsConcurrencyError() {
        UUID nodeId = UUID.randomUUID();
        UniversalContext context = mock(UniversalContext.class);
        when(context.nodeId()).thenReturn(nodeId);
        MediaFileOwnerRepository owners = mock(MediaFileOwnerRepository.class);
        when(owners.findByFile(nodeId, "hash")).thenReturn(List.of());
        EntityManager entityManager = mock(EntityManager.class);
        MediaManager manager = new MediaManager();
        ReflectionTestUtils.setField(manager, "universalContext", context);
        ReflectionTestUtils.setField(manager, "mediaFileOwnerRepository", owners);
        ReflectionTestUtils.setField(manager, "entityManager", entityManager);

        Assertions.assertThrows(
            MoeraNodeConcurrencyException.class,
            () -> manager.ownPreparedPrivateMedia(
                new MediaManager.PreparedPrivateMedia("hash", "title", null), null
            )
        );
    }

    @Test
    @SuppressWarnings("unchecked")
    void existingOwnerIsReturnedDuringConcurrentDownload() throws Exception {
        UUID nodeId = UUID.randomUUID();
        UniversalContext context = mock(UniversalContext.class);
        when(context.nodeId()).thenReturn(nodeId);
        MediaFileOwner existing = new MediaFileOwner();
        MediaFileOwnerRepository owners = mock(MediaFileOwnerRepository.class);
        when(owners.findByFile(nodeId, "hash")).thenReturn(List.of(existing));
        MediaManager manager = new MediaManager();
        ReflectionTestUtils.setField(manager, "universalContext", context);
        ReflectionTestUtils.setField(manager, "mediaFileOwnerRepository", owners);
        var locks = (ParametrizedLock<String>) ReflectionTestUtils.getField(manager, "mediaFileLocks");
        CountDownLatch downloading = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        CompletableFuture<Void> download = CompletableFuture.runAsync(() -> {
            try (var ignored = locks.lock("hash")) {
                downloading.countDown();
                Assertions.assertTrue(release.await(10, TimeUnit.SECONDS));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(e);
            }
        });
        try {
            Assertions.assertTrue(downloading.await(5, TimeUnit.SECONDS));
            CompletableFuture<MediaFileOwner> result = CompletableFuture.supplyAsync(() -> {
                try {
                    return manager.ownPreparedPrivateMedia(
                        new MediaManager.PreparedPrivateMedia("hash", "title", null), null
                    );
                } catch (MoeraNodeException e) {
                    throw new java.util.concurrent.CompletionException(e);
                }
            });

            Assertions.assertSame(existing, result.get(1, TimeUnit.SECONDS));
        } finally {
            release.countDown();
            download.get(5, TimeUnit.SECONDS);
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void cachedMediaIsReturnedDuringConcurrentDownload() throws Exception {
        UUID nodeId = UUID.randomUUID();
        UniversalContext context = mock(UniversalContext.class);
        when(context.nodeId()).thenReturn(nodeId);
        MediaFileOwnerRepository owners = mock(MediaFileOwnerRepository.class);
        when(owners.findByFile(nodeId, "hash")).thenReturn(List.of());
        MediaFile file = new MediaFile();
        file.setId("hash");
        RemoteMediaCache cache = new RemoteMediaCache();
        cache.setMediaFile(file);
        RemoteMediaCacheRepository caches = mock(RemoteMediaCacheRepository.class);
        when(caches.findByMediaWithoutNode("remote", "media-id")).thenReturn(List.of(cache));
        MediaManager manager = new MediaManager();
        ReflectionTestUtils.setField(manager, "universalContext", context);
        ReflectionTestUtils.setField(manager, "mediaFileOwnerRepository", owners);
        ReflectionTestUtils.setField(manager, "remoteMediaCacheRepository", caches);
        ReflectionTestUtils.setField(manager, "tx", new Transaction());
        var locks = (ParametrizedLock<String>) ReflectionTestUtils.getField(manager, "mediaFileLocks");
        CountDownLatch downloading = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        CompletableFuture<Void> download = CompletableFuture.runAsync(() -> {
            try (var ignored = locks.lock("hash")) {
                downloading.countDown();
                Assertions.assertTrue(release.await(10, TimeUnit.SECONDS));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(e);
            }
        });
        try {
            Assertions.assertTrue(downloading.await(5, TimeUnit.SECONDS));
            PrivateMediaFileInfo info = new PrivateMediaFileInfo();
            info.setId("media-id");
            info.setHash("hash");
            CompletableFuture<MediaManager.PreparedPrivateMedia> result = CompletableFuture.supplyAsync(() -> {
                try {
                    return manager.preparePrivateMedia("remote", "carte", info, 1024, null);
                } catch (MoeraNodeException e) {
                    throw new java.util.concurrent.CompletionException(e);
                }
            });

            MediaManager.PreparedPrivateMedia prepared = result.get(1, TimeUnit.SECONDS);
            Assertions.assertEquals("hash", prepared.mediaFileId());
        } finally {
            release.countDown();
            download.get(5, TimeUnit.SECONDS);
        }
    }

    private static class RecordingJobs extends Jobs {

        private final UUID id = UUID.randomUUID();
        private int calls;

        @Override
        public <P, T extends Job<P, ?>> UUID runAfterCommit(Class<T> klass, P parameters) {
            Assertions.assertEquals(VideoCompressionJob.class, klass);
            calls++;
            return id;
        }

    }

    @Test
    void cachedRemoteMediaErrorIsThrown() {
        RemoteMediaCache cache = new RemoteMediaCache();
        cache.setError(RemoteMediaError.DOWNLOAD_FAILED);

        RemoteMediaCacheRepository remoteMediaCacheRepository = (RemoteMediaCacheRepository) Proxy.newProxyInstance(
            RemoteMediaCacheRepository.class.getClassLoader(),
            new Class<?>[] {RemoteMediaCacheRepository.class},
            (proxy, method, args) -> method.getName().equals("findByMediaWithoutNode") ? List.of(cache) : null
        );

        MediaManager mediaManager = new MediaManager();
        ReflectionTestUtils.setField(mediaManager, "tx", new Transaction());
        ReflectionTestUtils.setField(mediaManager, "remoteMediaCacheRepository", remoteMediaCacheRepository);

        PrivateMediaFileInfo info = new PrivateMediaFileInfo();
        info.setId("media-id");
        info.setHash("media-hash");

        MoeraNodeException exception = Assertions.assertThrows(
            MoeraNodeException.class,
            () -> mediaManager.downloadPrivateMediaForCaching("remote", null, info, 1024)
        );

        Assertions.assertTrue(exception.getMessage().contains(RemoteMediaError.DOWNLOAD_FAILED.getErrorCode()));
    }

    @Test
    void existingCompressedFileRebindsNewOwnerWithoutCreatingJob() {
        MediaFile compressed = mediaFile("compressed", false);
        MediaFile original = mediaFile("original", true);
        original.setCompressedFile(compressed);
        MediaFileOwner owner = owner(original);
        RecordingJobs jobs = new RecordingJobs();
        MediaManager mediaManager = mediaManager(original, jobs, null);

        ReflectionTestUtils.invokeMethod(mediaManager, "prepareDownsize", owner, true);

        Assertions.assertTrue(owner.isDownsize());
        Assertions.assertSame(compressed, owner.getMediaFile());
        Assertions.assertEquals(0, jobs.calls);
    }

    @Test
    void uncompressedFileGetsOneGlobalJobAndRevokesCloudUploadClaim() {
        MediaFile original = mediaFile("original", true);
        original.setCloudUploadDeadline(Timestamp.from(Instant.now()));
        MediaFileOwner owner = owner(original);
        RecordingJobs jobs = new RecordingJobs();
        PendingJob pendingJob = new PendingJob();
        pendingJob.setId(jobs.id);
        EntityManager entityManager = (EntityManager) Proxy.newProxyInstance(
            EntityManager.class.getClassLoader(),
            new Class<?>[] {EntityManager.class},
            (proxy, method, args) -> method.getName().equals("getReference") ? pendingJob : null
        );
        MediaManager mediaManager = mediaManager(original, jobs, entityManager);

        ReflectionTestUtils.invokeMethod(mediaManager, "prepareDownsize", owner, true);

        Assertions.assertTrue(owner.isDownsize());
        Assertions.assertSame(original, owner.getMediaFile());
        Assertions.assertSame(pendingJob, original.getCompressionJob());
        Assertions.assertNull(original.getCloudUploadDeadline());
        Assertions.assertEquals(1, jobs.calls);
    }

    @Test
    void activeCompressionJobIsReused() {
        MediaFile original = mediaFile("original", true);
        PendingJob pendingJob = new PendingJob();
        pendingJob.setId(UUID.randomUUID());
        original.setCompressionJob(pendingJob);
        MediaFileOwner owner = owner(original);
        RecordingJobs jobs = new RecordingJobs();
        MediaManager mediaManager = mediaManager(original, jobs, null);

        ReflectionTestUtils.invokeMethod(mediaManager, "prepareDownsize", owner, true);

        Assertions.assertSame(pendingJob, original.getCompressionJob());
        Assertions.assertEquals(0, jobs.calls);
    }

    private static MediaManager mediaManager(MediaFile file, Jobs jobs, EntityManager entityManager) {
        MediaFileRepository mediaFileRepository = (MediaFileRepository) Proxy.newProxyInstance(
            MediaFileRepository.class.getClassLoader(),
            new Class<?>[] {MediaFileRepository.class},
            (proxy, method, args) -> method.getName().equals("findByIdForUpdate") ? Optional.of(file) : null
        );
        MediaManager mediaManager = new MediaManager();
        ReflectionTestUtils.setField(mediaManager, "mediaFileRepository", mediaFileRepository);
        ReflectionTestUtils.setField(mediaManager, "jobs", jobs);
        ReflectionTestUtils.setField(mediaManager, "entityManager", entityManager);
        return mediaManager;
    }

    private static MediaFile mediaFile(String id, boolean uncompressed) {
        MediaFile file = new MediaFile();
        file.setId(id);
        file.setMimeType("video/mp4");
        file.setUncompressed(uncompressed);
        return file;
    }

    private static MediaFileOwner owner(MediaFile file) {
        MediaFileOwner owner = new MediaFileOwner();
        owner.setMediaFile(file);
        return owner;
    }

}
