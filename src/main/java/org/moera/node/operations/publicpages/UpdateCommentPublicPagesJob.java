package org.moera.node.operations.publicpages;

import java.util.UUID;
import jakarta.inject.Inject;

import org.moera.node.task.Job;
import org.moera.node.util.ParametrizedLock;
import tools.jackson.databind.ObjectMapper;

public class UpdateCommentPublicPagesJob extends Job<UpdateCommentPublicPagesJob.Parameters, Object> {

    public record Parameters(
        UUID postingId,
        long moment
    ) {
    }

    private static final ParametrizedLock<UUID> LOCK = new ParametrizedLock<>();

    @Inject
    private CommentPublicPageOperations commentPublicPageOperations;

    public UpdateCommentPublicPagesJob() {
        noRetry();
    }

    @Override
    protected void setParameters(String parameters, ObjectMapper objectMapper) {
        this.parameters = objectMapper.readValue(parameters, UpdateCommentPublicPagesJob.Parameters.class);
    }

    @Override
    protected void setState(String state, ObjectMapper objectMapper) {
        this.state = null;
    }

    @Override
    protected void execute() {
        try (var ignored = LOCK.lock(parameters.postingId)) {
            tx.executeWrite(() ->
                commentPublicPageOperations.updatePublicPages(parameters.postingId, parameters.moment)
            );
        }
    }

}
