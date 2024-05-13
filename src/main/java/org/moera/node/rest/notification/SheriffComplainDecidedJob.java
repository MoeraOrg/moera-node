package org.moera.node.rest.notification;

import javax.inject.Inject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.moera.node.liberin.model.RemoteSheriffComplainDecidedLiberin;
import org.moera.node.media.MediaManager;
import org.moera.node.model.notification.SheriffComplainDecidedNotification;
import org.moera.node.task.Job;

public class SheriffComplainDecidedJob extends Job<SheriffComplainDecidedJob.Parameters, Object> {

    public static class Parameters {

        private SheriffComplainDecidedNotification notification;

        public Parameters() {
        }

        public Parameters(SheriffComplainDecidedNotification notification) {
            this.notification = notification;
        }

        public SheriffComplainDecidedNotification getNotification() {
            return notification;
        }

        public void setNotification(SheriffComplainDecidedNotification notification) {
            this.notification = notification;
        }

    }

    @Inject
    private MediaManager mediaManager;

    public SheriffComplainDecidedJob() {
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
        universalContext.send(new RemoteSheriffComplainDecidedLiberin(parameters.notification));
    }

}
