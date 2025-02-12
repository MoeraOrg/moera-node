package org.moera.node.rest.notification;

import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import jakarta.inject.Inject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.moera.lib.node.types.AvatarImage;
import org.moera.lib.node.types.FriendGroupDetails;
import org.moera.node.data.Contact;
import org.moera.node.data.ContactRepository;
import org.moera.node.data.FriendOf;
import org.moera.node.data.FriendOfRepository;
import org.moera.node.liberin.model.RemoteFriendshipUpdatedLiberin;
import org.moera.node.media.MediaManager;
import org.moera.node.model.AvatarImageUtil;
import org.moera.node.operations.ContactOperations;
import org.moera.node.task.Job;
import org.moera.node.util.Util;

public class FriendshipUpdatedJob extends Job<FriendshipUpdatedJob.Parameters, Object> {

    public static class Parameters {

        private String senderNodeName;
        private AvatarImage senderAvatar;
        private FriendGroupDetails[] friendGroups;

        public Parameters() {
        }

        public Parameters(String senderNodeName, AvatarImage senderAvatar, FriendGroupDetails[] friendGroups) {
            this.senderNodeName = senderNodeName;
            this.senderAvatar = senderAvatar;
            this.friendGroups = friendGroups;
        }

        public String getSenderNodeName() {
            return senderNodeName;
        }

        public void setSenderNodeName(String senderNodeName) {
            this.senderNodeName = senderNodeName;
        }

        public AvatarImage getSenderAvatar() {
            return senderAvatar;
        }

        public void setSenderAvatar(AvatarImage senderAvatar) {
            this.senderAvatar = senderAvatar;
        }

        public FriendGroupDetails[] getFriendGroups() {
            return friendGroups;
        }

        public void setFriendGroups(FriendGroupDetails[] friendGroups) {
            this.friendGroups = friendGroups;
        }

    }

    @Inject
    private FriendOfRepository friendOfRepository;

    @Inject
    private ContactRepository contactRepository;

    @Inject
    private MediaManager mediaManager;

    @Inject
    private ContactOperations contactOperations;

    public FriendshipUpdatedJob() {
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
        tx.executeWriteWithExceptions(() ->
                mediaManager.downloadAvatar(parameters.senderNodeName, parameters.senderAvatar));

        tx.executeWrite(() -> {
            Map<String, FriendOf> previous = friendOfRepository.findByNodeIdAndRemoteNode(
                            universalContext.nodeId(), parameters.senderNodeName).stream()
                    .collect(Collectors.toMap(FriendOf::getRemoteGroupId, Function.identity()));
            Map<String, FriendGroupDetails> current = Arrays.stream(parameters.friendGroups)
                    .collect(Collectors.toMap(FriendGroupDetails::getId, Function.identity()));

            RemoteFriendshipUpdatedLiberin liberin = new RemoteFriendshipUpdatedLiberin();
            Contact contact = contactOperations.find(parameters.senderNodeName);

            for (var prev : previous.entrySet()) {
                if (!current.containsKey(prev.getKey())) {
                    friendOfRepository.delete(prev.getValue());
                    // FIXME this is a separate transaction. If the outer transaction fails, the count will remain wrong
                    contactOperations.updateFriendOfCount(parameters.senderNodeName, -1);
                    liberin.getDeleted().add(prev.getValue());
                }
            }

            for (var curr : current.entrySet()) {
                FriendOf friendOf = previous.get(curr.getKey());
                if (friendOf == null) {
                    friendOf = new FriendOf();
                    friendOf.setId(UUID.randomUUID());
                    friendOf.setNodeId(universalContext.nodeId());
                    friendOf.setRemoteNodeName(parameters.senderNodeName);
                    friendOf.setContact(contact);
                    friendOf.setRemoteGroupId(curr.getValue().getId());
                    friendOf.setRemoteAddedAt(Util.toTimestamp(curr.getValue().getAddedAt()));
                    friendOf = friendOfRepository.save(friendOf);
                    liberin.getAdded().add(friendOf);
                }
                friendOf.setRemoteGroupTitle(curr.getValue().getTitle());
                liberin.getCurrent().add(friendOf);
            }

            // FIXME see above
            contactOperations.updateFriendOfCount(parameters.senderNodeName, liberin.getAdded().size());
            if (parameters.senderAvatar != null) {
                contactRepository.updateRemoteAvatar(
                    universalContext.nodeId(),
                    contact.getRemoteNodeName(),
                    AvatarImageUtil.getMediaFile(parameters.senderAvatar),
                    parameters.senderAvatar.getShape()
                );
                contact.setRemoteAvatarMediaFile(AvatarImageUtil.getMediaFile(parameters.senderAvatar));
                contact.setRemoteAvatarShape(parameters.senderAvatar.getShape());
            }
            liberin.setContact(contact);
            universalContext.send(liberin);
        });
    }

}
