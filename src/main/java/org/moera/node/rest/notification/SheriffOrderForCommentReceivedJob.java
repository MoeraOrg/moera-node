package org.moera.node.rest.notification;

import jakarta.inject.Inject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.moera.node.liberin.model.RemoteSheriffOrderReceivedLiberin;
import org.moera.node.media.MediaManager;
import org.moera.node.model.notification.SheriffOrderForCommentNotification;
import org.moera.node.task.Job;

public class SheriffOrderForCommentReceivedJob extends Job<SheriffOrderForCommentReceivedJob.Parameters, Object> {

    public static class Parameters {

        private boolean deleted;
        private SheriffOrderForCommentNotification notification;

        public Parameters() {
        }

        public Parameters(boolean deleted, SheriffOrderForCommentNotification notification) {
            this.deleted = deleted;
            this.notification = notification;
        }

        public boolean isDeleted() {
            return deleted;
        }

        public void setDeleted(boolean deleted) {
            this.deleted = deleted;
        }

        public SheriffOrderForCommentNotification getNotification() {
            return notification;
        }

        public void setNotification(SheriffOrderForCommentNotification notification) {
            this.notification = notification;
        }

    }

    @Inject
    private MediaManager mediaManager;

    public SheriffOrderForCommentReceivedJob() {
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
                        parameters.notification.getSenderAvatar()));
        universalContext.send(new RemoteSheriffOrderReceivedLiberin(parameters.deleted, parameters.notification));
    }

}
