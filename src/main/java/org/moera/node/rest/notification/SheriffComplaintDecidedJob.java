package org.moera.node.rest.notification;

import javax.inject.Inject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.moera.node.liberin.model.RemoteSheriffComplaintDecidedLiberin;
import org.moera.node.media.MediaManager;
import org.moera.node.model.notification.SheriffComplaintDecidedNotification;
import org.moera.node.task.Job;

public class SheriffComplaintDecidedJob extends Job<SheriffComplaintDecidedJob.Parameters, Object> {

    public static class Parameters {

        private SheriffComplaintDecidedNotification notification;

        public Parameters() {
        }

        public Parameters(SheriffComplaintDecidedNotification notification) {
            this.notification = notification;
        }

        public SheriffComplaintDecidedNotification getNotification() {
            return notification;
        }

        public void setNotification(SheriffComplaintDecidedNotification notification) {
            this.notification = notification;
        }

    }

    @Inject
    private MediaManager mediaManager;

    public SheriffComplaintDecidedJob() {
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
        universalContext.send(new RemoteSheriffComplaintDecidedLiberin(parameters.notification));
    }

}
