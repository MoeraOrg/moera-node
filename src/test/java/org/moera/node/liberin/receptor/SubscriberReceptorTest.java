package org.moera.node.liberin.receptor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.moera.lib.node.types.SubscriptionType;
import org.moera.lib.node.types.notifications.Notification;
import org.moera.lib.node.types.notifications.PostingSharingsUpdatedNotification;
import org.moera.lib.node.types.notifications.PostingUpdatedNotification;
import org.moera.lib.node.types.principal.Principal;
import org.moera.node.data.Posting;
import org.moera.node.data.PostingRepository;
import org.moera.node.data.Subscriber;
import org.moera.node.instant.SubscriberInstants;
import org.moera.node.liberin.model.SubscriberAddedLiberin;
import org.moera.node.notification.send.Direction;
import org.moera.node.notification.send.Directions;
import org.moera.node.util.Util;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

class SubscriberReceptorTest {

    private final UUID nodeId = UUID.randomUUID();
    private final Posting posting = new Posting();
    private final Subscriber subscriber = new Subscriber();

    @Mock
    private PostingRepository postingRepository;

    @Mock
    private SubscriberInstants subscriberInstants;

    @InjectMocks
    private final TestReceptor receptor = new TestReceptor();

    private AutoCloseable mocks;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        posting.setId(UUID.randomUUID());
        posting.setNodeId(nodeId);
        posting.setOwnerName("author");
        posting.setEditedAt(Util.toTimestamp(1000L));
        posting.setTotalSharings(3);
        subscriber.setId(UUID.randomUUID());
        subscriber.setNodeId(nodeId);
        subscriber.setSubscriptionType(SubscriptionType.POSTING);
        subscriber.setRemoteNodeName("reader");
        subscriber.setEntry(posting);
        when(postingRepository.findByNodeIdAndIdForUpdate(nodeId, posting.getId()))
            .thenReturn(Optional.of(posting));
    }

    @AfterEach
    void tearDown() throws Exception {
        mocks.close();
    }

    @ParameterizedTest
    @CsvSource({"1000, false", "999, true"})
    void postingSubscriptionReceivesSharingTotalRegardlessOfContentChange(long lastUpdatedAt, boolean contentChanged) {
        add(lastUpdatedAt);

        SentNotification sent = receptor.sharingUpdate();
        PostingSharingsUpdatedNotification notification = (PostingSharingsUpdatedNotification) sent.notification();
        assertEquals(posting.getId().toString(), notification.getPostingId());
        assertEquals(subscriber.getId().toString(), notification.getSubscriberId());
        assertEquals(3, notification.getTotal());
        assertNotNull(notification.getSubscriptionCreatedAt());
        assertEquals(Directions.single(nodeId, "reader"), sent.direction());
        assertEquals(
            contentChanged,
            receptor.notifications.stream()
                .map(SentNotification::notification)
                .anyMatch(PostingUpdatedNotification.class::isInstance)
        );
        assertEquals(contentChanged ? 2 : 1, receptor.notifications.size());
    }

    @Test
    void initialSharingTotalUsesCurrentPostingInsteadOfSubscriptionSnapshot() {
        Posting snapshot = new Posting();
        snapshot.setId(posting.getId());
        snapshot.setEditedAt(posting.getEditedAt());
        snapshot.setTotalSharings(0);
        subscriber.setEntry(snapshot);

        add(1000L);

        PostingSharingsUpdatedNotification notification =
            (PostingSharingsUpdatedNotification) receptor.sharingUpdate().notification();
        assertEquals(3, notification.getTotal());
    }

    @ParameterizedTest
    @CsvSource({
        "public, public, false, true",
        "private, public, false, false",
        "public, private, false, false",
        "public, subscribed, false, false",
        "public, subscribed, true, true"
    })
    void initialSharingTotalRespectsPostingAndCounterVisibility(
        String view, String viewTotal, boolean subscribed, boolean permitted
    ) {
        posting.setViewPrincipal(new Principal(view));
        posting.setViewTotalSharingsPrincipal(new Principal(viewTotal));

        add(1000L);

        assertEquals(
            permitted,
            receptor.sharingUpdate().direction().isPermitted(false, "reader", subscribed, new String[0])
        );
    }

    @ParameterizedTest
    @CsvSource({"public, private, false", "private, public, true"})
    void initialSharingTotalForPickedPostingUsesOriginalCounterVisibility(
        String localViewTotal, String originalViewTotal, boolean permitted
    ) {
        posting.setReceiverName("original");
        posting.setViewTotalSharingsPrincipal(new Principal(localViewTotal));
        posting.setReceiverViewTotalSharingsPrincipal(new Principal(originalViewTotal));

        add(1000L);

        assertEquals(
            permitted,
            receptor.sharingUpdate().direction().isPermitted(false, "reader", false, new String[0])
        );
    }

    @ParameterizedTest
    @EnumSource(value = SubscriptionType.class, names = {"POSTING_COMMENTS", "PROFILE", "SEARCH", "USER_LIST"})
    void otherSubscriptionTypesDoNotReceivePostingSharingTotal(SubscriptionType type) {
        subscriber.setSubscriptionType(type);

        add(1000L);

        assertTrue(receptor.notifications.isEmpty());
    }

    @Test
    void missingPostingDoesNotSendInitialState() {
        when(postingRepository.findByNodeIdAndIdForUpdate(nodeId, posting.getId()))
            .thenReturn(Optional.empty());

        add(999L);

        assertTrue(receptor.notifications.isEmpty());
    }

    private void add(Long lastUpdatedAt) {
        SubscriberAddedLiberin liberin = new SubscriberAddedLiberin(subscriber, lastUpdatedAt);
        liberin.setNodeId(nodeId);
        receptor.added(liberin);
    }

    private record SentNotification(Direction direction, Notification notification) {
    }

    private static class TestReceptor extends SubscriberReceptor {

        private final List<SentNotification> notifications = new ArrayList<>();

        @Override
        protected void send(Direction direction, Notification notification) {
            notifications.add(new SentNotification(direction, notification));
        }

        private SentNotification sharingUpdate() {
            var sharings = notifications.stream()
                .filter(sent -> sent.notification() instanceof PostingSharingsUpdatedNotification)
                .toList();
            assertEquals(1, sharings.size());
            return sharings.getFirst();
        }

    }

}
