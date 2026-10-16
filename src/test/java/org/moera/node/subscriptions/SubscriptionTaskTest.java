package org.moera.node.subscriptions;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.moera.lib.node.MoeraNode;
import org.moera.lib.node.types.Scope;
import org.moera.lib.node.types.SubscriberDescription;
import org.moera.lib.node.types.SubscriberInfo;
import org.moera.lib.node.types.SubscriptionType;
import org.moera.lib.node.types.principal.Principal;
import org.moera.node.api.node.NodeApi;
import org.moera.node.data.Feed;
import org.moera.node.data.Subscriber;
import org.moera.node.data.Subscription;
import org.moera.node.data.SubscriptionRepository;
import org.moera.node.data.SubscriptionStatus;
import org.moera.node.global.UniversalContext;
import org.moera.node.media.MediaManager;
import org.moera.node.model.SubscriberDescriptionUtil;
import org.moera.node.option.Options;
import org.moera.node.task.Jobs;
import org.moera.node.util.Transaction;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SubscriptionTaskTest {

    @ParameterizedTest
    @CsvSource({
        "public, public",
        "signed, signed",
        "admin, private",
        "subscribed, private",
        "f:00000000-0000-0000-0000-000000000001, private",
        "private, private",
        "none, private"
    })
    void subscriptionVisibilityIsTransferredToRemoteSubscriber(String ourView, String theirView) throws Exception {
        UUID subscriptionId = UUID.randomUUID();
        Subscription subscription = new Subscription();
        subscription.setId(subscriptionId);
        subscription.setSubscriptionType(SubscriptionType.FEED);
        subscription.setRemoteNodeName("remote");
        subscription.setRemoteFeedName(Feed.TIMELINE);
        subscription.setStatus(SubscriptionStatus.PENDING);
        ReflectionTestUtils.setField(subscription, "usageCount", 1);
        SubscriptionRepository subscriptionRepository = mock(SubscriptionRepository.class);
        when(subscriptionRepository.findById(subscriptionId)).thenReturn(Optional.of(subscription));
        UniversalContext universalContext = mock(UniversalContext.class);
        Options options = mock(Options.class);
        when(universalContext.getOptions()).thenReturn(options);
        when(options.getPrincipal("subscriptions.view")).thenReturn(new Principal(ourView));
        MoeraNode remote = mock(MoeraNode.class);
        when(remote.createSubscriber(any())).thenReturn(new SubscriberInfo());
        NodeApi nodeApi = mock(NodeApi.class);
        when(nodeApi.at("remote", "carte")).thenReturn(remote);
        SubscriptionTask task = new SubscriptionTask(subscriptionId) {
            @Override
            protected String generateCarte(String targetNodeName, Scope clientScope) {
                return "carte";
            }
        };
        task.setNodeId(UUID.randomUUID());
        ReflectionTestUtils.setField(task, "subscriptionRepository", subscriptionRepository);
        ReflectionTestUtils.setField(task, "universalContext", universalContext);
        ReflectionTestUtils.setField(task, "nodeApi", nodeApi);
        ReflectionTestUtils.setField(task, "tx", new Transaction());
        ReflectionTestUtils.setField(task, "subscriptionManager", mock(SubscriptionManager.class));
        ReflectionTestUtils.setField(task, "mediaManager", mock(MediaManager.class));
        ReflectionTestUtils.setField(task, "jobs", mock(Jobs.class));

        task.execute();

        ArgumentCaptor<SubscriberDescription> captor = ArgumentCaptor.forClass(SubscriberDescription.class);
        verify(remote).createSubscriber(captor.capture());
        Subscriber subscriber = new Subscriber();
        SubscriberDescriptionUtil.toSubscriber(captor.getValue(), subscriber);
        assertEquals(new Principal(theirView), subscriber.getViewPrincipal());
    }

}
