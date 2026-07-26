package org.moera.node.media.awss3;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import jakarta.inject.Inject;

import org.moera.node.config.Config;
import org.moera.node.config.DirectServeSource;
import org.moera.node.data.CloudUploadClaim;
import org.moera.node.data.EntryRevisionRepository;
import org.moera.node.data.MediaFileRepository;
import org.moera.node.global.RequestCounter;
import org.moera.node.media.MediaOperations;
import org.moera.node.util.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AwsS3MediaUploader {

    static final Duration MINIMUM_AGE = Duration.ofMinutes(30);
    static final Duration LEASE_DURATION = Duration.ofMinutes(15);
    static final Duration HEARTBEAT_INTERVAL = Duration.ofMinutes(5);
    static final int UPLOAD_BATCH_SIZE = 100;

    private static final Logger log = LoggerFactory.getLogger(AwsS3MediaUploader.class);

    private final AtomicBoolean uploading = new AtomicBoolean();

    @Inject
    private Config config;

    @Inject
    private AwsS3MediaStorage awsS3MediaStorage;

    @Inject
    private MediaOperations mediaOperations;

    @Inject
    private MediaFileRepository mediaFileRepository;

    @Inject
    private EntryRevisionRepository entryRevisionRepository;

    @Inject
    private Transaction tx;

    @Inject
    private RequestCounter requestCounter;

    @Scheduled(fixedDelayString = "PT5M")
    public void uploadMediaFiles() {
        if (config.getMedia().getDirectServe().getSource() != DirectServeSource.AWSS3) {
            return;
        }
        if (!uploading.compareAndSet(false, true)) {
            return;
        }

        int completed = 0;
        int failed = 0;
        try {
            try (var ignored = requestCounter.allot()) {
                if (!awsS3MediaStorage.isConfigured()) {
                    log.warn("AWS S3 media upload is unavailable: {}", awsS3MediaStorage.configurationProblem());
                    return;
                }

                for (int i = 0; i < UPLOAD_BATCH_SIZE; i++) {
                    CloudUploadClaim claim = claimNext();
                    if (claim == null) {
                        break;
                    }
                    if (upload(claim)) {
                        completed++;
                    } else {
                        failed++;
                    }
                }
                if (completed > 0 || failed > 0) {
                    log.info("AWS S3 media upload completed: {} uploaded, {} failed", completed, failed);
                }
            }
        } catch (RuntimeException e) {
            log.error("Error uploading media files to AWS S3", e);
        } finally {
            uploading.set(false);
        }
    }

    private CloudUploadClaim claimNext() {
        Instant now = Instant.now();
        Timestamp newDeadline = microsTimestamp(now.plus(LEASE_DURATION));
        return tx.executeWrite(() ->
            mediaFileRepository.claimCloudUpload(
                Timestamp.from(now),
                Timestamp.from(now.minus(MINIMUM_AGE)),
                newDeadline
            ).orElse(null)
        );
    }

    private boolean upload(CloudUploadClaim claim) {
        Path path;
        try {
            path = mediaOperations.getPath(claim.getId(), claim.getFileName());
        } catch (Exception e) {
            log.warn("Cannot resolve local media file {} for AWS S3 upload: {}", claim.getId(), e.getMessage());
            postpone(claim);
            return false;
        }

        Future<Void> future;
        try {
            future = awsS3MediaStorage.upload(
                path, claim.getCloudFileName(), claim.getMimeType(), claim.getFileSize()
            );
        } catch (RuntimeException e) {
            log.warn("Cannot start AWS S3 upload for media file {}: {}", claim.getId(), e.getMessage());
            postpone(claim);
            return false;
        }
        Timestamp deadline = claim.getDeadline();
        try {
            while (true) {
                try {
                    future.get(HEARTBEAT_INTERVAL.toSeconds(), TimeUnit.SECONDS);
                    break;
                } catch (TimeoutException e) {
                    Timestamp renewed = renew(claim.getId(), deadline);
                    if (renewed == null) {
                        future.cancel(true);
                        log.warn("Lost AWS S3 upload lease for media file {}", claim.getId());
                        return false;
                    }
                    deadline = renewed;
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            future.cancel(true);
            postpone(claim.withDeadline(deadline));
            return false;
        } catch (ExecutionException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            log.warn("AWS S3 upload failed for media file {}: {}", claim.getId(), cause.getMessage());
            postpone(claim.withDeadline(deadline));
            return false;
        } catch (RuntimeException e) {
            future.cancel(true);
            log.warn("Cannot maintain AWS S3 upload lease for media file {}: {}", claim.getId(), e.getMessage());
            return false;
        }

        return publish(claim, deadline, path);
    }

    private Timestamp renew(String id, Timestamp expectedDeadline) {
        Timestamp newDeadline = microsTimestamp(Instant.now().plus(LEASE_DURATION));
        int updated = tx.executeWrite(() ->
            mediaFileRepository.updateCloudUploadDeadline(id, expectedDeadline, newDeadline)
        );
        return updated > 0 ? newDeadline : null;
    }

    private void postpone(CloudUploadClaim claim) {
        Timestamp retryAt = microsTimestamp(Instant.now().plus(LEASE_DURATION));
        try {
            tx.executeWrite(() ->
                mediaFileRepository.updateCloudUploadDeadline(claim.getId(), claim.getDeadline(), retryAt)
            );
        } catch (RuntimeException e) {
            log.warn("Cannot postpone AWS S3 upload lease for media file {}: {}", claim.getId(), e.getMessage());
        }
    }

    private boolean publish(CloudUploadClaim claim, Timestamp deadline, Path path) {
        boolean published;
        try {
            published = tx.executeWriteWithExceptions(() -> {
                int updated = mediaFileRepository.completeCloudUpload(
                    claim.getId(), deadline, claim.getCloudFileName(), claim.isExposed()
                );
                if (updated == 0) {
                    return false;
                }
                entryRevisionRepository.clearAttachmentsCacheByMediaFile(claim.getId());
                if (!claim.isExposed()) {
                    Files.deleteIfExists(path);
                }
                return true;
            });
        } catch (Exception e) {
            log.warn("Cannot publish AWS S3 upload for media file {}: {}", claim.getId(), e.getMessage());
            postpone(claim.withDeadline(deadline));
            return false;
        }

        if (published) {
            log.debug("AWS S3 upload completed for media file {}", claim.getCloudFileName());
        } else {
            log.warn("AWS S3 upload completed after its lease was lost for media file {}", claim.getId());
        }

        return published;
    }

    /**
     * Converts an instant to PostgreSQL's microsecond timestamp precision. Lease updates retain the value passed to
     * the database as the fencing token for the next exact comparison, so removing unsupported nanoseconds ensures
     * that the local token is identical to the stored value.
     */
    private static Timestamp microsTimestamp(Instant instant) {
        return Timestamp.from(instant.truncatedTo(ChronoUnit.MICROS));
    }

}
