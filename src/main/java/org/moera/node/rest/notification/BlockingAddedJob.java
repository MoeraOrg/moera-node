package org.moera.node.rest.notification;

import java.util.UUID;
import javax.inject.Inject;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.moera.node.data.BlockedByUser;
import org.moera.node.data.BlockedByUserRepository;
import org.moera.node.data.BlockedOperation;
import org.moera.node.data.Contact;
import org.moera.node.data.ContactRepository;
import org.moera.node.liberin.model.BlockedByUserAddedLiberin;
import org.moera.node.media.MediaManager;
import org.moera.node.model.AvatarImage;
import org.moera.node.operations.ContactOperations;
import org.moera.node.task.Job;
import org.moera.node.util.Util;

public class BlockingAddedJob extends Job<BlockingAddedJob.Parameters, BlockingAddedJob.State> {

    public static class Parameters {

        private String senderNodeName;
        private String senderFullName;
        private String senderGender;
        private AvatarImage senderAvatar;
        private BlockedOperation blockedOperation;
        private String postingId;
        private String postingHeading;
        private Long deadline;
        private String reason;

        public Parameters() {
        }

        public Parameters(String senderNodeName, String senderFullName, String senderGender, AvatarImage senderAvatar,
                          BlockedOperation blockedOperation, String postingId, String postingHeading, Long deadline,
                          String reason) {
            this.senderNodeName = senderNodeName;
            this.senderFullName = senderFullName;
            this.senderGender = senderGender;
            this.senderAvatar = senderAvatar;
            this.blockedOperation = blockedOperation;
            this.postingId = postingId;
            this.postingHeading = postingHeading;
            this.deadline = deadline;
            this.reason = reason;
        }

        public String getSenderNodeName() {
            return senderNodeName;
        }

        public void setSenderNodeName(String senderNodeName) {
            this.senderNodeName = senderNodeName;
        }

        public String getSenderFullName() {
            return senderFullName;
        }

        public void setSenderFullName(String senderFullName) {
            this.senderFullName = senderFullName;
        }

        public String getSenderGender() {
            return senderGender;
        }

        public void setSenderGender(String senderGender) {
            this.senderGender = senderGender;
        }

        public AvatarImage getSenderAvatar() {
            return senderAvatar;
        }

        public void setSenderAvatar(AvatarImage senderAvatar) {
            this.senderAvatar = senderAvatar;
        }

        public BlockedOperation getBlockedOperation() {
            return blockedOperation;
        }

        public void setBlockedOperation(BlockedOperation blockedOperation) {
            this.blockedOperation = blockedOperation;
        }

        public String getPostingId() {
            return postingId;
        }

        public void setPostingId(String postingId) {
            this.postingId = postingId;
        }

        public String getPostingHeading() {
            return postingHeading;
        }

        public void setPostingHeading(String postingHeading) {
            this.postingHeading = postingHeading;
        }

        public Long getDeadline() {
            return deadline;
        }

        public void setDeadline(Long deadline) {
            this.deadline = deadline;
        }

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }

    }

    public static class State {

        private boolean contactDetailsUpdated;
        private UUID blockedByUserId;
        private boolean blockedByUserCountsUpdated;

        @JsonIgnore
        private Contact contact;

        @JsonIgnore
        private BlockedByUser blockedByUser;

        public State() {
        }

        public boolean isContactDetailsUpdated() {
            return contactDetailsUpdated;
        }

        public void setContactDetailsUpdated(boolean contactDetailsUpdated) {
            this.contactDetailsUpdated = contactDetailsUpdated;
        }

        public UUID getBlockedByUserId() {
            return blockedByUserId;
        }

        public void setBlockedByUserId(UUID blockedByUserId) {
            this.blockedByUserId = blockedByUserId;
        }

        public boolean isBlockedByUserCountsUpdated() {
            return blockedByUserCountsUpdated;
        }

        public void setBlockedByUserCountsUpdated(boolean blockedByUserCountsUpdated) {
            this.blockedByUserCountsUpdated = blockedByUserCountsUpdated;
        }

    }

    @Inject
    private BlockedByUserRepository blockedByUserRepository;

    @Inject
    private ContactRepository contactRepository;

    @Inject
    private ContactOperations contactOperations;

    @Inject
    private MediaManager mediaManager;

    public BlockingAddedJob() {
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
        if (!state.contactDetailsUpdated) {
            state.contact = contactOperations.updateDetails(parameters.senderNodeName, parameters.senderFullName,
                    parameters.senderGender);
            state.contactDetailsUpdated = true;
            checkpoint();
        }

        if (state.blockedByUserId == null) {
            tx.executeWrite(() -> {
                if (state.contact == null) {
                    state.contact = contactRepository.findByRemoteNode(nodeId, parameters.senderNodeName)
                            .orElse(null);
                }
                state.blockedByUserId = UUID.randomUUID();

                state.blockedByUser = new BlockedByUser();
                state.blockedByUser.setId(state.blockedByUserId);
                state.blockedByUser.setNodeId(universalContext.nodeId());
                state.blockedByUser.setBlockedOperation(parameters.blockedOperation);
                state.blockedByUser.setContact(state.contact);
                state.blockedByUser.setRemoteNodeName(parameters.senderNodeName);
                state.blockedByUser.setRemotePostingId(parameters.postingId);
                state.blockedByUser.setDeadline(Util.toTimestamp(parameters.deadline));
                state.blockedByUser.setReason(parameters.reason != null ? parameters.reason : "");
                return blockedByUserRepository.save(state.blockedByUser);
            });
            checkpoint();
        } else {
            tx.executeRead(() -> {
                state.blockedByUser = blockedByUserRepository.findByNodeIdAndId(nodeId, state.blockedByUserId)
                        .orElse(null);
                if (state.blockedByUser != null) {
                    state.contact = state.blockedByUser.getContact();
                }
            });
        }

        if (!state.blockedByUserCountsUpdated) {
            state.contact = contactOperations.updateBlockedByUserCounts(state.blockedByUser, 1);
            state.blockedByUserCountsUpdated = true;
            checkpoint();
        }

        Contact.toAvatar(state.contact, parameters.senderAvatar);
        if (parameters.senderAvatar != null) {
            tx.executeWriteWithExceptions(
                () -> {
                    mediaManager.downloadAvatar(parameters.senderNodeName, parameters.senderAvatar);
                    contactRepository.updateRemoteAvatar(
                            universalContext.nodeId(),
                            parameters.senderNodeName,
                            parameters.senderAvatar.getMediaFile(),
                            parameters.senderAvatar.getShape()
                    );
                    state.contact.setRemoteAvatarMediaFile(parameters.senderAvatar.getMediaFile());
                    state.contact.setRemoteAvatarShape(parameters.senderAvatar.getShape());
                }
            );
        }

        BlockedByUserAddedLiberin liberin =
                new BlockedByUserAddedLiberin(state.blockedByUser, parameters.postingHeading);
        liberin.getBlockedByUser().setContact(state.contact);
        universalContext.send(liberin);
    }

}
