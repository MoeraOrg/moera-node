package org.moera.node.rest.notification;

import java.util.List;
import jakarta.inject.Inject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.moera.lib.node.types.SubscriptionReason;
import org.moera.node.data.Contact;
import org.moera.node.data.OwnCommentRepository;
import org.moera.node.data.SheriffMark;
import org.moera.node.liberin.model.ForeignCommentAddedLiberin;
import org.moera.node.media.MediaManager;
import org.moera.node.model.AvatarImage;
import org.moera.node.operations.ContactOperations;
import org.moera.node.task.Job;

public class RemotePostingCommentAddedJob
        extends Job<RemotePostingCommentAddedJob.Parameters, RemotePostingCommentAddedJob.State> {

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
        private String commentOwnerName;
        private String commentOwnerFullName;
        private String commentOwnerGender;
        private AvatarImage commentOwnerAvatar;
        private String commentHeading;
        private List<SheriffMark> commentSheriffMarks;
        private String commentRepliedTo;
        private SubscriptionReason reason;

        public Parameters() {
        }

        public Parameters(String senderNodeName, String postingId, String postingOwnerName, String postingOwnerFullName,
                          String postingOwnerGender, AvatarImage postingOwnerAvatar, String postingHeading,
                          List<String> postingSheriffs, List<SheriffMark> postingSheriffMarks, String commentId,
                          String commentOwnerName, String commentOwnerFullName, String commentOwnerGender,
                          AvatarImage commentOwnerAvatar, String commentHeading, List<SheriffMark> commentSheriffMarks,
                          String commentRepliedTo, SubscriptionReason reason) {
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
            this.commentOwnerName = commentOwnerName;
            this.commentOwnerFullName = commentOwnerFullName;
            this.commentOwnerGender = commentOwnerGender;
            this.commentOwnerAvatar = commentOwnerAvatar;
            this.commentHeading = commentHeading;
            this.commentSheriffMarks = commentSheriffMarks;
            this.commentRepliedTo = commentRepliedTo;
            this.reason = reason;
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

        public String getCommentHeading() {
            return commentHeading;
        }

        public void setCommentHeading(String commentHeading) {
            this.commentHeading = commentHeading;
        }

        public List<SheriffMark> getCommentSheriffMarks() {
            return commentSheriffMarks;
        }

        public void setCommentSheriffMarks(List<SheriffMark> commentSheriffMarks) {
            this.commentSheriffMarks = commentSheriffMarks;
        }

        public String getCommentRepliedTo() {
            return commentRepliedTo;
        }

        public void setCommentRepliedTo(String commentRepliedTo) {
            this.commentRepliedTo = commentRepliedTo;
        }

        public SubscriptionReason getReason() {
            return reason;
        }

        public void setReason(SubscriptionReason reason) {
            this.reason = reason;
        }

    }

    public static class State {

        private boolean repliedToChecked;
        private boolean closenessUpdated;

        public State() {
        }

        public boolean isRepliedToChecked() {
            return repliedToChecked;
        }

        public void setRepliedToChecked(boolean repliedToChecked) {
            this.repliedToChecked = repliedToChecked;
        }

        public boolean isClosenessUpdated() {
            return closenessUpdated;
        }

        public void setClosenessUpdated(boolean closenessUpdated) {
            this.closenessUpdated = closenessUpdated;
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
    protected void setParameters(String parameters, ObjectMapper objectMapper) throws JsonProcessingException {
        this.parameters = objectMapper.readValue(parameters, Parameters.class);
    }

    @Override
    protected void setState(String state, ObjectMapper objectMapper) throws JsonProcessingException {
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
                            parameters.commentRepliedTo));
            if (count > 0) {
                success(); // We should receive another notification about somebody replied to our comment
            }
            state.repliedToChecked = true;
            checkpoint();
        }

        Contact.toAvatar(
                contactOperations.find(parameters.postingOwnerName),
                parameters.postingOwnerAvatar);
        if (!state.closenessUpdated) {
            Contact.toAvatar(
                    contactOperations.updateCloseness(parameters.commentOwnerName, 1),
                    parameters.commentOwnerAvatar);
            state.closenessUpdated = true;
            checkpoint();
        }
        tx.executeWriteWithExceptions(() ->
                mediaManager.downloadAvatars(parameters.senderNodeName,
                        new AvatarImage[] {parameters.postingOwnerAvatar, parameters.commentOwnerAvatar}));

        universalContext.send(
                new ForeignCommentAddedLiberin(parameters.senderNodeName, parameters.postingOwnerName,
                        parameters.postingOwnerFullName, parameters.postingOwnerGender, parameters.postingOwnerAvatar,
                        parameters.postingId, parameters.postingHeading, parameters.postingSheriffs,
                        parameters.postingSheriffMarks, parameters.commentOwnerName, parameters.commentOwnerFullName,
                        parameters.commentOwnerGender, parameters.commentOwnerAvatar, parameters.commentId,
                        parameters.commentHeading, parameters.commentSheriffMarks, parameters.reason));
    }

}
