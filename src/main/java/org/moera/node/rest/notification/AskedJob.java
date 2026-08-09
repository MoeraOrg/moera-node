package org.moera.node.rest.notification;

import java.util.UUID;
import jakarta.inject.Inject;

import org.moera.lib.node.types.AskSubject;
import org.moera.lib.node.types.AvatarImage;
import org.moera.lib.node.types.SubscriptionType;
import org.moera.node.data.AskHistory;
import org.moera.node.data.AskHistoryRepository;
import org.moera.node.data.UserSubscriptionRepository;
import org.moera.node.liberin.model.AskSubjectsChangedLiberin;
import org.moera.node.liberin.model.AskedToFriendLiberin;
import org.moera.node.liberin.model.AskedToSubscribeLiberin;
import org.moera.node.media.MediaManager;
import org.moera.node.task.Job;
import org.moera.node.util.Transaction;
import tools.jackson.databind.ObjectMapper;

public class AskedJob extends Job<AskedJob.Parameters, AskedJob.State> {

    public record Parameters(
        AskSubject askSubject,
        String senderNodeName,
        String senderFullName,
        String senderGender,
        AvatarImage senderAvatar,
        UUID friendGroupId,
        String friendGroupTitle,
        String message
    ) {

        public Parameters(
            AskSubject askSubject,
            String senderNodeName,
            String senderFullName,
            String senderGender,
            AvatarImage senderAvatar,
            String message
        ) {
            this(askSubject, senderNodeName, senderFullName, senderGender, senderAvatar, null, null, message);
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
    protected void setParameters(String parameters, ObjectMapper objectMapper) {
        this.parameters = objectMapper.readValue(parameters, Parameters.class);
    }

    @Override
    protected void setState(String state, ObjectMapper objectMapper) {
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
