package org.moera.node.rest.notification;

import jakarta.inject.Inject;

import org.moera.lib.node.types.notifications.SheriffComplaintDecidedNotification;
import org.moera.node.liberin.model.RemoteSheriffComplaintDecidedLiberin;
import org.moera.node.media.MediaManager;
import org.moera.node.task.Job;
import tools.jackson.databind.ObjectMapper;

public class SheriffComplaintDecidedJob extends Job<SheriffComplaintDecidedJob.Parameters, Object> {

    public record Parameters(
        SheriffComplaintDecidedNotification notification
    ) {
    }

    @Inject
    private MediaManager mediaManager;

    public SheriffComplaintDecidedJob() {
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
        tx.executeWriteWithExceptions(() ->
            mediaManager.downloadAvatar(
                parameters.notification.getSenderNodeName(),
                parameters.notification.getSenderAvatar()
            )
        );
        universalContext.send(new RemoteSheriffComplaintDecidedLiberin(parameters.notification));
    }

}
