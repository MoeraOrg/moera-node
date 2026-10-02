package org.moera.node.rest.notification;

import java.util.UUID;
import jakarta.inject.Inject;

import org.moera.lib.node.types.AvatarImage;
import org.moera.node.data.Contact;
import org.moera.node.media.MediaManager;
import org.moera.node.operations.SharingOperations;
import org.moera.node.operations.ContactOperations;
import org.moera.node.task.Job;
import tools.jackson.databind.ObjectMapper;

public class PostingSharedJob extends Job<PostingSharedJob.Parameters, Object> {

    public record Parameters(
        String senderNodeName,
        AvatarImage senderAvatar,
        UUID postingId,
        UUID sharingId
    ) {
    }

    @Inject
    private MediaManager mediaManager;

    @Inject
    private SharingOperations sharingOperations;

    @Inject
    private ContactOperations contactOperations;

    public PostingSharedJob() {
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
        Contact.toAvatar(contactOperations.find(parameters.senderNodeName), parameters.senderAvatar);
        tx.executeWriteWithExceptions(() ->
            mediaManager.downloadAvatar(parameters.senderNodeName, parameters.senderAvatar)
        );

        tx.executeWrite(() -> sharingOperations.completeShared(
            parameters.postingId, parameters.sharingId, parameters.senderAvatar
        ));
    }

}
