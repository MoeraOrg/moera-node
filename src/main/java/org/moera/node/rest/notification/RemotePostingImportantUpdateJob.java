package org.moera.node.rest.notification;

import jakarta.inject.Inject;

import org.moera.lib.node.types.AvatarImage;
import org.moera.node.data.Contact;
import org.moera.node.liberin.model.RemotePostingImportantUpdateLiberin;
import org.moera.node.media.MediaManager;
import org.moera.node.operations.ContactOperations;
import org.moera.node.task.Job;
import tools.jackson.databind.ObjectMapper;

public class RemotePostingImportantUpdateJob extends Job<RemotePostingImportantUpdateJob.Parameters, Object> {

    public record Parameters(
        String senderNodeName,
        String postingId,
        String postingOwnerName,
        String postingOwnerFullName,
        String postingOwnerGender,
        AvatarImage postingOwnerAvatar,
        String postingHeading,
        String description
    ) {
    }

    @Inject
    private MediaManager mediaManager;

    @Inject
    private ContactOperations contactOperations;

    public RemotePostingImportantUpdateJob() {
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
                contactOperations.find(parameters.postingOwnerName),
                parameters.postingOwnerAvatar);
        tx.executeWriteWithExceptions(() ->
                mediaManager.downloadAvatar(parameters.senderNodeName, parameters.postingOwnerAvatar));
        universalContext.send(
                new RemotePostingImportantUpdateLiberin(parameters.senderNodeName, parameters.postingOwnerName,
                        parameters.postingOwnerFullName, parameters.postingOwnerGender, parameters.postingOwnerAvatar,
                        parameters.postingId, parameters.postingHeading, parameters.description));
    }

}
