package org.moera.node.media;

import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.moera.node.config.Config;
import org.moera.node.data.MediaFile;
import org.moera.node.data.MediaFileRemoval;
import org.moera.node.data.MediaFileRemovalRepository;
import org.moera.node.data.MediaFileRepository;
import org.moera.node.global.RequestCounter;
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
        MediaOperations operations = removalOperations(true, locked, removalDeleted);

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
        MediaOperations operations = removalOperations(false, locked, removalDeleted);

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
        MediaOperations operations = removalOperations(false, locked, removalDeleted);

        ReflectionTestUtils.invokeMethod(operations, "removeMediaFile", 1L);

        Assertions.assertTrue(locked.get());
        Assertions.assertTrue(Files.exists(directory));
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
        MediaOperations operations = mediaOperations(removalMediaFileRepository(false, new AtomicBoolean()));
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

    private MediaOperations removalOperations(
        boolean mediaExists, AtomicBoolean locked, AtomicBoolean removalDeleted
    ) {
        MediaFileRepository mediaFileRepository = removalMediaFileRepository(mediaExists, locked);
        MediaOperations operations = mediaOperations(mediaFileRepository);
        ReflectionTestUtils.setField(
            operations, "mediaFileRemovalRepository", mediaFileRemovalRepository(removalDeleted)
        );
        ReflectionTestUtils.setField(operations, "tx", directTransaction());
        return operations;
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
        MediaFileRemoval removal = new MediaFileRemoval();
        removal.setId(1L);
        removal.setMediaFileId("media-hash");
        removal.setFileName("stored-name.legacy");
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
