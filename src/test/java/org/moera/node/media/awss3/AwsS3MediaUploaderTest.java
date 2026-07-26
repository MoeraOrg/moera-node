package org.moera.node.media.awss3;

import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.moera.node.config.Config;
import org.moera.node.config.DirectServeSource;
import org.moera.node.data.CloudUploadClaim;
import org.moera.node.data.EntryRevisionRepository;
import org.moera.node.data.MediaFileRepository;
import org.moera.node.global.RequestCounter;
import org.moera.node.media.MediaOperations;
import org.moera.node.util.CallableNoExceptions;
import org.moera.node.util.Transaction;
import org.springframework.test.util.ReflectionTestUtils;

public class AwsS3MediaUploaderTest {

    @TempDir
    Path mediaPath;

    @Test
    void uploadIsIgnoredWhenAwsS3IsDisabled() {
        Config config = config();
        config.getMedia().getDirectServe().setSource(DirectServeSource.NONE);
        AwsS3MediaUploader operations = new AwsS3MediaUploader();
        ReflectionTestUtils.setField(operations, "config", config);

        operations.uploadMediaFiles();
    }

    @Test
    void successfulUploadPublishesKeyDeletesLocalFileAndInvalidatesCaches() throws Exception {
        successfulUploadPublishesKeyAndHandlesLocalFile(false);
    }

    @Test
    void successfulUploadRetainsExposedLocalFile() throws Exception {
        successfulUploadPublishesKeyAndHandlesLocalFile(true);
    }

    private void successfulUploadPublishesKeyAndHandlesLocalFile(boolean exposed) throws Exception {
        Files.writeString(mediaPath.resolve("persisted-name.jpg"), "content");
        Config config = config();
        AtomicBoolean inTransaction = new AtomicBoolean();
        AtomicBoolean uploadedOutsideTransaction = new AtomicBoolean();
        AtomicBoolean published = new AtomicBoolean();
        AtomicBoolean cacheCleared = new AtomicBoolean();
        Timestamp createdAt = Timestamp.from(Instant.ofEpochSecond(1784883600));
        Timestamp deadline = Timestamp.from(Instant.now().plusSeconds(900));
        AtomicInteger claims = new AtomicInteger();

        MediaFileRepository mediaFileRepository = (MediaFileRepository) Proxy.newProxyInstance(
            MediaFileRepository.class.getClassLoader(),
            new Class<?>[] {MediaFileRepository.class},
            (proxy, method, arguments) -> switch (method.getName()) {
                case "claimCloudUpload" -> claims.getAndIncrement() == 0
                    ? Optional.of(claim(deadline, createdAt, exposed))
                    : Optional.empty();
                case "completeCloudUpload" -> {
                    Assertions.assertTrue(inTransaction.get());
                    Assertions.assertEquals("hash_1784883600.jpg", arguments[2]);
                    Assertions.assertEquals(exposed, arguments[3]);
                    published.set(true);
                    yield 1;
                }
                case "toString" -> "MediaFileRepositoryTestProxy";
                default -> throw new UnsupportedOperationException(method.getName());
            }
        );
        EntryRevisionRepository entryRevisionRepository =
            (EntryRevisionRepository) Proxy.newProxyInstance(
                EntryRevisionRepository.class.getClassLoader(),
                new Class<?>[] {EntryRevisionRepository.class},
                (proxy, method, arguments) -> switch (method.getName()) {
                    case "clearAttachmentsCacheByMediaFile" -> {
                        Assertions.assertTrue(inTransaction.get());
                        cacheCleared.set(true);
                        yield null;
                    }
                    case "toString" -> "EntryRevisionRepositoryTestProxy";
                    default -> throw new UnsupportedOperationException(method.getName());
                }
            );
        AwsS3MediaStorage storage = new AwsS3MediaStorage() {
            @Override
            public CompletableFuture<Void> upload(Path path, String key, String mimeType, long contentLength) {
                uploadedOutsideTransaction.set(!inTransaction.get());
                Assertions.assertEquals(mediaPath.resolve("persisted-name.jpg"), path);
                Assertions.assertEquals("hash_1784883600.jpg", key);
                Assertions.assertEquals("image/jpeg", mimeType);
                Assertions.assertEquals(7L, contentLength);
                return CompletableFuture.completedFuture(null);
            }
        };
        ReflectionTestUtils.setField(storage, "config", config);

        AwsS3MediaUploader operations = operations(
            config, storage, mediaFileRepository, entryRevisionRepository, inTransaction
        );
        operations.uploadMediaFiles();

        Assertions.assertTrue(uploadedOutsideTransaction.get());
        Assertions.assertTrue(published.get());
        Assertions.assertTrue(cacheCleared.get());
        Assertions.assertEquals(exposed, Files.exists(mediaPath.resolve("persisted-name.jpg")));
    }

    private AwsS3MediaUploader operations(
        Config config,
        AwsS3MediaStorage storage,
        MediaFileRepository mediaFileRepository,
        EntryRevisionRepository entryRevisionRepository,
        AtomicBoolean inTransaction
    ) {
        MediaOperations mediaOperations = new MediaOperations();
        ReflectionTestUtils.setField(mediaOperations, "config", config);

        AwsS3MediaUploader operations = new AwsS3MediaUploader();
        ReflectionTestUtils.setField(operations, "config", config);
        ReflectionTestUtils.setField(operations, "awsS3MediaStorage", storage);
        ReflectionTestUtils.setField(operations, "mediaOperations", mediaOperations);
        ReflectionTestUtils.setField(operations, "mediaFileRepository", mediaFileRepository);
        ReflectionTestUtils.setField(operations, "entryRevisionRepository", entryRevisionRepository);
        ReflectionTestUtils.setField(operations, "requestCounter", new RequestCounter());
        ReflectionTestUtils.setField(operations, "tx", new Transaction() {
            @Override
            public <T> T executeWrite(CallableNoExceptions<T> inside) {
                Assertions.assertFalse(inTransaction.getAndSet(true));
                try {
                    return inside.call();
                } finally {
                    inTransaction.set(false);
                }
            }

            @Override
            public <T> T executeWriteWithExceptions(Callable<T> inside) throws Exception {
                Assertions.assertFalse(inTransaction.getAndSet(true));
                try {
                    return inside.call();
                } finally {
                    inTransaction.set(false);
                }
            }
        });
        return operations;
    }

    private Config config() {
        Config config = new Config();
        config.getMedia().setPath(mediaPath.toString());
        config.getMedia().getDirectServe().setSource(DirectServeSource.AWSS3);
        config.getMedia().getDirectServe().setBucket("media-bucket");
        config.getMedia().getDirectServe().setRegion("eu-central-1");
        return config;
    }

    private static CloudUploadClaim claim(Timestamp deadline, Timestamp createdAt, boolean exposed) {
        return new CloudUploadClaim(
            "hash",
            deadline.toLocalDateTime(),
            "persisted-name.jpg",
            "image/jpeg",
            7,
            createdAt.toLocalDateTime(),
            exposed
        );
    }

}
