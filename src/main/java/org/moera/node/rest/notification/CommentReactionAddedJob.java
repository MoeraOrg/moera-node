package org.moera.node.rest.notification;

import jakarta.inject.Inject;

import org.moera.lib.node.types.AvatarImage;
import org.moera.node.data.Contact;
import org.moera.node.liberin.model.RemoteCommentReactionAddedLiberin;
import org.moera.node.media.MediaManager;
import org.moera.node.operations.ContactOperations;
import org.moera.node.task.Job;
import tools.jackson.databind.ObjectMapper;

public class CommentReactionAddedJob extends Job<CommentReactionAddedJob.Parameters, Object> {

    public record Parameters(
        String senderNodeName,
        String postingId,
        String postingNodeName,
        String postingFullName,
        String postingGender,
        AvatarImage postingAvatar,
        String postingHeading,
        String commentId,
        String commentHeading,
        String ownerName,
        String ownerFullName,
        String ownerGender,
        AvatarImage ownerAvatar,
        boolean negative,
        int emoji
    ) {
    }

    @Inject
    private MediaManager mediaManager;

    @Inject
    private ContactOperations contactOperations;

    public CommentReactionAddedJob() {
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
            contactOperations.find(parameters.postingNodeName),
            parameters.postingAvatar
        );
        Contact.toAvatar(
            contactOperations.find(parameters.ownerName),
            parameters.ownerAvatar
        );
        tx.executeWriteWithExceptions(() ->
            mediaManager.downloadAvatars(
                parameters.senderNodeName,
                new AvatarImage[] {parameters.postingAvatar, parameters.ownerAvatar}
            )
        );

        universalContext.send(
            new RemoteCommentReactionAddedLiberin(
                parameters.senderNodeName,
                parameters.postingNodeName,
                parameters.postingFullName,
                parameters.postingGender,
                parameters.postingAvatar,
                parameters.postingId,
                parameters.commentId,
                parameters.ownerName,
                parameters.ownerFullName,
                parameters.ownerGender,
                parameters.ownerAvatar,
                parameters.commentHeading,
                parameters.negative,
                parameters.emoji
            )
        );
    }

}
