package org.moera.node.rest.notification;

import java.util.List;
import jakarta.inject.Inject;

import org.moera.lib.node.types.AvatarImage;
import org.moera.lib.node.types.SheriffMark;
import org.moera.lib.node.types.SubscriptionReason;
import org.moera.node.data.Contact;
import org.moera.node.data.OwnCommentRepository;
import org.moera.node.liberin.model.ForeignCommentAddedLiberin;
import org.moera.node.media.MediaManager;
import org.moera.node.operations.ContactOperations;
import org.moera.node.task.Job;
import tools.jackson.databind.ObjectMapper;

public class RemotePostingCommentAddedJob
        extends Job<RemotePostingCommentAddedJob.Parameters, RemotePostingCommentAddedJob.State> {

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
        List<SheriffMark> commentSheriffMarks,
        String commentRepliedTo,
        SubscriptionReason reason
    ) {
    }

    public static class State {

        private boolean repliedToChecked;

        public State() {
        }

        public boolean isRepliedToChecked() {
            return repliedToChecked;
        }

        public void setRepliedToChecked(boolean repliedToChecked) {
            this.repliedToChecked = repliedToChecked;
        }

    }

    @Inject
    private OwnCommentRepository ownCommentRepository;

    @Inject
    private MediaManager mediaManager;

    @Inject
    private ContactOperations contactOperations;

    public RemotePostingCommentAddedJob() {
        state = new State();
    }

    @Override
    protected void setParameters(String parameters, ObjectMapper objectMapper) {
        this.parameters = objectMapper.readValue(parameters, Parameters.class);
    }

    @Override
    protected void setState(String state, ObjectMapper objectMapper) {
        this.state = objectMapper.readValue(state, State.class);
    }

    @Override
    protected void execute() throws Exception {
        if (parameters.commentRepliedTo != null && !state.repliedToChecked) {
            int count = tx.executeRead(() ->
                ownCommentRepository.countByRemoteCommentId(
                    universalContext.nodeId(),
                    parameters.senderNodeName,
                    parameters.postingId,
                    parameters.commentRepliedTo
                )
            );
            if (count > 0) {
                success(); // We should receive another notification about somebody replied to our comment
            }
            state.repliedToChecked = true;
            checkpoint();
        }

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
            new ForeignCommentAddedLiberin(
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
                parameters.commentSheriffMarks,
                parameters.reason
            )
        );
    }

}
