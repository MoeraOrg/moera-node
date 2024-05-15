package org.moera.node.rest.task.upgrade;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.inject.Inject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.moera.node.config.Config;
import org.moera.node.data.MediaFile;
import org.moera.node.data.MediaFileRepository;
import org.moera.node.media.MimeUtils;
import org.moera.node.task.Job;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

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
    protected void setParameters(String parameters, ObjectMapper objectMapper) throws JsonProcessingException {
        this.parameters = objectMapper.readValue(parameters, Parameters.class);
    }

    @Override
    protected void setState(String state, ObjectMapper objectMapper) throws JsonProcessingException {
        this.state = null;
    }

    @Override
    protected void started() {
        super.started();
        log.info("Removing padding from media file IDs");
    }

    @Override
    protected void execute() throws IOException {
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

    private void rename(MediaFile mediaFile) throws IOException {
        String newId = mediaFile.getId().substring(0, mediaFile.getId().length() - 1);
        try {
            log.debug("Quering media file {}", newId);
            var dupMediaFile = tx.executeRead(() -> mediaFileRepository.findById(newId).orElse(null));
            if (dupMediaFile != null) {
                log.warn("Duplicate media file {}", dupMediaFile.getId());
                tx.executeWrite(() -> mediaFileRepository.deleteById(dupMediaFile.getId()));
                log.debug("Removed {} successfully", dupMediaFile.getId());
            }
            log.debug("Renaming {} -> {}", mediaFile.getId(), newId);
            tx.executeWrite(() -> mediaFileRepository.updateId(mediaFile.getId(), newId));
        } catch (RuntimeException e) {
            log.error("Error renaming {} -> {}", mediaFile.getId(), newId);
            throw e;
        }
        Path oldPath = FileSystems.getDefault().getPath(
                config.getMedia().getPath(), MimeUtils.fileName(mediaFile.getId(), mediaFile.getMimeType()));
        Path newPath = FileSystems.getDefault().getPath(
                config.getMedia().getPath(), MimeUtils.fileName(newId, mediaFile.getMimeType()));
        Files.move(oldPath, newPath, REPLACE_EXISTING);
    }

    @Override
    protected void succeeded() {
        super.succeeded();
        log.info("Removal of padding from media file IDs finished successfully");
    }

}
