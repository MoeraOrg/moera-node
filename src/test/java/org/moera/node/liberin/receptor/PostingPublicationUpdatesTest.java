package org.moera.node.liberin.receptor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.moera.lib.node.types.SubscriptionType;
import org.moera.lib.node.types.notifications.Notification;
import org.moera.lib.node.types.notifications.PostingUpdatedNotification;
import org.moera.lib.node.types.principal.Principal;
import org.moera.node.data.EntryRevision;
import org.moera.node.data.Posting;
import org.moera.node.data.PostingRepository;
import org.moera.node.liberin.Liberin;
import org.moera.node.liberin.model.PostingPublishedAtUpdatedLiberin;
import org.moera.node.model.event.Event;
import org.moera.node.model.event.PostingUpdatedEvent;
import org.moera.node.notification.send.Direction;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PostingPublicationUpdatesTest {

    @Test
    void publicationChangeNotifiesPostingSubscribersAndClientsWithCurrentVisibility() {
        Posting posting = new Posting();
        posting.setNodeId(UUID.randomUUID());
        posting.setId(UUID.randomUUID());
        posting.setOwnerName("alice");
        posting.setViewPrincipal(Principal.SUBSCRIBED);
        EntryRevision revision = new EntryRevision();
        revision.setUpdateImportant(true);
        posting.setCurrentRevision(revision);
        PostingPublishedAtUpdatedLiberin liberin = new PostingPublishedAtUpdatedLiberin(posting.getId());
        liberin.setNodeId(posting.getNodeId());
        TestReceptor receptor = receptor(liberin, Optional.of(posting));

        receptor.publishedAtUpdated(liberin);

        assertEquals(1, receptor.notifications.size());
        PostingUpdatedNotification notification =
            assertInstanceOf(PostingUpdatedNotification.class, receptor.notifications.getFirst());
        assertEquals(posting.getId().toString(), notification.getPostingId());
        Direction direction = receptor.directions.getFirst();
        assertEquals(posting.getNodeId(), direction.getNodeId());
        assertEquals(SubscriptionType.POSTING, ReflectionTestUtils.getField(direction, "subscriptionType"));
        assertEquals(posting.getId(), ReflectionTestUtils.getField(direction, "postingId"));
        assertTrue(direction.isPermitted(false, "reader", true, new String[0]));
        assertFalse(direction.isPermitted(false, "reader", false, new String[0]));
        assertEquals(1, receptor.events.size());
        PostingUpdatedEvent event = assertInstanceOf(PostingUpdatedEvent.class, receptor.events.getFirst());
        assertEquals(posting.getId().toString(), event.getId());
        assertTrue(event.getFilter().includes(false, "reader", true, new String[0]));
        assertFalse(event.getFilter().includes(false, "reader", false, new String[0]));
    }

    @Test
    void postingDeletedBeforeDeliveryDoesNotEmitUpdates() {
        PostingPublishedAtUpdatedLiberin liberin = new PostingPublishedAtUpdatedLiberin(UUID.randomUUID());
        liberin.setNodeId(UUID.randomUUID());
        TestReceptor receptor = receptor(liberin, Optional.empty());

        receptor.publishedAtUpdated(liberin);

        assertTrue(receptor.notifications.isEmpty());
        assertTrue(receptor.events.isEmpty());
    }

    private static TestReceptor receptor(PostingPublishedAtUpdatedLiberin liberin, Optional<Posting> posting) {
        PostingRepository repository = mock(PostingRepository.class);
        when(repository.findByNodeIdAndId(liberin.getNodeId(), liberin.getPostingId())).thenReturn(posting);
        TestReceptor receptor = new TestReceptor();
        ReflectionTestUtils.setField(receptor, "postingRepository", repository);
        return receptor;
    }

    private static class TestReceptor extends PostingReceptor {

        private final List<Notification> notifications = new ArrayList<>();
        private final List<Direction> directions = new ArrayList<>();
        private final List<Event> events = new ArrayList<>();

        @Override
        protected void send(Direction direction, Notification notification) {
            notifications.add(notification);
            directions.add(direction);
        }

        @Override
        protected void send(Liberin liberin, Event event) {
            events.add(event);
        }

    }

}
