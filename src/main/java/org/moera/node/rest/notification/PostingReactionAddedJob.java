package org.moera.node.rest.notification;

import jakarta.inject.Inject;

import org.moera.lib.node.types.AvatarImage;
import org.moera.node.data.Contact;
import org.moera.node.liberin.model.RemoteCommentMediaReactionAddedLiberin;
import org.moera.node.liberin.model.RemotePostingMediaReactionAddedLiberin;
import org.moera.node.media.MediaManager;
import org.moera.node.operations.ContactOperations;
import org.moera.node.task.Job;
import tools.jackson.databind.ObjectMapper;

public class PostingReactionAddedJob extends Job<PostingReactionAddedJob.Parameters, Object> {

    public record Parameters(
        String senderNodeName,
        String ownerName,
        String ownerFullName,
        String ownerGender,
        AvatarImage ownerAvatar,
        boolean negative,
        int emoji,
        String parentPostingNodeName,
        String parentPostingFullName,
        String parentPostingGender,
        AvatarImage parentPostingAvatar,
        String parentPostingId,
        String parentHeading,
        String parentCommentId,
        String parentMediaId,
        String postingId
    ) {
    }

    @Inject
    private MediaManager mediaManager;

    @Inject
    private ContactOperations contactOperations;

    public PostingReactionAddedJob() {
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
                contactOperations.find(parameters.parentPostingNodeName),
                parameters.parentPostingAvatar);
        Contact.toAvatar(
                contactOperations.find(parameters.ownerName),
                parameters.ownerAvatar);

        tx.executeWriteWithExceptions(() ->
                mediaManager.downloadAvatars(
                        parameters.senderNodeName,
                        new AvatarImage[] {parameters.parentPostingAvatar, parameters.ownerAvatar}));

        if (parameters.parentPostingId != null) {
            if (parameters.parentCommentId == null) {
                universalContext.send(
                        new RemotePostingMediaReactionAddedLiberin(parameters.senderNodeName,
                                parameters.parentPostingNodeName, parameters.parentPostingFullName,
                                parameters.parentPostingGender, parameters.parentPostingAvatar, parameters.postingId,
                                parameters.parentPostingId, parameters.parentMediaId, parameters.ownerName,
                                parameters.ownerFullName, parameters.ownerGender, parameters.ownerAvatar,
                                parameters.parentHeading, parameters.negative, parameters.emoji));
            } else {
                universalContext.send(
                        new RemoteCommentMediaReactionAddedLiberin(parameters.senderNodeName,
                                parameters.parentPostingNodeName, parameters.parentPostingFullName,
                                parameters.parentPostingGender, parameters.parentPostingAvatar, parameters.postingId,
                                parameters.parentPostingId, parameters.parentCommentId, parameters.parentMediaId,
                                parameters.ownerName, parameters.ownerFullName, parameters.ownerGender,
                                parameters.ownerAvatar, parameters.parentHeading, parameters.negative,
                                parameters.emoji));
            }
        }
    }

}
