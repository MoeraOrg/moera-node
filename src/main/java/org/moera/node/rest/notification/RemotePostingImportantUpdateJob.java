package org.moera.node.rest.notification;

import jakarta.inject.Inject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.moera.lib.node.types.AvatarImage;
import org.moera.node.data.Contact;
import org.moera.node.liberin.model.RemotePostingImportantUpdateLiberin;
import org.moera.node.media.MediaManager;
import org.moera.node.operations.ContactOperations;
import org.moera.node.task.Job;

public class RemotePostingImportantUpdateJob extends Job<RemotePostingImportantUpdateJob.Parameters, Object> {

    public static class Parameters {

        private String senderNodeName;
        private String postingId;
        private String postingOwnerName;
        private String postingOwnerFullName;
        private String postingOwnerGender;
        private AvatarImage postingOwnerAvatar;
        private String postingHeading;
        private String description;

        public Parameters() {
        }

        public Parameters(String senderNodeName, String postingId, String postingOwnerName, String postingOwnerFullName,
                          String postingOwnerGender, AvatarImage postingOwnerAvatar, String postingHeading,
                          String description) {
            this.senderNodeName = senderNodeName;
            this.postingId = postingId;
            this.postingOwnerName = postingOwnerName;
            this.postingOwnerFullName = postingOwnerFullName;
            this.postingOwnerGender = postingOwnerGender;
            this.postingOwnerAvatar = postingOwnerAvatar;
            this.postingHeading = postingHeading;
            this.description = description;
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

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

    }

    @Inject
    private MediaManager mediaManager;

    @Inject
    private ContactOperations contactOperations;

    public RemotePostingImportantUpdateJob() {
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
        tx.executeWriteWithExceptions(() ->
                mediaManager.downloadAvatar(parameters.senderNodeName, parameters.postingOwnerAvatar));
        universalContext.send(
                new RemotePostingImportantUpdateLiberin(parameters.senderNodeName, parameters.postingOwnerName,
                        parameters.postingOwnerFullName, parameters.postingOwnerGender, parameters.postingOwnerAvatar,
                        parameters.postingId, parameters.postingHeading, parameters.description));
    }

}
