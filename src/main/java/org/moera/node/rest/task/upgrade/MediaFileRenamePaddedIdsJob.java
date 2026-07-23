package org.moera.node.rest.task.upgrade;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

import java.nio.file.Files;
import java.nio.file.Path;
import jakarta.inject.Inject;

import org.moera.node.config.Config;
import org.moera.node.data.MediaFile;
import org.moera.node.data.MediaFileRepository;
import org.moera.node.task.Job;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import tools.jackson.databind.ObjectMapper;

public class MediaFileRenamePaddedIdsJob extends Job<MediaFileRenamePaddedIdsJob.Parameters, Object> {

    public static class Parameters {

        public Parameters() {
        }

    }

    private static final Logger log = LoggerFactory.getLogger(MediaFileRenamePaddedIdsJob.class);

    private static final int PAGE_SIZE = 1024;

    @Inject
    private Config config;

    @Inject
    private MediaFileRepository mediaFileRepository;

    public MediaFileRenamePaddedIdsJob() {
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
    protected void started() {
        super.started();
        log.info("Removing padding from media file IDs");
    }

    @Override
    protected void execute() throws Exception {
        Pageable pageable = PageRequest.of(0, PAGE_SIZE);
        Page<MediaFile> page;
        do {
            page = tx.executeRead(() -> mediaFileRepository.findIdWithPadding(pageable));
            log.info("{} media files to rename", page.getTotalElements());
            for (MediaFile mediaFile : page.getContent()) {
                rename(mediaFile);
            }
        } while (!page.isEmpty());
    }

    private void rename(MediaFile mediaFile) throws Exception {
        String newId = mediaFile.getId().substring(0, mediaFile.getId().length() - 1);
        String newFileName = renamedFileName(mediaFile, newId);
        try {
            log.debug("Quering media file {}", newId);
            var dupMediaFile = tx.executeRead(() -> mediaFileRepository.findById(newId).orElse(null));
            if (dupMediaFile != null) {
                log.warn("Duplicate media file {}", dupMediaFile.getId());
                if (
                    dupMediaFile.getFileName() != null
                    && !dupMediaFile.getFileName().equals(newFileName)
                ) {
                    log.warn(
                        "Duplicate local file {} will be left for manual cleanup",
                        dupMediaFile.getFileName()
                    );
                }
                if (
                    dupMediaFile.getCloudFileName() != null
                    && !dupMediaFile.getCloudFileName().equals(mediaFile.getCloudFileName())
                ) {
                    log.warn(
                        "Duplicate cloud file {} will be left for manual cleanup",
                        dupMediaFile.getCloudFileName()
                    );
                }
            }
            log.debug("Renaming {} -> {}", mediaFile.getId(), newId);
            tx.executeWriteWithExceptions(() -> {
                if (dupMediaFile != null) {
                    mediaFileRepository.deleteById(dupMediaFile.getId());
                }
                mediaFileRepository.updateId(mediaFile.getId(), newId, newFileName);
                if (newFileName != null) {
                    Path oldPath = Path.of(config.getMedia().getPath(), mediaFile.getFileName());
                    Path newPath = Path.of(config.getMedia().getPath(), newFileName);
                    Files.move(oldPath, newPath, REPLACE_EXISTING);
                }
            });
            if (dupMediaFile != null) {
                log.debug("Removed {} successfully", dupMediaFile.getId());
            }
        } catch (Exception e) {
            log.error("Error renaming {} -> {}", mediaFile.getId(), newId);
            throw e;
        }
    }

    private String renamedFileName(MediaFile mediaFile, String newId) {
        String fileName = mediaFile.getFileName();
        if (fileName == null) {
            return null;
        }
        String prefix = mediaFile.getId() + ".";
        if (!fileName.startsWith(prefix)) {
            throw new IllegalStateException("Media filename does not start with its ID: " + fileName);
        }
        return newId + fileName.substring(mediaFile.getId().length());
    }

    @Override
    protected void succeeded() {
        super.succeeded();
        log.info("Removal of padding from media file IDs finished successfully");
    }

}
