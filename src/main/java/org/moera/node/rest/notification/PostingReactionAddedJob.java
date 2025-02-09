package org.moera.node.rest.notification;

import jakarta.inject.Inject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.moera.node.data.Contact;
import org.moera.node.liberin.model.RemoteCommentMediaReactionAddedLiberin;
import org.moera.node.liberin.model.RemotePostingMediaReactionAddedLiberin;
import org.moera.node.media.MediaManager;
import org.moera.node.model.AvatarImage;
import org.moera.node.operations.ContactOperations;
import org.moera.node.task.Job;

public class PostingReactionAddedJob extends Job<PostingReactionAddedJob.Parameters, Object> {

    public static class Parameters {

        private String senderNodeName;
        private String ownerName;
        private String ownerFullName;
        private String ownerGender;
        private AvatarImage ownerAvatar;
        private boolean negative;
        private int emoji;
        private String parentPostingNodeName;
        private String parentPostingFullName;
        private String parentPostingGender;
        private AvatarImage parentPostingAvatar;
        private String parentPostingId;
        private String parentHeading;
        private String parentCommentId;
        private String parentMediaId;
        private String postingId;

        public Parameters() {
        }

        public Parameters(String senderNodeName, String ownerName, String ownerFullName, String ownerGender,
                          AvatarImage ownerAvatar, boolean negative, int emoji, String parentPostingNodeName,
                          String parentPostingFullName, String parentPostingGender, AvatarImage parentPostingAvatar,
                          String parentPostingId, String parentHeading, String parentCommentId, String parentMediaId,
                          String postingId) {
            this.senderNodeName = senderNodeName;
            this.ownerName = ownerName;
            this.ownerFullName = ownerFullName;
            this.ownerGender = ownerGender;
            this.ownerAvatar = ownerAvatar;
            this.negative = negative;
            this.emoji = emoji;
            this.parentPostingNodeName = parentPostingNodeName;
            this.parentPostingFullName = parentPostingFullName;
            this.parentPostingGender = parentPostingGender;
            this.parentPostingAvatar = parentPostingAvatar;
            this.parentPostingId = parentPostingId;
            this.parentHeading = parentHeading;
            this.parentCommentId = parentCommentId;
            this.parentMediaId = parentMediaId;
            this.postingId = postingId;
        }

        public String getSenderNodeName() {
            return senderNodeName;
        }

        public void setSenderNodeName(String senderNodeName) {
            this.senderNodeName = senderNodeName;
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

        public String getParentPostingNodeName() {
            return parentPostingNodeName;
        }

        public void setParentPostingNodeName(String parentPostingNodeName) {
            this.parentPostingNodeName = parentPostingNodeName;
        }

        public String getParentPostingFullName() {
            return parentPostingFullName;
        }

        public void setParentPostingFullName(String parentPostingFullName) {
            this.parentPostingFullName = parentPostingFullName;
        }

        public String getParentPostingGender() {
            return parentPostingGender;
        }

        public void setParentPostingGender(String parentPostingGender) {
            this.parentPostingGender = parentPostingGender;
        }

        public AvatarImage getParentPostingAvatar() {
            return parentPostingAvatar;
        }

        public void setParentPostingAvatar(AvatarImage parentPostingAvatar) {
            this.parentPostingAvatar = parentPostingAvatar;
        }

        public String getParentPostingId() {
            return parentPostingId;
        }

        public void setParentPostingId(String parentPostingId) {
            this.parentPostingId = parentPostingId;
        }

        public String getParentHeading() {
            return parentHeading;
        }

        public void setParentHeading(String parentHeading) {
            this.parentHeading = parentHeading;
        }

        public String getParentCommentId() {
            return parentCommentId;
        }

        public void setParentCommentId(String parentCommentId) {
            this.parentCommentId = parentCommentId;
        }

        public String getParentMediaId() {
            return parentMediaId;
        }

        public void setParentMediaId(String parentMediaId) {
            this.parentMediaId = parentMediaId;
        }

        public String getPostingId() {
            return postingId;
        }

        public void setPostingId(String postingId) {
            this.postingId = postingId;
        }

    }

    @Inject
    private MediaManager mediaManager;

    @Inject
    private ContactOperations contactOperations;

    public PostingReactionAddedJob() {
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
