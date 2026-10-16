package org.moera.node.operations;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.moera.lib.node.types.SubscriptionType;
import org.moera.node.data.Contact;
import org.moera.node.data.Feed;
import org.moera.node.data.Posting;
import org.moera.node.data.PostingRepository;
import org.moera.node.data.StoryRepository;
import org.moera.node.data.UserSubscription;
import org.moera.node.data.UserSubscriptionRepository;
import org.moera.node.global.UniversalContext;
import org.moera.node.liberin.model.PostingRecommendationUpdatedLiberin;
import org.moera.node.option.Options;
import org.moera.node.task.Jobs;
import org.moera.node.util.Transaction;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.moera.node.util.MediaTransactionTestSupport.transaction;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SubscriptionOperationsTest {

    @Test
    void subscriptionClearsRecommendationsInDestinationAndExploreWithUpdates() throws Exception {
        UUID nodeId = UUID.randomUUID();
        UniversalContext context = mock(UniversalContext.class);
        when(context.nodeId()).thenReturn(nodeId);
        Options options = mock(Options.class);
        when(context.getOptions()).thenReturn(options);
        when(options.getString("subscription.auto.node")).thenReturn("auto");
        UserSubscriptionRepository subscriptions = mock(UserSubscriptionRepository.class);
        when(subscriptions.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        PostingRepository postings = mock(PostingRepository.class);
        Posting news = posting();
        Posting explore = posting();
        when(postings.findByOwnerNameAndFeed(nodeId, "remote", Feed.NEWS)).thenReturn(List.of(news));
        when(postings.findByOwnerNameAndFeed(nodeId, "remote", Feed.EXPLORE)).thenReturn(List.of(explore));
        ContactOperations contacts = mock(ContactOperations.class);
        when(contacts.updateViewPrincipal(any(UserSubscription.class))).thenReturn(new Contact());
        PostingOperations postingOperations = new PostingOperations();
        ReflectionTestUtils.setField(postingOperations, "universalContext", context);
        SubscriptionOperations operations = new SubscriptionOperations();
        Transaction tx = transaction();
        ReflectionTestUtils.setField(operations, "tx", tx);
        ReflectionTestUtils.setField(operations, "universalContext", context);
        ReflectionTestUtils.setField(operations, "userSubscriptionRepository", subscriptions);
        ReflectionTestUtils.setField(operations, "postingRepository", postings);
        ReflectionTestUtils.setField(operations, "postingOperations", postingOperations);
        ReflectionTestUtils.setField(operations, "storyRepository", mock(StoryRepository.class));
        ReflectionTestUtils.setField(operations, "contactOperations", contacts);
        ReflectionTestUtils.setField(operations, "jobs", mock(Jobs.class));

        operations.subscribe(subscription -> {
            subscription.setSubscriptionType(SubscriptionType.FEED);
            subscription.setRemoteNodeName("remote");
            subscription.setRemoteFeedName(Feed.TIMELINE);
            subscription.setFeedName(Feed.NEWS);
        });

        assertFalse(news.isRecommended());
        assertFalse(explore.isRecommended());
        ArgumentCaptor<PostingRecommendationUpdatedLiberin> captor =
            ArgumentCaptor.forClass(PostingRecommendationUpdatedLiberin.class);
        verify(context, times(2)).send(captor.capture());
        assertEquals(List.of(news.getId(), explore.getId()), captor.getAllValues().stream()
            .map(PostingRecommendationUpdatedLiberin::getPostingId).toList());
    }

    private Posting posting() {
        Posting posting = new Posting();
        posting.setId(UUID.randomUUID());
        posting.setRecommended(true);
        return posting;
    }

}
