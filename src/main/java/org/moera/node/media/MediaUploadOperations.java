package org.moera.node.media;

import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.sql.Timestamp;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import jakarta.inject.Inject;

import org.moera.lib.node.types.validate.ValidationFailure;
import org.moera.lib.util.LogUtil;
import org.moera.node.config.Config;
import org.moera.node.data.MediaUpload;
import org.moera.node.data.MediaUploadRepository;
import org.moera.node.global.RequestCounter;
import org.moera.node.util.ParametrizedLock;
import org.moera.node.util.Transaction;
import org.moera.node.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class MediaUploadOperations {

    public static final String UPLOADS_DIR = "uploads";
    public static final Duration UPLOAD_TTL = Duration.ofHours(6);

    private static final Logger log = LoggerFactory.getLogger(MediaUploadOperations.class);

    @Inject
    private Config config;

    @Inject
    private RequestCounter requestCounter;

    @Inject
    private MediaOperations mediaOperations;

    @Inject
    private MediaUploadRepository mediaUploadRepository;

    @Inject
    private Transaction tx;

    private final ParametrizedLock<UUID> uploadLocks = new ParametrizedLock<>();

    public ParametrizedLock<UUID>.AutoUnlock lock(UUID id) {
        return uploadLocks.lock(id);
    }

    public int selectChunkSize(Integer proposedChunkSize, int maxChunkSize) {
        if (maxChunkSize <= 0 || proposedChunkSize != null && proposedChunkSize <= 0) {
            throw new ValidationFailure("media-upload.chunk-size.invalid");
        }
        return proposedChunkSize != null && proposedChunkSize <= maxChunkSize ? proposedChunkSize : maxChunkSize;
    }

    public void createUploadFile(MediaUpload mediaUpload) throws IOException {
        Path path = getPath(mediaUpload);
        try (RandomAccessFile file = new RandomAccessFile(path.toFile(), "rw")) {
            file.setLength(mediaUpload.getFileSize());
        }
    }

    public void writeChunk(
        MediaUpload mediaUpload, int chunk, InputStream in, Long contentLength
    ) throws IOException {
        if (mediaUpload.getCompletedAt() != null) {
            return;
        }

        int expectedSize = expectedChunkSize(mediaUpload, chunk);
        long offset = (long) chunk * mediaUpload.getChunkSize();
        Path tmpPath = receiveChunk(in, contentLength, expectedSize);
        try {
            writeUploadFile(mediaUpload, tmpPath, offset);
        } finally {
            Files.deleteIfExists(tmpPath);
        }

        mediaUpload.addChunk(chunk);
        if (mediaUpload.isCompleted()) {
            mediaUpload.setCompletedAt(Util.now());
        }
    }

    public void deleteUploadFile(MediaUpload mediaUpload) throws IOException {
        Files.deleteIfExists(getPath(mediaUpload));
    }

    public void deleteUploadFileQuietly(MediaUpload mediaUpload) {
        try {
            deleteUploadFile(mediaUpload);
        } catch (IOException e) {
            log.warn(
                "Error deleting media upload file {}: {}",
                LogUtil.format(getPath(mediaUpload).toString()), e.getMessage()
            );
        }
    }

    private Path receiveChunk(InputStream in, Long contentLength, int expectedSize) throws IOException {
        if (contentLength != null && contentLength != expectedSize) {
            throw new ValidationFailure("media-upload.chunk.wrong-size");
        }

        var tmp = mediaOperations.tmpFile();
        try {
            MediaOperations.transfer(in, tmp.outputStream(), contentLength, expectedSize);
            if (Files.size(tmp.path()) != expectedSize) {
                throw new ValidationFailure("media-upload.chunk.wrong-size");
            }
            return tmp.path();
        } catch (ThresholdReachedException e) {
            throw new ValidationFailure("media-upload.chunk.wrong-size");
        } catch (IOException | RuntimeException e) {
            Files.deleteIfExists(tmp.path());
            throw e;
        }
    }

    private void writeUploadFile(MediaUpload mediaUpload, Path tmpPath, long offset) throws IOException {
        try (
            FileChannel upload = FileChannel.open(getPath(mediaUpload), StandardOpenOption.WRITE);
            FileChannel chunk = FileChannel.open(tmpPath, StandardOpenOption.READ)
        ) {
            upload.position(offset);
            long position = 0;
            while (position < chunk.size()) {
                position += chunk.transferTo(position, chunk.size() - position, upload);
            }
        }
    }

    private int expectedChunkSize(MediaUpload mediaUpload, int chunk) {
        int totalChunks = mediaUpload.getTotalChunks();
        if (chunk < 0 || chunk >= totalChunks) {
            throw new ValidationFailure("media-upload.chunk.invalid");
        }

        long offset = (long) chunk * mediaUpload.getChunkSize();
        return (int) Math.min(mediaUpload.getChunkSize(), mediaUpload.getFileSize() - offset);
    }

    public Path getPath(MediaUpload mediaUpload) {
        return FileSystems.getDefault()
            .getPath(config.getMedia().getPath(), UPLOADS_DIR, mediaUpload.getId().toString());
    }

    @Scheduled(fixedDelayString = "PT3H")
    public void purgeExpired() {
        try (var ignored = requestCounter.allot()) {
            log.info("Purging expired media uploads");

            Timestamp now = Util.now();
            List<MediaUpload> uploads = tx.executeRead(() -> mediaUploadRepository.findExpired(now));
            for (MediaUpload mediaUpload : uploads) {
                try (var ignoredLock = lock(mediaUpload.getId())) {
                    deleteUploadFileQuietly(mediaUpload);
                    tx.executeWrite(() -> mediaUploadRepository.deleteByNodeIdAndId(
                        mediaUpload.getNodeId(), mediaUpload.getId()
                    ));
                }
            }
        }
    }

}
