package org.moera.node.rest.notification;

import java.util.UUID;
import jakarta.inject.Inject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.moera.node.data.AskHistory;
import org.moera.node.data.AskHistoryRepository;
import org.moera.node.data.SubscriptionType;
import org.moera.node.data.UserSubscriptionRepository;
import org.moera.node.liberin.model.AskSubjectsChangedLiberin;
import org.moera.node.liberin.model.AskedToFriendLiberin;
import org.moera.node.liberin.model.AskedToSubscribeLiberin;
import org.moera.node.media.MediaManager;
import org.moera.node.model.AskSubject;
import org.moera.node.model.AvatarImage;
import org.moera.node.task.Job;
import org.moera.node.util.Transaction;

public class AskedJob extends Job<AskedJob.Parameters, AskedJob.State> {

    public static class Parameters {

        private AskSubject askSubject;
        private String senderNodeName;
        private String senderFullName;
        private String senderGender;
        private AvatarImage senderAvatar;
        private UUID friendGroupId;
        private String friendGroupTitle;
        private String message;

        public Parameters() {
        }

        public Parameters(AskSubject askSubject, String senderNodeName, String senderFullName, String senderGender,
                          AvatarImage senderAvatar, String message) {
            this.askSubject = askSubject;
            this.senderNodeName = senderNodeName;
            this.senderFullName = senderFullName;
            this.senderGender = senderGender;
            this.senderAvatar = senderAvatar;
            this.message = message;
        }

        public Parameters(AskSubject askSubject, String senderNodeName, String senderFullName, String senderGender,
                          AvatarImage senderAvatar, UUID friendGroupId, String friendGroupTitle, String message) {
            this.askSubject = askSubject;
            this.senderNodeName = senderNodeName;
            this.senderFullName = senderFullName;
            this.senderGender = senderGender;
            this.senderAvatar = senderAvatar;
            this.friendGroupId = friendGroupId;
            this.friendGroupTitle = friendGroupTitle;
            this.message = message;
        }

        public AskSubject getAskSubject() {
            return askSubject;
        }

        public void setAskSubject(AskSubject askSubject) {
            this.askSubject = askSubject;
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

        public UUID getFriendGroupId() {
            return friendGroupId;
        }

        public void setFriendGroupId(UUID friendGroupId) {
            this.friendGroupId = friendGroupId;
        }

        public String getFriendGroupTitle() {
            return friendGroupTitle;
        }

        public void setFriendGroupTitle(String friendGroupTitle) {
            this.friendGroupTitle = friendGroupTitle;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

    }

    public static class State {

        private boolean savedToHistory;

        public State() {
        }

        public boolean isSavedToHistory() {
            return savedToHistory;
        }

        public void setSavedToHistory(boolean savedToHistory) {
            this.savedToHistory = savedToHistory;
        }

    }

    @Inject
    private AskHistoryRepository askHistoryRepository;

    @Inject
    private UserSubscriptionRepository userSubscriptionRepository;

    @Inject
    private MediaManager mediaManager;

    @Inject
    private Transaction tx;

    public AskedJob() {
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
        switch (parameters.askSubject) {
            case SUBSCRIBE:
                int count = tx.executeRead(
                    () -> userSubscriptionRepository.countByTypeAndRemoteNode(
                            universalContext.nodeId(), SubscriptionType.FEED, parameters.senderNodeName)
                );
                if (count > 0) {
                    break;
                }

                saveToHistory(parameters.senderNodeName, parameters.askSubject);

                tx.executeWriteWithExceptions(() ->
                        mediaManager.downloadAvatar(parameters.senderNodeName, parameters.senderAvatar));
                universalContext.send(new AskedToSubscribeLiberin(parameters.senderNodeName, parameters.senderFullName,
                        parameters.senderGender, parameters.senderAvatar, parameters.message));
                break;

            case FRIEND: {
                saveToHistory(parameters.senderNodeName, parameters.askSubject);

                tx.executeWriteWithExceptions(() ->
                        mediaManager.downloadAvatar(parameters.senderNodeName, parameters.senderAvatar));
                universalContext.send(new AskedToFriendLiberin(parameters.senderNodeName, parameters.senderFullName,
                        parameters.senderGender, parameters.senderAvatar, parameters.friendGroupId,
                        parameters.friendGroupTitle, parameters.message));
                break;
            }
        }
    }

    private void saveToHistory(String remoteNodeName, AskSubject subject) {
        if (state.savedToHistory) {
            return;
        }

        tx.executeWrite(() -> {
            AskHistory askHistory = new AskHistory();
            askHistory.setId(UUID.randomUUID());
            askHistory.setNodeId(universalContext.nodeId());
            askHistory.setRemoteNodeName(remoteNodeName);
            askHistory.setSubject(subject);
            askHistoryRepository.save(askHistory);
        });

        universalContext.send(new AskSubjectsChangedLiberin());

        state.savedToHistory = true;
        checkpoint();
    }

}
