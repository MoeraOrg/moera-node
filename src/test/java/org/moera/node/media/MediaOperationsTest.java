package org.moera.node.media;

import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.moera.node.config.Config;
import org.moera.node.config.DirectServeSource;
import org.moera.node.data.MediaFile;
import org.moera.node.data.MediaFileRemoval;
import org.moera.node.data.MediaFileRemovalRepository;
import org.moera.node.data.MediaFileRepository;
import org.moera.node.global.RequestCounter;
import org.moera.node.media.awss3.AwsS3MediaStorage;
import org.moera.node.util.CallableNoExceptions;
import org.moera.node.util.CallableVoid;
import org.moera.node.util.Transaction;
import org.springframework.test.util.ReflectionTestUtils;

public class MediaOperationsTest {

    @TempDir
    Path mediaPath;

    @Test
    void newMediaFilePersistsGeneratedFileName() throws Exception {
        AtomicBoolean locked = new AtomicBoolean();
        MediaFileRepository repository = mediaFileRepository(null, new AtomicBoolean(), locked);
        MediaOperations operations = mediaOperations(repository);
        Path temporaryFile = mediaPath.resolve("temporary");
        Files.writeString(temporaryFile, "content");

        MediaFile mediaFile = operations.putInPlace(
            "media-hash", "text/markdown", temporaryFile, new byte[] {1}, false
        );

        Assertions.assertEquals("media-hash.md", mediaFile.getFileName());
        Assertions.assertTrue(locked.get());
        Assertions.assertTrue(Files.exists(mediaPath.resolve("media-hash.md")));
        Assertions.assertFalse(Files.exists(temporaryFile));
    }

    @Test
    void existingMediaFileDiscardsNewCopy() throws Exception {
        MediaFile existing = new MediaFile();
        existing.setId("media-hash");
        existing.setMimeType("text/markdown");
        existing.setFileName("original-name.legacy");
        AtomicBoolean saved = new AtomicBoolean();
        AtomicBoolean locked = new AtomicBoolean();
        MediaFileRepository repository = mediaFileRepository(existing, saved, locked);
        MediaOperations operations = mediaOperations(repository);
        Path temporaryFile = mediaPath.resolve("temporary");
        Files.writeString(temporaryFile, "content");

        MediaFile mediaFile = operations.putInPlace(
            "media-hash", "text/markdown", temporaryFile, new byte[] {1}, false
        );

        Assertions.assertSame(existing, mediaFile);
        Assertions.assertEquals("original-name.legacy", mediaFile.getFileName());
        Assertions.assertTrue(locked.get());
        Assertions.assertTrue(Files.exists(temporaryFile));
        Assertions.assertFalse(saved.get());
    }

    @Test
    void existingCloudOnlyMediaFileDoesNotRecreateLocalCopy() throws Exception {
        MediaFile existing = new MediaFile();
        existing.setId("media-hash");
        existing.setMimeType("text/markdown");
        existing.setCloudFileName("media-hash_1784883600.md");
        AtomicBoolean saved = new AtomicBoolean();
        AtomicBoolean locked = new AtomicBoolean();
        MediaFileRepository repository = mediaFileRepository(existing, saved, locked);
        MediaOperations operations = mediaOperations(repository);
        Path temporaryFile = mediaPath.resolve("temporary");
        Files.writeString(temporaryFile, "content");

        MediaFile mediaFile = operations.putInPlace(
            "media-hash", "text/markdown", temporaryFile, new byte[] {1}, false
        );

        Assertions.assertSame(existing, mediaFile);
        Assertions.assertNull(mediaFile.getFileName());
        Assertions.assertTrue(locked.get());
        Assertions.assertTrue(Files.exists(temporaryFile));
        Assertions.assertFalse(Files.exists(mediaPath.resolve("media-hash.md")));
        Assertions.assertFalse(saved.get());
    }

    @Test
    void cloudOnlyContentIsDownloadedToTemporaryFile() throws Exception {
        Files.createDirectory(mediaPath.resolve(MediaOperations.TMP_DIR));
        MediaOperations operations = new MediaOperations() {
            @Override
            public TemporaryFile tmpFile() {
                try {
                    Path path = mediaPath.resolve(MediaOperations.TMP_DIR).resolve("cloud-download");
                    return new TemporaryFile(path, Files.newOutputStream(path));
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            }
        };
        Config config = new Config();
        config.getMedia().setPath(mediaPath.toString());
        ReflectionTestUtils.setField(operations, "config", config);
        var storage = new AwsS3MediaStorage() {
            @Override
            public boolean isConfigured() {
                return true;
            }

            @Override
            public CompletableFuture<Void> download(String key, Path path) {
                Assertions.assertEquals("media-hash_1784883600.md", key);
                try {
                    Files.writeString(path, "cloud content");
                    return CompletableFuture.completedFuture(null);
                } catch (Exception e) {
                    return CompletableFuture.failedFuture(e);
                }
            }
        };
        ReflectionTestUtils.setField(operations, "awsS3MediaStorage", storage);
        MediaFile mediaFile = new MediaFile();
        mediaFile.setId("media-hash");
        mediaFile.setMimeType("text/markdown");
        mediaFile.setCloudFileName("media-hash_1784883600.md");

        Path temporaryPath;
        try (var content = operations.openContent(mediaFile)) {
            temporaryPath = content.path();
            Assertions.assertTrue(content.temporary());
            Assertions.assertEquals("cloud content", Files.readString(temporaryPath));
        }

        Assertions.assertFalse(Files.exists(temporaryPath));
    }

    @Test
    void pathIsUnavailableWithoutStoredFileName() {
        MediaOperations operations = mediaOperations(mediaFileRepository(null, new AtomicBoolean(), new AtomicBoolean()));
        MediaFile mediaFile = new MediaFile();
        mediaFile.setId("media-hash");

        Assertions.assertThrows(MediaFileNotAvailableException.class, () -> operations.getPath(mediaFile));
    }

    @Test
    void removalDoesNotDeleteFileWhenMediaIdWasRecreated() throws Exception {
        Path file = mediaPath.resolve("stored-name.legacy");
        Files.writeString(file, "content");
        AtomicBoolean locked = new AtomicBoolean();
        AtomicBoolean removalDeleted = new AtomicBoolean();
        MediaCleanupOperations operations = removalOperations(true, locked, removalDeleted);

        ReflectionTestUtils.invokeMethod(operations, "removeMediaFile", 1L);

        Assertions.assertTrue(locked.get());
        Assertions.assertTrue(Files.exists(file));
        Assertions.assertTrue(removalDeleted.get());
    }

    @Test
    void removalDeletesFileAndTombstoneWhenMediaIdIsAbsent() throws Exception {
        Path file = mediaPath.resolve("stored-name.legacy");
        Files.writeString(file, "content");
        AtomicBoolean locked = new AtomicBoolean();
        AtomicBoolean removalDeleted = new AtomicBoolean();
        MediaCleanupOperations operations = removalOperations(false, locked, removalDeleted);

        ReflectionTestUtils.invokeMethod(operations, "removeMediaFile", 1L);

        Assertions.assertTrue(locked.get());
        Assertions.assertFalse(Files.exists(file));
        Assertions.assertTrue(removalDeleted.get());
    }

    @Test
    void removalRetainsTombstoneWhenFileDeletionFails() throws Exception {
        Path directory = mediaPath.resolve("stored-name.legacy");
        Files.createDirectory(directory);
        Files.writeString(directory.resolve("child"), "content");
        AtomicBoolean locked = new AtomicBoolean();
        AtomicBoolean removalDeleted = new AtomicBoolean();
        MediaCleanupOperations operations = removalOperations(false, locked, removalDeleted);

        ReflectionTestUtils.invokeMethod(operations, "removeMediaFile", 1L);

        Assertions.assertTrue(locked.get());
        Assertions.assertTrue(Files.exists(directory));
        Assertions.assertFalse(removalDeleted.get());
    }

    @Test
    void recreatedMediaKeepsLocalFileButDeletesExactCloudObjectOutsideLock() throws Exception {
        Path file = mediaPath.resolve("stored-name.legacy");
        Files.writeString(file, "content");
        AtomicBoolean locked = new AtomicBoolean();
        AtomicBoolean localTransaction = new AtomicBoolean();
        AtomicBoolean cloudDeleted = new AtomicBoolean();
        AtomicBoolean removalDeleted = new AtomicBoolean();
        MediaCleanupOperations operations = removalOperations(true, locked, removalDeleted);
        enableAwsS3(operations);
        ReflectionTestUtils.setField(
            operations, "mediaFileRemovalRepository",
            mediaFileRemovalRepository(removalDeleted, "media-hash_1784883600.jpg")
        );
        ReflectionTestUtils.setField(operations, "tx", new Transaction() {
            @Override
            public void executeWriteWithExceptions(CallableVoid inside) throws Exception {
                localTransaction.set(true);
                try {
                    inside.call();
                } finally {
                    localTransaction.set(false);
                }
            }
        });
        var storage = new AwsS3MediaStorage() {
            @Override
            public CompletableFuture<Void> delete(String key) {
                Assertions.assertFalse(localTransaction.get());
                Assertions.assertEquals("media-hash_1784883600.jpg", key);
                cloudDeleted.set(true);
                return CompletableFuture.completedFuture(null);
            }
        };
        ReflectionTestUtils.setField(operations, "awsS3MediaStorage", storage);

        ReflectionTestUtils.invokeMethod(operations, "removeMediaFile", 1L);

        Assertions.assertTrue(locked.get());
        Assertions.assertTrue(Files.exists(file));
        Assertions.assertTrue(cloudDeleted.get());
        Assertions.assertTrue(removalDeleted.get());
    }

    @Test
    void cloudOnlyRemovalDoesNotAcquireMediaIdLock() {
        AtomicBoolean locked = new AtomicBoolean();
        AtomicBoolean cloudDeleted = new AtomicBoolean();
        AtomicBoolean removalDeleted = new AtomicBoolean();
        MediaCleanupOperations operations = removalOperations(false, locked, removalDeleted);
        enableAwsS3(operations);
        ReflectionTestUtils.setField(
            operations, "mediaFileRemovalRepository",
            mediaFileRemovalRepository(removalDeleted, null, "media-hash_1784883600.jpg")
        );
        var storage = new AwsS3MediaStorage() {
            @Override
            public CompletableFuture<Void> delete(String key) {
                cloudDeleted.set(true);
                return CompletableFuture.completedFuture(null);
            }
        };
        ReflectionTestUtils.setField(operations, "awsS3MediaStorage", storage);

        ReflectionTestUtils.invokeMethod(operations, "removeMediaFile", 1L);

        Assertions.assertFalse(locked.get());
        Assertions.assertTrue(cloudDeleted.get());
        Assertions.assertTrue(removalDeleted.get());
    }

    @Test
    void cloudDeletionFailureRetainsTombstone() {
        AtomicBoolean locked = new AtomicBoolean();
        AtomicBoolean removalDeleted = new AtomicBoolean();
        MediaCleanupOperations operations = removalOperations(false, locked, removalDeleted);
        enableAwsS3(operations);
        ReflectionTestUtils.setField(
            operations, "mediaFileRemovalRepository",
            mediaFileRemovalRepository(removalDeleted, null, "media-hash_1784883600.jpg")
        );
        var storage = new AwsS3MediaStorage() {
            @Override
            public CompletableFuture<Void> delete(String key) {
                return CompletableFuture.failedFuture(new IllegalStateException("S3 unavailable"));
            }
        };
        ReflectionTestUtils.setField(operations, "awsS3MediaStorage", storage);

        ReflectionTestUtils.invokeMethod(operations, "removeMediaFile", 1L);

        Assertions.assertFalse(locked.get());
        Assertions.assertFalse(removalDeleted.get());
    }

    @Test
    void cloudRemovalIsRetainedWhenAwsS3IsDisabled() {
        AtomicBoolean locked = new AtomicBoolean();
        AtomicBoolean cloudDeleted = new AtomicBoolean();
        AtomicBoolean removalDeleted = new AtomicBoolean();
        MediaCleanupOperations operations = removalOperations(false, locked, removalDeleted);
        ReflectionTestUtils.setField(
            operations, "mediaFileRemovalRepository",
            mediaFileRemovalRepository(removalDeleted, null, "media-hash_1784883600.jpg")
        );
        var storage = new AwsS3MediaStorage() {
            @Override
            public CompletableFuture<Void> delete(String key) {
                cloudDeleted.set(true);
                return CompletableFuture.completedFuture(null);
            }
        };
        ReflectionTestUtils.setField(operations, "awsS3MediaStorage", storage);

        ReflectionTestUtils.invokeMethod(operations, "removeMediaFile", 1L);

        Assertions.assertFalse(locked.get());
        Assertions.assertFalse(cloudDeleted.get());
        Assertions.assertFalse(removalDeleted.get());
    }

    @Test
    void scheduledRemovalDoesNotOverlap() throws Exception {
        AtomicInteger reads = new AtomicInteger();
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        Transaction transaction = new Transaction() {
            @Override
            public <T> T executeRead(CallableNoExceptions<T> inside) {
                reads.incrementAndGet();
                entered.countDown();
                try {
                    release.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(e);
                }
                return inside.call();
            }
        };
        MediaCleanupOperations operations = mediaCleanupOperations(
            removalMediaFileRepository(false, new AtomicBoolean())
        );
        ReflectionTestUtils.setField(
            operations, "mediaFileRemovalRepository", mediaFileRemovalRepository(new AtomicBoolean())
        );
        ReflectionTestUtils.setField(operations, "requestCounter", new RequestCounter());
        ReflectionTestUtils.setField(operations, "tx", transaction);

        Thread first = new Thread(operations::removeMediaFiles);
        first.start();
        try {
            Assertions.assertTrue(entered.await(5, TimeUnit.SECONDS));
            operations.removeMediaFiles();
            Assertions.assertEquals(1, reads.get());
        } finally {
            release.countDown();
            first.join();
        }

        operations.removeMediaFiles();
        Assertions.assertEquals(2, reads.get());
    }

    private MediaOperations mediaOperations(MediaFileRepository repository) {
        Config config = new Config();
        config.getMedia().setPath(mediaPath.toString());
        MediaOperations operations = new MediaOperations();
        ReflectionTestUtils.setField(operations, "config", config);
        ReflectionTestUtils.setField(operations, "mediaFileRepository", repository);
        return operations;
    }

    private MediaCleanupOperations mediaCleanupOperations(MediaFileRepository repository) {
        Config config = new Config();
        config.getMedia().setPath(mediaPath.toString());
        MediaCleanupOperations operations = new MediaCleanupOperations();
        ReflectionTestUtils.setField(operations, "config", config);
        ReflectionTestUtils.setField(operations, "mediaFileRepository", repository);
        return operations;
    }

    private MediaCleanupOperations removalOperations(
        boolean mediaExists, AtomicBoolean locked, AtomicBoolean removalDeleted
    ) {
        MediaFileRepository mediaFileRepository = removalMediaFileRepository(mediaExists, locked);
        MediaCleanupOperations operations = mediaCleanupOperations(mediaFileRepository);
        ReflectionTestUtils.setField(
            operations, "mediaFileRemovalRepository", mediaFileRemovalRepository(removalDeleted)
        );
        ReflectionTestUtils.setField(operations, "tx", directTransaction());
        return operations;
    }

    private static void enableAwsS3(MediaCleanupOperations operations) {
        Config config = (Config) ReflectionTestUtils.getField(operations, "config");
        config.getMedia().getDirectServe().setSource(DirectServeSource.AWSS3);
    }

    private static MediaFileRepository mediaFileRepository(
        MediaFile existing, AtomicBoolean saved, AtomicBoolean locked
    ) {
        return (MediaFileRepository) Proxy.newProxyInstance(
            MediaFileRepository.class.getClassLoader(),
            new Class<?>[] {MediaFileRepository.class},
            (proxy, method, arguments) -> switch (method.getName()) {
                case "lockMediaFileId" -> {
                    locked.set(true);
                    yield null;
                }
                case "findById" -> Optional.ofNullable(existing);
                case "save" -> {
                    saved.set(true);
                    yield arguments[0];
                }
                case "toString" -> "MediaFileRepositoryTestProxy";
                default -> throw new UnsupportedOperationException(method.getName());
            }
        );
    }

    private static MediaFileRepository removalMediaFileRepository(boolean mediaExists, AtomicBoolean locked) {
        return (MediaFileRepository) Proxy.newProxyInstance(
            MediaFileRepository.class.getClassLoader(),
            new Class<?>[] {MediaFileRepository.class},
            (proxy, method, arguments) -> switch (method.getName()) {
                case "lockMediaFileId" -> {
                    locked.set(true);
                    yield null;
                }
                case "countById" -> mediaExists ? 1L : 0L;
                case "toString" -> "MediaFileRepositoryTestProxy";
                default -> throw new UnsupportedOperationException(method.getName());
            }
        );
    }

    private static MediaFileRemovalRepository mediaFileRemovalRepository(AtomicBoolean removalDeleted) {
        return mediaFileRemovalRepository(removalDeleted, "stored-name.legacy", null);
    }

    private static MediaFileRemovalRepository mediaFileRemovalRepository(
        AtomicBoolean removalDeleted, String cloudFileName
    ) {
        return mediaFileRemovalRepository(removalDeleted, "stored-name.legacy", cloudFileName);
    }

    private static MediaFileRemovalRepository mediaFileRemovalRepository(
        AtomicBoolean removalDeleted, String fileName, String cloudFileName
    ) {
        MediaFileRemoval removal = new MediaFileRemoval();
        removal.setId(1L);
        removal.setMediaFileId("media-hash");
        removal.setFileName(fileName);
        removal.setCloudFileName(cloudFileName);
        return (MediaFileRemovalRepository) Proxy.newProxyInstance(
            MediaFileRemovalRepository.class.getClassLoader(),
            new Class<?>[] {MediaFileRemovalRepository.class},
            (proxy, method, arguments) -> switch (method.getName()) {
                case "findPendingIds" -> Collections.emptyList();
                case "findById" -> Optional.of(removal);
                case "deleteById" -> {
                    removalDeleted.set(true);
                    yield null;
                }
                case "toString" -> "MediaFileRemovalRepositoryTestProxy";
                default -> throw new UnsupportedOperationException(method.getName());
            }
        );
    }

    private static Transaction directTransaction() {
        return new Transaction() {
            @Override
            public void executeWriteWithExceptions(CallableVoid inside) throws Exception {
                inside.call();
            }
        };
    }

}
