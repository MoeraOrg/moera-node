package org.moera.node.operations.publicpages;

import java.util.UUID;
import jakarta.inject.Inject;

import org.moera.node.task.Job;
import org.moera.node.util.ParametrizedLock;
import tools.jackson.databind.ObjectMapper;

public class UpdateTimelinePublicPagesJob extends Job<UpdateTimelinePublicPagesJob.Parameters, Object> {

    public record Parameters(
        long moment
    ) {
    }

    private static final ParametrizedLock<UUID> LOCK = new ParametrizedLock<>();

    @Inject
    private TimelinePublicPageOperations timelinePublicPageOperations;

    public UpdateTimelinePublicPagesJob() {
        noRetry();
    }

    @Override
    protected void setParameters(String parameters, ObjectMapper objectMapper) {
        this.parameters = objectMapper.readValue(parameters, UpdateTimelinePublicPagesJob.Parameters.class);
    }

    @Override
    protected void setState(String state, ObjectMapper objectMapper) {
        this.state = null;
    }

    @Override
    protected void execute() {
        try (var ignored = LOCK.lock(nodeId)) {
            tx.executeWrite(() -> timelinePublicPageOperations.updatePublicPages(parameters.moment));
        }
    }

}
