package org.moera.node.rest.notification;

import java.util.List;
import jakarta.inject.Inject;

import org.moera.lib.node.types.AvatarImage;
import org.moera.lib.node.types.SheriffMark;
import org.moera.node.data.Contact;
import org.moera.node.liberin.model.MentionInRemoteCommentAddedLiberin;
import org.moera.node.media.MediaManager;
import org.moera.node.operations.ContactOperations;
import org.moera.node.task.Job;
import tools.jackson.databind.ObjectMapper;

public class MentionCommentAddedJob extends Job<MentionCommentAddedJob.Parameters, Object> {

    public record Parameters(
        String senderNodeName,
        String postingId,
        String postingOwnerName,
        String postingOwnerFullName,
        String postingOwnerGender,
        AvatarImage postingOwnerAvatar,
        String postingHeading,
        List<String> postingSheriffs,
        List<SheriffMark> postingSheriffMarks,
        String commentId,
        String commentOwnerName,
        String commentOwnerFullName,
        String commentOwnerGender,
        AvatarImage commentOwnerAvatar,
        String commentHeading,
        List<SheriffMark> commentSheriffMarks
    ) {
    }

    @Inject
    private MediaManager mediaManager;

    @Inject
    private ContactOperations contactOperations;

    public MentionCommentAddedJob() {
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
            parameters.postingOwnerAvatar
        );
        Contact.toAvatar(
            contactOperations.find(parameters.commentOwnerName),
            parameters.commentOwnerAvatar
        );

        tx.executeWriteWithExceptions(() ->
            mediaManager.downloadAvatars(
                parameters.senderNodeName,
                new AvatarImage[] {parameters.postingOwnerAvatar, parameters.commentOwnerAvatar}
            )
        );

        universalContext.send(
            new MentionInRemoteCommentAddedLiberin(
                parameters.senderNodeName,
                parameters.postingOwnerName,
                parameters.postingOwnerFullName,
                parameters.postingOwnerGender,
                parameters.postingOwnerAvatar,
                parameters.postingId,
                parameters.postingHeading,
                parameters.postingSheriffs,
                parameters.postingSheriffMarks,
                parameters.commentOwnerName,
                parameters.commentOwnerFullName,
                parameters.commentOwnerGender,
                parameters.commentOwnerAvatar,
                parameters.commentId,
                parameters.commentHeading,
                parameters.commentSheriffMarks
            )
        );
    }

}
