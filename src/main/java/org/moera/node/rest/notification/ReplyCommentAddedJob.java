package org.moera.node.rest.notification;

import java.util.List;
import jakarta.inject.Inject;

import org.moera.lib.node.types.AvatarImage;
import org.moera.lib.node.types.SheriffMark;
import org.moera.node.data.Contact;
import org.moera.node.liberin.model.ReplyCommentAddedLiberin;
import org.moera.node.media.MediaManager;
import org.moera.node.operations.ContactOperations;
import org.moera.node.task.Job;
import tools.jackson.databind.ObjectMapper;

public class ReplyCommentAddedJob extends Job<ReplyCommentAddedJob.Parameters, Object> {

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
        String repliedToId,
        String repliedToHeading,
        String commentOwnerName,
        String commentOwnerFullName,
        String commentOwnerGender,
        AvatarImage commentOwnerAvatar,
        List<SheriffMark> commentSheriffMarks
    ) {
    }

    @Inject
    private MediaManager mediaManager;

    @Inject
    private ContactOperations contactOperations;

    public ReplyCommentAddedJob() {
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
        Contact.toAvatar(
                contactOperations.find(parameters.commentOwnerName),
                parameters.commentOwnerAvatar);
        tx.executeWriteWithExceptions(
                () -> mediaManager.downloadAvatars(
                        parameters.senderNodeName,
                        new AvatarImage[] {parameters.postingOwnerAvatar, parameters.commentOwnerAvatar}));
        universalContext.send(
                new ReplyCommentAddedLiberin(parameters.senderNodeName, parameters.postingOwnerName,
                        parameters.postingOwnerFullName, parameters.postingOwnerGender, parameters.postingOwnerAvatar,
                        parameters.postingHeading, parameters.postingSheriffs, parameters.postingSheriffMarks,
                        parameters.postingId, parameters.repliedToHeading, parameters.repliedToId,
                        parameters.commentOwnerName, parameters.commentOwnerFullName, parameters.commentOwnerGender,
                        parameters.commentOwnerAvatar, parameters.commentSheriffMarks, parameters.commentId));
    }

}
