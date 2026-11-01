package org.moera.node.rest.notification;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.moera.lib.node.types.StoryType;
import org.moera.lib.node.types.SubscriptionType;
import org.moera.lib.node.types.notifications.PostingUpdatedNotification;
import org.moera.lib.node.types.notifications.StoryAddedNotification;
import org.moera.node.data.Pick;
import org.moera.node.data.Posting;
import org.moera.node.data.PostingRepository;
import org.moera.node.data.Subscription;
import org.moera.node.data.SubscriptionRepository;
import org.moera.node.data.UserSubscription;
import org.moera.node.data.UserSubscriptionRepository;
import org.moera.node.global.UniversalContext;
import org.moera.node.picker.PickerPool;
import org.moera.node.util.Transaction;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.moera.node.util.MediaTransactionTestSupport.transaction;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostingProcessorTest {

    private final UUID nodeId = UUID.randomUUID();

    @Mock
    private UniversalContext universalContext;

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private UserSubscriptionRepository userSubscriptionRepository;

    @Mock
    private PostingRepository postingRepository;

    @Mock
    private PickerPool pickerPool;

    @InjectMocks
    private PostingProcessor postingProcessor;

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void addedStartsPicksOnlyAfterCommit(boolean commit) throws Exception {
        Subscription subscription = subscription(SubscriptionType.FEED);
        subscription.setRemoteFeedName("timeline");
        when(userSubscriptionRepository.findAllByTypeAndNodeAndFeedName(
            nodeId, SubscriptionType.FEED, "remote", "timeline"
        )).thenReturn(List.of(userSubscription("news"), userSubscription("explore")));
        StoryAddedNotification notification = new StoryAddedNotification();
        notification.setSenderNodeName("remote");
        notification.setSubscriberId("subscriber");
        notification.setFeedName("timeline");
        notification.setStoryType(StoryType.POSTING_ADDED);
        notification.setPostingId("posting");

        assertPicksOnlyAfterCommit(commit, 2, () -> postingProcessor.added(notification));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void updatedStartsPickOnlyAfterCommit(boolean commit) throws Exception {
        Subscription subscription = subscription(SubscriptionType.POSTING);
        subscription.setRemoteEntryId("posting");
        when(postingRepository.findByReceiverId(nodeId, "remote", "posting"))
            .thenReturn(Optional.of(new Posting()));
        PostingUpdatedNotification notification = new PostingUpdatedNotification();
        notification.setSenderNodeName("remote");
        notification.setSubscriberId("subscriber");
        notification.setPostingId("posting");

        assertPicksOnlyAfterCommit(commit, 1, () -> postingProcessor.updated(notification));
    }

    private Subscription subscription(SubscriptionType type) {
        when(universalContext.nodeId()).thenReturn(nodeId);
        Subscription subscription = new Subscription();
        subscription.setSubscriptionType(type);
        subscription.setRemoteNodeName("remote");
        when(subscriptionRepository.findBySubscriber(nodeId, "remote", "subscriber"))
            .thenReturn(Optional.of(subscription));
        return subscription;
    }

    private static UserSubscription userSubscription(String feedName) {
        UserSubscription subscription = new UserSubscription();
        subscription.setRemoteNodeName("remote");
        subscription.setRemoteFeedName("timeline");
        subscription.setFeedName(feedName);
        return subscription;
    }

    private void assertPicksOnlyAfterCommit(boolean commit, int count, Runnable action) throws Exception {
        doCallRealMethod().when(pickerPool).pickAfterCommit(any(Pick.class));
        Transaction tx = transaction();
        Runnable inside = () -> {
            action.run();
            verify(pickerPool, never()).pick(any(Pick.class));
            if (!commit) {
                throw new IllegalStateException("rollback");
            }
        };
        if (commit) {
            tx.executeWrite(inside);
        } else {
            assertThrows(IllegalStateException.class, () -> tx.executeWrite(inside));
        }
        verify(pickerPool, times(commit ? count : 0)).pick(any(Pick.class));
    }

}
