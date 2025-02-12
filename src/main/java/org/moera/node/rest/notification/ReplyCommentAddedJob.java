package org.moera.node.rest.notification;

import java.util.List;
import jakarta.inject.Inject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.moera.lib.node.types.AvatarImage;
import org.moera.lib.node.types.SheriffMark;
import org.moera.node.data.Contact;
import org.moera.node.liberin.model.ReplyCommentAddedLiberin;
import org.moera.node.media.MediaManager;
import org.moera.node.operations.ContactOperations;
import org.moera.node.task.Job;

public class ReplyCommentAddedJob extends Job<ReplyCommentAddedJob.Parameters, Object> {

    public static class Parameters {

        private String senderNodeName;
        private String postingId;
        private String postingOwnerName;
        private String postingOwnerFullName;
        private String postingOwnerGender;
        private AvatarImage postingOwnerAvatar;
        private String postingHeading;
        private List<String> postingSheriffs;
        private List<SheriffMark> postingSheriffMarks;
        private String commentId;
        private String repliedToId;
        private String repliedToHeading;
        private String commentOwnerName;
        private String commentOwnerFullName;
        private String commentOwnerGender;
        private AvatarImage commentOwnerAvatar;
        private List<SheriffMark> commentSheriffMarks;

        public Parameters() {
        }

        public Parameters(String senderNodeName, String postingId, String postingOwnerName, String postingOwnerFullName,
                          String postingOwnerGender, AvatarImage postingOwnerAvatar, String postingHeading,
                          List<String> postingSheriffs, List<SheriffMark> postingSheriffMarks, String commentId,
                          String repliedToId, String repliedToHeading, String commentOwnerName,
                          String commentOwnerFullName, String commentOwnerGender, AvatarImage commentOwnerAvatar,
                          List<SheriffMark> commentSheriffMarks) {
            this.senderNodeName = senderNodeName;
            this.postingId = postingId;
            this.postingOwnerName = postingOwnerName;
            this.postingOwnerFullName = postingOwnerFullName;
            this.postingOwnerGender = postingOwnerGender;
            this.postingOwnerAvatar = postingOwnerAvatar;
            this.postingHeading = postingHeading;
            this.postingSheriffs = postingSheriffs;
            this.postingSheriffMarks = postingSheriffMarks;
            this.commentId = commentId;
            this.repliedToId = repliedToId;
            this.repliedToHeading = repliedToHeading;
            this.commentOwnerName = commentOwnerName;
            this.commentOwnerFullName = commentOwnerFullName;
            this.commentOwnerGender = commentOwnerGender;
            this.commentOwnerAvatar = commentOwnerAvatar;
            this.commentSheriffMarks = commentSheriffMarks;
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

        public String getPostingOwnerName() {
            return postingOwnerName;
        }

        public void setPostingOwnerName(String postingOwnerName) {
            this.postingOwnerName = postingOwnerName;
        }

        public String getPostingOwnerFullName() {
            return postingOwnerFullName;
        }

        public void setPostingOwnerFullName(String postingOwnerFullName) {
            this.postingOwnerFullName = postingOwnerFullName;
        }

        public String getPostingOwnerGender() {
            return postingOwnerGender;
        }

        public void setPostingOwnerGender(String postingOwnerGender) {
            this.postingOwnerGender = postingOwnerGender;
        }

        public AvatarImage getPostingOwnerAvatar() {
            return postingOwnerAvatar;
        }

        public void setPostingOwnerAvatar(AvatarImage postingOwnerAvatar) {
            this.postingOwnerAvatar = postingOwnerAvatar;
        }

        public String getPostingHeading() {
            return postingHeading;
        }

        public void setPostingHeading(String postingHeading) {
            this.postingHeading = postingHeading;
        }

        public List<String> getPostingSheriffs() {
            return postingSheriffs;
        }

        public void setPostingSheriffs(List<String> postingSheriffs) {
            this.postingSheriffs = postingSheriffs;
        }

        public List<SheriffMark> getPostingSheriffMarks() {
            return postingSheriffMarks;
        }

        public void setPostingSheriffMarks(List<SheriffMark> postingSheriffMarks) {
            this.postingSheriffMarks = postingSheriffMarks;
        }

        public String getCommentId() {
            return commentId;
        }

        public void setCommentId(String commentId) {
            this.commentId = commentId;
        }

        public String getRepliedToId() {
            return repliedToId;
        }

        public void setRepliedToId(String repliedToId) {
            this.repliedToId = repliedToId;
        }

        public String getRepliedToHeading() {
            return repliedToHeading;
        }

        public void setRepliedToHeading(String repliedToHeading) {
            this.repliedToHeading = repliedToHeading;
        }

        public String getCommentOwnerName() {
            return commentOwnerName;
        }

        public void setCommentOwnerName(String commentOwnerName) {
            this.commentOwnerName = commentOwnerName;
        }

        public String getCommentOwnerFullName() {
            return commentOwnerFullName;
        }

        public void setCommentOwnerFullName(String commentOwnerFullName) {
            this.commentOwnerFullName = commentOwnerFullName;
        }

        public String getCommentOwnerGender() {
            return commentOwnerGender;
        }

        public void setCommentOwnerGender(String commentOwnerGender) {
            this.commentOwnerGender = commentOwnerGender;
        }

        public AvatarImage getCommentOwnerAvatar() {
            return commentOwnerAvatar;
        }

        public void setCommentOwnerAvatar(AvatarImage commentOwnerAvatar) {
            this.commentOwnerAvatar = commentOwnerAvatar;
        }

        public List<SheriffMark> getCommentSheriffMarks() {
            return commentSheriffMarks;
        }

        public void setCommentSheriffMarks(List<SheriffMark> commentSheriffMarks) {
            this.commentSheriffMarks = commentSheriffMarks;
        }

    }

    @Inject
    private MediaManager mediaManager;

    @Inject
    private ContactOperations contactOperations;

    public ReplyCommentAddedJob() {
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
