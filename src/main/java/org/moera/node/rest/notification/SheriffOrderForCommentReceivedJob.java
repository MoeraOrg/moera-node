package org.moera.node.rest.notification;

import jakarta.inject.Inject;

import org.moera.lib.node.types.notifications.Notification;
import org.moera.lib.node.types.notifications.SheriffOrderForCommentAddedNotification;
import org.moera.lib.node.types.notifications.SheriffOrderForCommentDeletedNotification;
import org.moera.node.liberin.model.RemoteSheriffOrderReceivedLiberin;
import org.moera.node.media.MediaManager;
import org.moera.node.task.Job;
import tools.jackson.databind.ObjectMapper;

public class SheriffOrderForCommentReceivedJob extends Job<SheriffOrderForCommentReceivedJob.Parameters, Object> {

    public static class Parameters {

        private Notification notification;

        public Parameters() {
        }

        public Parameters(SheriffOrderForCommentAddedNotification notification) {
            this.notification = notification;
        }

        public Parameters(SheriffOrderForCommentDeletedNotification notification) {
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

    public SheriffOrderForCommentReceivedJob() {
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
        if (parameters.notification instanceof SheriffOrderForCommentAddedNotification addedNotification) {
            universalContext.send(new RemoteSheriffOrderReceivedLiberin(addedNotification));
        }
        if (parameters.notification instanceof SheriffOrderForCommentDeletedNotification deletedNotification) {
            universalContext.send(new RemoteSheriffOrderReceivedLiberin(deletedNotification));
        }
    }

}
