package org.moera.node.media.awss3;

import java.net.URI;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.moera.node.config.Config;
import org.moera.node.config.DirectServeConfig;
import org.moera.node.config.DirectServeSource;
import org.moera.node.data.CloudUploadClaim;
import org.moera.node.data.MediaFile;
import org.moera.node.media.DirectServeOperations;
import org.moera.node.util.ExtendedDuration;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;

public class AwsS3MediaTest {

    @Test
    void cloudFileNameIsStableAndUsesMimeExtension() {
        Timestamp createdAt = Timestamp.from(Instant.ofEpochSecond(1784883600));

        Assertions.assertEquals(
            "AbCdEf0123_1784883600.jpg",
            new CloudUploadClaim(
                "AbCdEf0123",
                createdAt.toLocalDateTime(),
                "file.jpg",
                "image/jpeg",
                0,
                createdAt.toLocalDateTime(),
                false
            )
                .getCloudFileName()
        );
    }

    @Test
    void profileCredentialsOverrideDefaultChain() {
        DirectServeConfig config = s3Config();
        config.setProfile("production");

        Assertions.assertInstanceOf(
            ProfileCredentialsProvider.class,
            storage(config).credentialsProvider()
        );
        config.setProfile(null);
        Assertions.assertInstanceOf(
            DefaultCredentialsProvider.class,
            storage(config).credentialsProvider()
        );
    }

    @Test
    void cancellingResultFutureCancelsSdkFuture() {
        CompletableFuture<Object> sdkFuture = new CompletableFuture<>();

        var future = (Future<Void>) new AwsS3MediaStorage.VoidFuture(sdkFuture);
        Assertions.assertTrue(future.cancel(true));

        Assertions.assertTrue(future.isCancelled());
        Assertions.assertTrue(sdkFuture.isCancelled());
    }

    @Test
    void s3PathsAreAbsentUntilUploadIsPublished() {
        DirectServeConfig config = s3Config();
        DirectServeOperations operations = operations(config, new FakeStorage());
        MediaFile mediaFile = mediaFile(null);

        Assertions.assertNull(operations.directPath(mediaFile, ExtendedDuration.ALWAYS).url());
    }

    @Test
    void s3DisplayAndDownloadPathsHaveIndependentSignedDisposition() {
        DirectServeConfig config = s3Config();
        FakeStorage storage = new FakeStorage();
        DirectServeOperations operations = operations(config, storage);
        MediaFile mediaFile = mediaFile("AbCdEf0123_1784883600.jpg");

        var display = operations.directPath(
            mediaFile, new ExtendedDuration(Duration.ofDays(3)), "quoted \" title.jpg"
        );
        Assertions.assertTrue(display.url().startsWith("https://media-bucket.s3.example/"));
        Assertions.assertTrue(storage.disposition.startsWith("inline;"));
        Assertions.assertTrue(storage.disposition.contains("filename*="));
        Assertions.assertEquals(Duration.ofDays(3), storage.duration);

        var download = operations.directDownloadPath(
            mediaFile, new ExtendedDuration(Duration.ofDays(3)), "Заголовок.jpg"
        );
        Assertions.assertNotEquals(display.url(), download.url());
        Assertions.assertTrue(storage.disposition.startsWith("attachment;"));
        Assertions.assertTrue(storage.disposition.contains("filename*="));
        Assertions.assertEquals(download.expires(), storage.expires);
    }

    @Test
    void s3PathLifetimeIsCappedAtSevenDays() {
        DirectServeConfig config = s3Config();
        FakeStorage storage = new FakeStorage();
        DirectServeOperations operations = operations(config, storage);

        operations.directPath(mediaFile("AbCdEf0123_1784883600.jpg"), ExtendedDuration.ALWAYS);

        Assertions.assertEquals(Duration.ofDays(7), storage.duration);
    }

    @Test
    void cachedS3PathIsRefreshedOnlyForExpectedBucketAndMediaId() {
        DirectServeConfig config = s3Config();
        FakeStorage storage = new FakeStorage();
        DirectServeOperations operations = operations(config, storage);
        String path = "https://media-bucket.s3.example/AbCdEf0123_1784883600.jpg?X-Amz-Signature=old";

        var refreshed = operations.refreshDirectDownloadPath(
            path, "AbCdEf0123", new ExtendedDuration(Duration.ofDays(3)), "new-name.jpg"
        );

        Assertions.assertNotNull(refreshed.url());
        Assertions.assertEquals("AbCdEf0123_1784883600.jpg", storage.key);
        Assertions.assertNull(operations.refreshDirectPath(
            path, "another-id", ExtendedDuration.ALWAYS
        ).url());
        Assertions.assertNull(operations.refreshDirectPath(
            path.replace("media-bucket", "another-bucket"), "AbCdEf0123", ExtendedDuration.ALWAYS
        ).url());
        Assertions.assertNull(operations.refreshDirectPath(
            path.replace("https:", "http:"), "AbCdEf0123", ExtendedDuration.ALWAYS
        ).url());
    }

    private static DirectServeConfig s3Config() {
        DirectServeConfig config = new DirectServeConfig();
        config.setSource(DirectServeSource.AWSS3);
        config.setBucket("media-bucket");
        config.setRegion("eu-central-1");
        return config;
    }

    private static DirectServeOperations operations(DirectServeConfig directServe, AwsS3MediaStorage storage) {
        Config config = rootConfig(directServe);
        DirectServeOperations operations = new DirectServeOperations();
        ReflectionTestUtils.setField(operations, "config", config);
        ReflectionTestUtils.setField(storage, "config", config);
        ReflectionTestUtils.setField(operations, "awsS3MediaStorage", storage);
        return operations;
    }

    private static AwsS3MediaStorage storage(DirectServeConfig directServeConfig) {
        AwsS3MediaStorage awsS3MediaStorage = new AwsS3MediaStorage();
        ReflectionTestUtils.setField(awsS3MediaStorage, "config", rootConfig(directServeConfig));
        return awsS3MediaStorage;
    }

    private static Config rootConfig(DirectServeConfig directServeConfig) {
        Config config = new Config();
        config.getMedia().setDirectServe(directServeConfig);
        return config;
    }

    private static MediaFile mediaFile(String cloudFileName) {
        MediaFile mediaFile = new MediaFile();
        mediaFile.setId("AbCdEf0123");
        mediaFile.setCloudFileName(cloudFileName);
        return mediaFile;
    }

    private static class FakeStorage extends AwsS3MediaStorage {

        private String key;
        private Duration duration;
        private String disposition;
        private Long expires;

        @Override
        public PresignedPath presign(
            String key, Duration signatureDuration, String responseContentDisposition
        ) {
            this.key = key;
            duration = signatureDuration;
            disposition = responseContentDisposition;
            expires = Instant.now().plus(signatureDuration).getEpochSecond();
            return new PresignedPath(
                "https://media-bucket.s3.example/" + key + "?variant="
                    + (responseContentDisposition != null ? responseContentDisposition.hashCode() : 0),
                expires
            );
        }

        @Override
        public Optional<S3Location> parseUri(URI uri) {
            String host = uri.getHost();
            String bucket = host != null ? host.substring(0, host.indexOf('.')) : null;
            String key = uri.getPath().startsWith("/") ? uri.getPath().substring(1) : uri.getPath();
            return Optional.of(new S3Location(bucket, key));
        }

    }

}
