package org.moera.node.media.video;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import jakarta.inject.Inject;

import org.moera.node.data.MediaFile;
import org.moera.node.data.MediaFileOwner;
import org.moera.node.data.MediaFileOwnerRepository;
import org.moera.node.data.MediaFileRepository;
import org.moera.node.liberin.model.MediaCompressedLiberin;
import org.moera.node.media.DirectServeOperations;
import org.moera.node.media.MediaOperations;
import org.moera.node.media.TemporaryFile;
import org.moera.node.media.grant.MediaGrantGenerator;
import org.moera.node.model.PrivateMediaFileInfoUtil;
import org.moera.node.task.Job;
import org.moera.node.util.DigestingOutputStream;
import org.moera.node.util.ToolRunner.StopReason;
import org.moera.node.util.ToolRunner.ToolResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.ObjectMapper;

public class VideoCompressionJob extends Job<VideoCompressionJob.Parameters, Object> {

    public static class Parameters {

        private String mediaFileId;

        public Parameters() {
        }

        public Parameters(String mediaFileId) {
            this.mediaFileId = mediaFileId;
        }

        public String getMediaFileId() {
            return mediaFileId;
        }

        public void setMediaFileId(String mediaFileId) {
            this.mediaFileId = mediaFileId;
        }

    }

    private static final Logger log = LoggerFactory.getLogger(VideoCompressionJob.class);

    private static final Duration NO_PROGRESS_TIMEOUT = Duration.ofMinutes(5);

    @Inject
    private MediaFileRepository mediaFileRepository;

    @Inject
    private MediaFileOwnerRepository mediaFileOwnerRepository;

    @Inject
    private MediaOperations mediaOperations;

    @Inject
    private DirectServeOperations directServeOperations;

    public VideoCompressionJob() {
        retryCount(6, "PT30M");
    }

    @Override
    protected void setParameters(String parameters, ObjectMapper objectMapper) {
        this.parameters = objectMapper.readValue(parameters, Parameters.class);
    }

    @Override
    protected void setState(String state, ObjectMapper objectMapper) {
        this.state = null;
    }

    @Override
    protected void execute() throws Exception {
        MediaFile source = tx.executeWriteWithExceptions(() -> loadSource(parameters.mediaFileId));
        if (source == null) {
            return;
        }
        VideoInfo videoInfo = VideoProbe.parseProbeOutput(
            Path.of(source.getId()), source.getMimeType(), source.getStreamInfo()
        );

        TemporaryFile output = mediaOperations.tmpFile();
        try {
            output.outputStream().close();
            try (var input = mediaOperations.openContent(source)) {
                compress(input.path(), output, videoInfo);
            }
            if (VideoUtil.getVideoInfo(output.path(), "video/mp4").uncompressed()) {
                throw new InvalidVideoException();
            }

            DigestingOutputStream digests;
            try (InputStream in = Files.newInputStream(output.path())) {
                digests = mediaOperations.transfer(in, null, null, null);
            }
            MediaFile compressed = mediaOperations.putInPlace(
                digests.getHash(), "video/mp4", output.path(), digests.getDigest(), false
            );
            if (compressed.isUncompressed()) {
                throw new InvalidVideoException();
            }

            tx.executeWriteWithExceptions(() -> publish(compressed.getId()));
        } finally {
            Files.deleteIfExists(output.path());
        }
    }

    @Override
    protected void unhandledException(Throwable e) {
        if (e instanceof Exception) {
            log.error("Video compression attempt failed for {}: {}", parameters.mediaFileId, e.getMessage());
            log.debug("Video compression attempt failed for {}", parameters.mediaFileId, e);
            recover(null);
        } else {
            super.unhandledException(e);
        }
    }

    private MediaFile loadSource(String mediaFileId) {
        MediaFile mediaFile = mediaFileRepository.findByIdForUpdate(mediaFileId).orElse(null);
        if (!isCurrent(mediaFile)) {
            return null;
        }
        return mediaFile;
    }

    private boolean isCurrent(MediaFile mediaFile) {
        return mediaFile != null
            && mediaFile.getCompressedFile() == null
            && mediaFile.getCompressionJob() != null
            && Objects.equals(mediaFile.getCompressionJob().getId(), getId());
    }

    private void compress(Path input, TemporaryFile output, VideoInfo info) throws IOException, InterruptedException {
        boolean hasSubtitles = !info.subtitleStreamIndexes().isEmpty();
        boolean hasAttachedPictures = !info.attachedPictureStreamIndexes().isEmpty();
        ToolResult result = runFfmpeg(input, output, info, hasSubtitles, hasAttachedPictures);
        if (result.exitedWithError() && hasSubtitles && hasAttachedPictures) {
            log.warn("Could not preserve all ancillary streams, omitting attached pictures: {}", result.stderr());
            result = runFfmpeg(input, output, info, true, false);
            if (result.exitedWithError()) {
                log.warn("Could not preserve subtitles, trying attached pictures only: {}", result.stderr());
                result = runFfmpeg(input, output, info, false, true);
            }
        }
        if (result.exitedWithError() && (hasSubtitles || hasAttachedPictures)) {
            log.warn("Could not preserve ancillary streams, omitting them: {}", result.stderr());
            result = runFfmpeg(input, output, info, false, false);
        }
        if (result.stopReason() != StopReason.NONE || result.exitCode() != 0) {
            throw new IOException(ffmpegFailure(result));
        }
    }

    private ToolResult runFfmpeg(
        Path input,
        TemporaryFile output,
        VideoInfo info,
        boolean preserveSubtitles,
        boolean preserveAttachedPictures
    ) throws IOException, InterruptedException {
        return VideoUtil.compressVideo(
            input,
            output.path(),
            info,
            preserveSubtitles,
            preserveAttachedPictures,
            NO_PROGRESS_TIMEOUT,
            executionDeadline(info.duration())
        );
    }

    static Duration executionDeadline(float sourceDuration) {
        return Duration.ofSeconds(900 + (long) Math.ceil(15 * sourceDuration));
    }

    private String ffmpegFailure(ToolResult result) {
        return switch (result.stopReason()) {
            case NO_PROGRESS -> "ffmpeg stopped reporting progress: " + result.stderr();
            case DEADLINE -> "ffmpeg exceeded its execution deadline: " + result.stderr();
            case NONE -> "ffmpeg exited with code %d: %s".formatted(result.exitCode(), result.stderr());
        };
    }

    private void publish(String compressedFileId) throws IOException {
        MediaFile original = mediaFileRepository.findByIdForUpdate(parameters.mediaFileId).orElse(null);
        if (!isCurrent(original)) {
            log.warn("Compression job for media file {} is obsolete", parameters.mediaFileId);
            return;
        }
        MediaFile compressed = mediaFileRepository.findById(compressedFileId)
            .orElseThrow(() -> new IllegalStateException("Compressed media file disappeared"));

        Collection<MediaFileOwner> owners = mediaFileOwnerRepository.findAllByFileAndDownsize(original.getId());
        Map<UUID, List<MediaFileOwner>> ownersByNode = owners.stream()
            .collect(Collectors.groupingBy(MediaFileOwner::getNodeId));
        for (var entry : ownersByNode.entrySet()) {
            universalContext.associate(entry.getKey());
            for (MediaFileOwner owner : entry.getValue()) {
                MediaFileOwner replacement = mediaOperations.own(compressed, owner.getTitle());
                replacement.setDownsize(true);
                replacement.setUnrestricted(owner.isUnrestricted());
                replacement.setPermissionsUpdatedAt(owner.getPermissionsUpdatedAt());
                replacement = mediaFileOwnerRepository.save(replacement);
                owner.setCompressedOwner(replacement);
                send(new MediaCompressedLiberin(
                    owner.getId(),
                    original.getId(),
                    PrivateMediaFileInfoUtil.build(
                        replacement,
                        directServeOperations,
                        new MediaGrantGenerator(universalContext.getOptions())
                    )
                ));
            }
        }

        original.setCompressedFile(compressed);
    }

}
