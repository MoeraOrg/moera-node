package org.moera.node.media;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.Instant;
import javax.inject.Inject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.moera.node.data.MediaFileOwner;
import org.moera.node.data.MediaFileOwnerRepository;
import org.moera.node.task.Job;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

public class PreparePrivateDirectServingJob extends Job<PreparePrivateDirectServingJob.Parameters, Object> {

    public static class Parameters {

        public Parameters() {
        }

    }

    private static final Logger log = LoggerFactory.getLogger(PreparePrivateDirectServingJob.class);

    private static final int PAGE_SIZE = 1024;

    @Inject
    private MediaFileOwnerRepository mediaFileOwnerRepository;

    @Inject
    private MediaOperations mediaOperations;

    public PreparePrivateDirectServingJob() {
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
        log.info("Creating links for serving private media files directly");
    }

    @Override
    protected void execute() {
        Timestamp deadline = Timestamp.from(Instant.now().plus(MediaOperations.NONCE_REFRESH_INTERVAL));

        Pageable pageable = PageRequest.of(0, PAGE_SIZE);
        Page<MediaFileOwner> page;
        do {
            page = tx.executeRead(() -> mediaFileOwnerRepository.findAllWithoutNonce(pageable));
            log.info("{} private media files to process", page.getTotalElements());
            for (MediaFileOwner mediaFileOwner : page.getContent()) {
                String nonce = MediaFileOwner.generateNonce();
                tx.executeWrite(() -> mediaFileOwnerRepository.replaceNonce(mediaFileOwner.getId(), nonce, deadline));
                mediaFileOwner.setNonce(nonce);
                try {
                    mediaOperations.createPrivateServingLink(mediaFileOwner);
                } catch (IOException e) {
                    log.error("Could not create a link for {}: {}", mediaFileOwner.getDirectFileName(), e.getMessage());
                }
            }
        } while (!page.isEmpty());
    }

    @Override
    protected void succeeded() {
        super.succeeded();
        log.info("Links created successfully");
    }

}
