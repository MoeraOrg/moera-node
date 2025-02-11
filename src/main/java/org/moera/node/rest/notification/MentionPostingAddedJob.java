package org.moera.node.rest.notification;

import java.util.List;
import jakarta.inject.Inject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.moera.lib.node.types.SubscriptionReason;
import org.moera.node.data.Contact;
import org.moera.node.data.SheriffMark;
import org.moera.node.liberin.model.MentionInRemotePostingAddedLiberin;
import org.moera.node.media.MediaManager;
import org.moera.node.model.AvatarImage;
import org.moera.node.operations.ContactOperations;
import org.moera.node.operations.SubscriptionOperations;
import org.moera.node.task.Job;

public class MentionPostingAddedJob extends Job<MentionPostingAddedJob.Parameters, Object> {

    public static class Parameters {

        private String senderNodeName;
        private String postingId;
        private String ownerName;
        private String ownerFullName;
        private String ownerGender;
        private AvatarImage ownerAvatar;
        private String heading;
        private List<String> sheriffs;
        private List<SheriffMark> sheriffMarks;

        public Parameters() {
        }

        public Parameters(String senderNodeName, String postingId, String ownerName, String ownerFullName,
                          String ownerGender, AvatarImage ownerAvatar, String heading, List<String> sheriffs,
                          List<SheriffMark> sheriffMarks) {
            this.senderNodeName = senderNodeName;
            this.postingId = postingId;
            this.ownerName = ownerName;
            this.ownerFullName = ownerFullName;
            this.ownerGender = ownerGender;
            this.ownerAvatar = ownerAvatar;
            this.heading = heading;
            this.sheriffs = sheriffs;
            this.sheriffMarks = sheriffMarks;
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

        public String getHeading() {
            return heading;
        }

        public void setHeading(String heading) {
            this.heading = heading;
        }

        public List<String> getSheriffs() {
            return sheriffs;
        }

        public void setSheriffs(List<String> sheriffs) {
            this.sheriffs = sheriffs;
        }

        public List<SheriffMark> getSheriffMarks() {
            return sheriffMarks;
        }

        public void setSheriffMarks(List<SheriffMark> sheriffMarks) {
            this.sheriffMarks = sheriffMarks;
        }

    }

    @Inject
    private SubscriptionOperations subscriptionOperations;

    @Inject
    private MediaManager mediaManager;

    @Inject
    private ContactOperations contactOperations;

    public MentionPostingAddedJob() {
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
                contactOperations.find(parameters.ownerName),
                parameters.ownerAvatar);

        tx.executeWriteWithExceptions(() ->
                mediaManager.downloadAvatar(parameters.senderNodeName, parameters.ownerAvatar));

        universalContext.send(
                new MentionInRemotePostingAddedLiberin(parameters.senderNodeName, parameters.ownerName,
                        parameters.ownerFullName, parameters.ownerGender, parameters.ownerAvatar, parameters.postingId,
                        parameters.heading, parameters.sheriffs, parameters.sheriffMarks));

        tx.executeWrite(() ->
                subscriptionOperations.subscribeToPostingComments(
                        parameters.senderNodeName,
                        parameters.postingId,
                        SubscriptionReason.MENTION));
    }

}
