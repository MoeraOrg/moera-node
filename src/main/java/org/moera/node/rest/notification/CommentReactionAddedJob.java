package org.moera.node.rest.notification;

import jakarta.inject.Inject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.moera.lib.node.types.AvatarImage;
import org.moera.node.data.Contact;
import org.moera.node.liberin.model.RemoteCommentReactionAddedLiberin;
import org.moera.node.media.MediaManager;
import org.moera.node.operations.ContactOperations;
import org.moera.node.task.Job;

public class CommentReactionAddedJob extends Job<CommentReactionAddedJob.Parameters, CommentReactionAddedJob.State> {

    public static class Parameters {

        private String senderNodeName;
        private String postingId;
        private String postingNodeName;
        private String postingFullName;
        private String postingGender;
        private AvatarImage postingAvatar;
        private String postingHeading;
        private String commentId;
        private String commentHeading;
        private String ownerName;
        private String ownerFullName;
        private String ownerGender;
        private AvatarImage ownerAvatar;
        private boolean negative;
        private int emoji;

        public Parameters() {
        }

        public Parameters(
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
            this.senderNodeName = senderNodeName;
            this.postingId = postingId;
            this.postingNodeName = postingNodeName;
            this.postingFullName = postingFullName;
            this.postingGender = postingGender;
            this.postingAvatar = postingAvatar;
            this.postingHeading = postingHeading;
            this.commentId = commentId;
            this.commentHeading = commentHeading;
            this.ownerName = ownerName;
            this.ownerFullName = ownerFullName;
            this.ownerGender = ownerGender;
            this.ownerAvatar = ownerAvatar;
            this.negative = negative;
            this.emoji = emoji;
        }

        public String getSenderNodeName() {
            return senderNodeName;
        }

        public void setSenderNodeName(String senderNodeName) {
            this.senderNodeName = senderNodeName;
        }

        public String getPostingId() {
            return postingId;
        }

        public void setPostingId(String postingId) {
            this.postingId = postingId;
        }

        public String getPostingNodeName() {
            return postingNodeName;
        }

        public void setPostingNodeName(String postingNodeName) {
            this.postingNodeName = postingNodeName;
        }

        public String getPostingFullName() {
            return postingFullName;
        }

        public void setPostingFullName(String postingFullName) {
            this.postingFullName = postingFullName;
        }

        public String getPostingGender() {
            return postingGender;
        }

        public void setPostingGender(String postingGender) {
            this.postingGender = postingGender;
        }

        public AvatarImage getPostingAvatar() {
            return postingAvatar;
        }

        public void setPostingAvatar(AvatarImage postingAvatar) {
            this.postingAvatar = postingAvatar;
        }

        public String getPostingHeading() {
            return postingHeading;
        }

        public void setPostingHeading(String postingHeading) {
            this.postingHeading = postingHeading;
        }

        public String getCommentId() {
            return commentId;
        }

        public void setCommentId(String commentId) {
            this.commentId = commentId;
        }

        public String getCommentHeading() {
            return commentHeading;
        }

        public void setCommentHeading(String commentHeading) {
            this.commentHeading = commentHeading;
        }

        public String getOwnerName() {
            return ownerName;
        }

        public void setOwnerName(String ownerName) {
            this.ownerName = ownerName;
        }

        public String getOwnerFullName() {
            return ownerFullName;
        }

        public void setOwnerFullName(String ownerFullName) {
            this.ownerFullName = ownerFullName;
        }

        public String getOwnerGender() {
            return ownerGender;
        }

        public void setOwnerGender(String ownerGender) {
            this.ownerGender = ownerGender;
        }

        public AvatarImage getOwnerAvatar() {
            return ownerAvatar;
        }

        public void setOwnerAvatar(AvatarImage ownerAvatar) {
            this.ownerAvatar = ownerAvatar;
        }

        public boolean isNegative() {
            return negative;
        }

        public void setNegative(boolean negative) {
            this.negative = negative;
        }

        public int getEmoji() {
            return emoji;
        }

        public void setEmoji(int emoji) {
            this.emoji = emoji;
        }

    }

    public static class State {

        private boolean closenessUpdated;

        public State() {
        }

        public boolean isClosenessUpdated() {
            return closenessUpdated;
        }

        public void setClosenessUpdated(boolean closenessUpdated) {
            this.closenessUpdated = closenessUpdated;
        }

    }

    @Inject
    private MediaManager mediaManager;

    @Inject
    private ContactOperations contactOperations;

    public CommentReactionAddedJob() {
        state = new State();
    }

    @Override
    protected void setParameters(String parameters, ObjectMapper objectMapper) throws JsonProcessingException {
        this.parameters = objectMapper.readValue(parameters, Parameters.class);
    }

    @Override
    protected void setState(String state, ObjectMapper objectMapper) throws JsonProcessingException {
        this.state = objectMapper.readValue(state, State.class);
    }

    @Override
    protected void execute() throws Exception {
        Contact.toAvatar(
            contactOperations.find(parameters.postingNodeName),
            parameters.postingAvatar
        );
        if (!state.closenessUpdated) {
            Contact.toAvatar(
                contactOperations.updateCloseness(parameters.getOwnerName(), 0.1f),
                parameters.ownerAvatar
            );
            state.closenessUpdated = true;
            checkpoint();
        }

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
