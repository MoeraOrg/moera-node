package org.moera.node.rest.notification;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.moera.lib.node.types.StoryType;
import org.moera.lib.node.types.SubscriptionType;
import org.moera.lib.node.types.notifications.PostingUpdatedNotification;
import org.moera.lib.node.types.notifications.StoryAddedNotification;
import org.moera.node.data.Pick;
import org.moera.node.data.Posting;
import org.moera.node.data.PostingRepository;
import org.moera.node.data.Story;
import org.moera.node.data.Subscription;
import org.moera.node.data.SubscriptionRepository;
import org.moera.node.data.UserSubscription;
import org.moera.node.data.UserSubscriptionRepository;
import org.moera.node.global.UniversalContext;
import org.moera.node.model.UnsubscribeFailure;
import org.moera.node.model.notification.StoryAddedNotificationUtil;
import org.moera.node.picker.PickerPool;
import org.moera.node.util.Transaction;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.moera.node.util.MediaTransactionTestSupport.transaction;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostingProcessorTest {

    private final UUID nodeId = UUID.randomUUID();

    private final UUID repostId = UUID.randomUUID();

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
    @NullSource
    @ValueSource(strings = "original-posting")
    void addedIgnoresRepostsOfLocalPostings(String originalPostingId) {
        Subscription subscription = subscription(SubscriptionType.FEED);
        subscription.setRemoteFeedName("timeline");
        when(universalContext.nodeName()).thenReturn("local");

        postingProcessor.added(storyNotification("local", originalPostingId));

        verifyNoInteractions(userSubscriptionRepository, pickerPool);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void addedPicksOriginalAfterCommitAndPreservesRepostSource(boolean commit) throws Exception {
        Subscription subscription = subscription(SubscriptionType.FEED);
        subscription.setRemoteFeedName("timeline");
        when(userSubscriptionRepository.findAllByTypeAndNodeAndFeedName(
            nodeId, SubscriptionType.FEED, "remote", "timeline"
        )).thenReturn(List.of(userSubscription("news"), userSubscription("explore")));
        StoryAddedNotification notification = storyNotification("original", "original-posting");

        assertPicksOnlyAfterCommit(commit, 2, () -> postingProcessor.added(notification));

        if (commit) {
            var captor = ArgumentCaptor.forClass(Pick.class);
            verify(pickerPool, times(2)).pick(captor.capture());
            List<Pick> picks = captor.getAllValues();
            assertEquals(List.of("news", "explore"), picks.stream().map(Pick::getFeedName).toList());
            for (Pick pick : picks) {
                assertEquals("original", pick.getRemoteNodeName());
                assertEquals("original-posting", pick.getRemotePostingId());
                assertEquals("remote", pick.getSourceNodeName());
                assertEquals(repostId.toString(), pick.getSourcePostingId());
                assertEquals("timeline", pick.getRemoteFeedName());
            }
        }
    }

    @ParameterizedTest
    @CsvSource({",", "original,", ", original-posting", "'', original-posting", "original, ''"})
    void addedWithoutCompleteOriginalAddressPicksFromSender(String remoteNodeName, String remotePostingId) {
        Subscription subscription = subscription(SubscriptionType.FEED);
        subscription.setRemoteFeedName("timeline");
        when(userSubscriptionRepository.findAllByTypeAndNodeAndFeedName(
            nodeId, SubscriptionType.FEED, "remote", "timeline"
        )).thenReturn(List.of(userSubscription("news")));

        postingProcessor.added(storyNotification(remoteNodeName, remotePostingId));

        var captor = ArgumentCaptor.forClass(Pick.class);
        verify(pickerPool).pickAfterCommit(captor.capture());
        Pick pick = captor.getValue();
        assertEquals("remote", pick.getRemoteNodeName());
        assertEquals(repostId.toString(), pick.getRemotePostingId());
        assertEquals("timeline", pick.getRemoteFeedName());
        assertEquals("news", pick.getFeedName());
        assertNull(pick.getSourceNodeName());
        assertNull(pick.getSourcePostingId());
    }

    @Test
    void addedStillRejectsUnknownSubscriptionForLocalRepost() {
        when(universalContext.nodeId()).thenReturn(nodeId);
        lenient().when(universalContext.nodeName()).thenReturn("local");
        StoryAddedNotification notification = storyNotification("local", "original-posting");

        assertThrows(UnsubscribeFailure.class, () -> postingProcessor.added(notification));

        verifyNoInteractions(userSubscriptionRepository, pickerPool);
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

    private StoryAddedNotification storyNotification(String remoteNodeName, String remotePostingId) {
        Posting posting = new Posting();
        posting.setId(repostId);
        Story story = new Story(UUID.randomUUID(), UUID.randomUUID(), StoryType.POSTING_ADDED);
        story.setFeedName("timeline");
        story.setEntry(posting);
        story.setRemoteNodeName(remoteNodeName);
        story.setRemotePostingId(remotePostingId);
        ObjectMapper mapper = new ObjectMapper();
        StoryAddedNotification notification = mapper.readValue(
            mapper.writeValueAsString(StoryAddedNotificationUtil.build(story)), StoryAddedNotification.class
        );
        notification.setSenderNodeName("remote");
        notification.setSubscriberId("subscriber");
        return notification;
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
