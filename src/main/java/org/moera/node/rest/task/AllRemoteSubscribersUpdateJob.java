package org.moera.node.rest.task;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import jakarta.inject.Inject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.moera.lib.node.exception.MoeraNodeException;
import org.moera.lib.node.types.Scope;
import org.moera.lib.node.types.SubscriberOperations;
import org.moera.lib.node.types.SubscriberOverride;
import org.moera.lib.node.types.SubscriptionType;
import org.moera.lib.node.types.principal.Principal;
import org.moera.node.data.Subscription;
import org.moera.node.data.SubscriptionRepository;
import org.moera.node.task.Job;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AllRemoteSubscribersUpdateJob
        extends Job<AllRemoteSubscribersUpdateJob.Parameters, AllRemoteSubscribersUpdateJob.State> {

    public static class Parameters {

        private Principal viewPrincipal;

        public Parameters() {
        }

        public Parameters(Principal viewPrincipal) {
            this.viewPrincipal = viewPrincipal;
        }

        public Principal getViewPrincipal() {
            return viewPrincipal;
        }

        public void setViewPrincipal(Principal viewPrincipal) {
            this.viewPrincipal = viewPrincipal;
        }

    }

    public static class State {

        private Set<UUID> updated = new HashSet<>();

        public State() {
        }

        public Set<UUID> getUpdated() {
            return updated;
        }

        public void setUpdated(Set<UUID> updated) {
            this.updated = updated;
        }

    }

    private static final Logger log = LoggerFactory.getLogger(AllRemoteSubscribersUpdateJob.class);

    @Inject
    private SubscriptionRepository subscriptionRepository;

    public AllRemoteSubscribersUpdateJob() {
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
    protected void execute() {
        List<Subscription> subscriptions = subscriptionRepository.findAllByType(nodeId, SubscriptionType.FEED);
        SubscriberOverride override = new SubscriberOverride();
        SubscriberOperations operations = new SubscriberOperations();
        operations.setView(parameters.viewPrincipal);
        override.setOperations(operations);
        for (Subscription subscription : subscriptions) {
            if (state.updated.contains(subscription.getId())) {
                return;
            }

            log.info("Updating subscriber info at node {}", subscription.getRemoteNodeName());
            try {
                nodeApi
                    .at(
                        subscription.getRemoteNodeName(),
                        generateCarte(subscription.getRemoteNodeName(), Scope.SUBSCRIBE)
                    )
                    .updateSubscriber(subscription.getRemoteSubscriberId(), override);
                state.updated.add(subscription.getId());
            } catch (MoeraNodeException e) {
                log.warn(
                    "Error updating subscriber info at node {}: {}",
                    subscription.getRemoteNodeName(), e.getMessage()
                );
            }
        }
        if (state.updated.size() < subscriptions.size()) {
            retry();
        }
    }

}
