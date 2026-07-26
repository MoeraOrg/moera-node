package org.moera.node.media;

import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import jakarta.inject.Inject;

import org.moera.node.config.Config;
import org.moera.node.config.DirectServeSource;
import org.moera.node.data.MediaFileOwnerRepository;
import org.moera.node.data.MediaFileRemoval;
import org.moera.node.data.MediaFileRemovalRepository;
import org.moera.node.data.MediaFileRepository;
import org.moera.node.data.MediaLeaseRepository;
import org.moera.node.global.RequestCounter;
import org.moera.node.media.awss3.AwsS3MediaStorage;
import org.moera.node.util.Transaction;
import org.moera.node.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class MediaCleanupOperations {

    public static final Duration DRAFT_ONLY_LEASE_TTL = Duration.ofDays(1);

    private static final Logger log = LoggerFactory.getLogger(MediaCleanupOperations.class);

    private static final int MEDIA_FILE_PURGE_BATCH_SIZE = 1024;
    private static final int MEDIA_FILE_REMOVAL_BATCH_SIZE = 100;

    private final AtomicBoolean removingMediaFiles = new AtomicBoolean();

    @Inject
    private RequestCounter requestCounter;

    @Inject
    private Config config;

    @Inject
    private MediaFileRepository mediaFileRepository;

    @Inject
    private MediaFileRemovalRepository mediaFileRemovalRepository;

    @Inject
    private AwsS3MediaStorage awsS3MediaStorage;

    @Inject
    private MediaFileOwnerRepository mediaFileOwnerRepository;

    @Inject
    private MediaLeaseRepository mediaLeaseRepository;

    @Inject
    private Transaction tx;

    @Scheduled(fixedDelayString = "PT6H")
    public void purgeUnused() {
        try (var ignored = requestCounter.allot()) {
            log.info("Purging unused media files");

            Timestamp now = Util.now();
            tx.executeWrite(() -> mediaFileOwnerRepository.deleteUnused(now));
            int moved;
            do {
                moved = tx.executeWrite(() ->
                    mediaFileRepository.moveUnusedToRemovals(now, MEDIA_FILE_PURGE_BATCH_SIZE)
                );
            } while (moved == MEDIA_FILE_PURGE_BATCH_SIZE);
        }
    }

    @Scheduled(fixedDelayString = "PT30M")
    public void removeMediaFiles() {
        if (!removingMediaFiles.compareAndSet(false, true)) {
            return;
        }

        try {
            try (var ignored = requestCounter.allot()) {
                log.info("Removing purged media files");

                List<Long> removalIds = tx.executeRead(() ->
                    mediaFileRemovalRepository.findPendingIds(Pageable.ofSize(MEDIA_FILE_REMOVAL_BATCH_SIZE))
                );
                removalIds.forEach(this::removeMediaFile);
            }
        } finally {
            removingMediaFiles.set(false);
        }
    }

    private void removeMediaFile(long removalId) {
        try {
            MediaFileRemoval removal = tx.executeRead(() ->
                mediaFileRemovalRepository.findById(removalId).orElse(null)
            );
            if (removal == null) {
                return;
            }

            if (removal.getFileName() != null) {
                tx.executeWriteWithExceptions(() -> {
                    mediaFileRepository.lockMediaFileId(removal.getMediaFileId());
                    if (mediaFileRepository.countById(removal.getMediaFileId()) > 0) {
                        return;
                    }
                    Path path = FileSystems.getDefault().getPath(
                        config.getMedia().getPath(), removal.getFileName()
                    );
                    Files.deleteIfExists(path);
                });
            }

            if (removal.getCloudFileName() != null) {
                if (config.getMedia().getDirectServe().getSource() != DirectServeSource.AWSS3) {
                    return;
                }
                awsS3MediaStorage.delete(removal.getCloudFileName()).get();
                log.debug("AWS S3 removal completed for media file {}", removal.getCloudFileName());
            }

            tx.executeWrite(() -> mediaFileRemovalRepository.deleteById(removal.getId()));
        } catch (Exception e) {
            log.warn("Error removing media file {}: {}", removalId, e.getMessage());
        }
    }

    @Scheduled(fixedDelayString = "PT12H")
    public void purgeExpiredDraftOnlyLeases() {
        try (var ignored = requestCounter.allot()) {
            log.info("Purging expired draft-only media leases");

            Timestamp now = Util.now();
            tx.executeWrite(() -> {
                mediaLeaseRepository.deleteExpiredDraftOnlyUnused(now);
                mediaLeaseRepository.findExpiredDraftOnlyUsed(now)
                    .forEach(ml -> ml.setDeadline(
                        Timestamp.from(ml.getDeadline().toInstant().plus(DRAFT_ONLY_LEASE_TTL))
                    ));
            });
        }
    }

}
