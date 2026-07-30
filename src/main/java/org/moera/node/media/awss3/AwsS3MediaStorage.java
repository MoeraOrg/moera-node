package org.moera.node.media.awss3;

import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Pattern;
import jakarta.annotation.PreDestroy;
import jakarta.inject.Inject;

import org.moera.node.config.Config;
import org.moera.node.config.DirectServeConfig;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.core.FileTransformerConfiguration;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Utilities;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

@Component
public class AwsS3MediaStorage {

    static class VoidFuture implements Future<Void> {

        private final Future<?> delegate;

        VoidFuture(Future<?> delegate) {
            this.delegate = delegate;
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            return delegate.cancel(mayInterruptIfRunning);
        }

        @Override
        public boolean isCancelled() {
            return delegate.isCancelled();
        }

        @Override
        public boolean isDone() {
            return delegate.isDone();
        }

        @Override
        public Void get() throws InterruptedException, ExecutionException {
            delegate.get();
            return null;
        }

        @Override
        public Void get(long timeout, TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {
            delegate.get(timeout, unit);
            return null;
        }

    }

    @Inject
    private Config config;

    private S3AsyncClient client;
    private S3Presigner presigner;
    private S3Utilities utilities;

    private DirectServeConfig serveConfig() {
        return config.getMedia().getDirectServe();
    }

    public boolean isConfigured() {
        return StringUtils.hasText(serveConfig().getBucket()) && StringUtils.hasText(serveConfig().getRegion());
    }

    public String configurationProblem() {
        if (!StringUtils.hasText(serveConfig().getBucket())) {
            return "node.media.direct-serve.bucket is empty";
        }
        if (!StringUtils.hasText(serveConfig().getRegion())) {
            return "node.media.direct-serve.region is empty";
        }
        return null;
    }

    public String bucket() {
        return serveConfig().getBucket();
    }

    AwsCredentialsProvider credentialsProvider() {
        return StringUtils.hasText(serveConfig().getProfile())
            ? ProfileCredentialsProvider.builder().profileName(serveConfig().getProfile()).build()
            : DefaultCredentialsProvider.builder().build();
    }

    private synchronized void initialize() {
        if (client != null) {
            return;
        }
        String problem = configurationProblem();
        if (problem != null) {
            throw new IllegalStateException(problem);
        }

        Region region = Region.of(serveConfig().getRegion());
        AwsCredentialsProvider credentialsProvider = credentialsProvider();
        client = S3AsyncClient.builder()
            .region(region)
            .credentialsProvider(credentialsProvider)
            .multipartEnabled(true)
            .build();
        presigner = S3Presigner.builder()
            .region(region)
            .credentialsProvider(credentialsProvider)
            .build();
        utilities = S3Utilities.builder().region(region).build();
    }

    public Future<Void> upload(Path path, String key, String mimeType, long contentLength) {
        initialize();
        var request = PutObjectRequest.builder()
            .bucket(serveConfig().getBucket())
            .key(key)
            .contentType(mimeType)
            .contentLength(contentLength)
            .build();
        return new VoidFuture(client.putObject(request, AsyncRequestBody.fromFile(path)));
    }

    public Future<Void> delete(String key) {
        initialize();
        var request = DeleteObjectRequest.builder().bucket(serveConfig().getBucket()).key(key).build();
        return new VoidFuture(client.deleteObject(request));
    }

    public Future<Void> download(String key, Path path) {
        initialize();
        var request = GetObjectRequest.builder().bucket(serveConfig().getBucket()).key(key).build();
        return new VoidFuture(client.getObject(
            request,
            AsyncResponseTransformer.toFile(path, FileTransformerConfiguration.defaultCreateOrReplaceExisting())
        ));
    }

    public PresignedPath presign(
        String key, Duration signatureDuration, String responseContentDisposition
    ) {
        initialize();
        var requestBuilder = GetObjectRequest.builder()
            .bucket(serveConfig().getBucket())
            .key(key)
            .responseCacheControl("private, max-age=" + signatureDuration.toSeconds());
        if (!ObjectUtils.isEmpty(responseContentDisposition)) {
            requestBuilder.responseContentDisposition(responseContentDisposition);
        }
        var request = GetObjectPresignRequest.builder()
            .signatureDuration(signatureDuration)
            .getObjectRequest(requestBuilder.build())
            .build();
        var presigned = presigner.presignGetObject(request);
        return new PresignedPath(presigned.url().toString(), presigned.expiration().getEpochSecond());
    }

    public Optional<S3Location> parseUri(URI uri) {
        initialize();
        var parsed = utilities.parseUri(uri);
        if (parsed.bucket().isEmpty() || parsed.key().isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new S3Location(parsed.bucket().get(), parsed.key().get()));
    }

    public String extractKeyForId(String url, String id) {
        if (!isConfigured()) {
            return null;
        }
        try {
            URI uri = URI.create(url);
            if (!Objects.equals(uri.getScheme(), "https") || uri.getUserInfo() != null || uri.getFragment() != null) {
                return null;
            }
            var location = parseUri(uri).orElse(null);
            if (location == null || !Objects.equals(location.bucket(), bucket())) {
                return null;
            }
            String keyPattern = Pattern.quote(id) + "_[0-9]+\\.[^./]+";
            return location.key().matches(keyPattern) ? location.key() : null;
        } catch (RuntimeException e) {
            return null;
        }
    }

    @PreDestroy
    public synchronized void close() {
        if (client != null) {
            client.close();
            client = null;
        }
        if (presigner != null) {
            presigner.close();
            presigner = null;
        }
        utilities = null;
    }

    public record PresignedPath(String url, long expires) {
    }

    public record S3Location(String bucket, String key) {
    }

}
