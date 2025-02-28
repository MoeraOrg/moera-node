package org.moera.node.rest.notification;

import jakarta.inject.Inject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.moera.lib.node.types.notifications.Notification;
import org.moera.lib.node.types.notifications.SheriffOrderForPostingAddedNotification;
import org.moera.lib.node.types.notifications.SheriffOrderForPostingDeletedNotification;
import org.moera.node.liberin.model.RemoteSheriffOrderReceivedLiberin;
import org.moera.node.media.MediaManager;
import org.moera.node.task.Job;

public class SheriffOrderForPostingReceivedJob extends Job<SheriffOrderForPostingReceivedJob.Parameters, Object> {

    public static class Parameters {

        private Notification notification;

        public Parameters() {
        }

        public Parameters(SheriffOrderForPostingAddedNotification notification) {
            this.notification = notification;
        }

        public Parameters(SheriffOrderForPostingDeletedNotification notification) {
            this.notification = notification;
        }

        public Notification getNotification() {
            return notification;
        }

        public void setNotification(Notification notification) {
            this.notification = notification;
        }

    }

    @Inject
    private MediaManager mediaManager;

    public SheriffOrderForPostingReceivedJob() {
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
    protected void execute() throws Exception {
        tx.executeWriteWithExceptions(() ->
            mediaManager.downloadAvatar(
                parameters.notification.getSenderNodeName(),
                parameters.notification.getSenderAvatar()
            )
        );
        if (parameters.notification instanceof SheriffOrderForPostingAddedNotification addedNotification) {
            universalContext.send(new RemoteSheriffOrderReceivedLiberin(addedNotification));
        }
        if (parameters.notification instanceof SheriffOrderForPostingDeletedNotification deletedNotification) {
            universalContext.send(new RemoteSheriffOrderReceivedLiberin(deletedNotification));
        }
    }

}
