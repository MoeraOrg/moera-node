package org.moera.node.media;

import java.io.IOException;
import javax.inject.Inject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.moera.node.data.MediaFile;
import org.moera.node.data.MediaFileRepository;
import org.moera.node.task.Job;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

public class PrepareDirectServingJob extends Job<PrepareDirectServingJob.Parameters, Object> {

    public static class Parameters {

        public Parameters() {
        }

    }

    private static final Logger log = LoggerFactory.getLogger(PrepareDirectServingJob.class);

    private static final int PAGE_SIZE = 1024;

    @Inject
    private MediaFileRepository mediaFileRepository;

    @Inject
    private MediaOperations mediaOperations;

    public PrepareDirectServingJob() {
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
        log.info("Creating links for serving media files directly");
    }

    @Override
    protected void execute() {
        Pageable pageRequest = PageRequest.of(0, PAGE_SIZE);
        Page<MediaFile> page;
        do {
            Pageable pageable = pageRequest;
            page = tx.executeRead(() -> mediaFileRepository.findAllExposed(pageable));
            log.info("Processing page {} of {} of public media files", page.getNumber(), page.getTotalPages());
            for (MediaFile mediaFile : page.getContent()) {
                try {
                    mediaOperations.createPublicServingLink(mediaFile);
                } catch (IOException e) {
                    log.error("Could not create a link for {}: {}", mediaFile.getFileName(), e.getMessage());
                }
            }
            pageRequest = pageRequest.next();
        } while (!page.isEmpty());
    }

    @Override
    protected void succeeded() {
        super.succeeded();
        log.info("Links created successfully");
    }

}
