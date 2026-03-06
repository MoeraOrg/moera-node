package org.moera.node.rest.notification;

import jakarta.inject.Inject;

import org.moera.lib.node.types.notifications.PremoderatedCommentDecidedNotification;
import org.moera.node.data.Contact;
import org.moera.node.liberin.model.PremoderatedCommentDecidedLiberin;
import org.moera.node.media.MediaManager;
import org.moera.node.operations.ContactOperations;
import org.moera.node.task.Job;
import tools.jackson.databind.ObjectMapper;

public class PremoderatedCommentDecidedJob extends Job<PremoderatedCommentDecidedJob.Parameters, Object> {

    public static class Parameters {

        private PremoderatedCommentDecidedNotification notification;

        public Parameters() {
        }

        public Parameters(PremoderatedCommentDecidedNotification notification) {
            this.notification = notification;
        }

        public PremoderatedCommentDecidedNotification getNotification() {
            return notification;
        }

        public void setNotification(PremoderatedCommentDecidedNotification notification) {
            this.notification = notification;
        }

    }

    @Inject
    private MediaManager mediaManager;

    @Inject
    private ContactOperations contactOperations;

    public PremoderatedCommentDecidedJob() {
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
        Contact.toAvatar(
            contactOperations.find(parameters.notification.getPostingOwnerName()),
            parameters.notification.getPostingOwnerAvatar()
        );
        tx.executeWriteWithExceptions(() ->
            mediaManager.downloadAvatar(
                parameters.notification.getSenderNodeName(),
                parameters.notification.getPostingOwnerAvatar()
            )
        );
        universalContext.send(new PremoderatedCommentDecidedLiberin(parameters.notification));
    }

}
