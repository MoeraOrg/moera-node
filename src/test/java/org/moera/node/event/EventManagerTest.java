package org.moera.node.event;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.moera.lib.node.types.ContactInfo;
import org.moera.lib.node.types.ContactOperations;
import org.moera.lib.node.types.FriendInfo;
import org.moera.lib.node.types.Scope;
import org.moera.lib.node.types.StoryType;
import org.moera.lib.node.types.SubscriptionInfo;
import org.moera.lib.node.types.SubscriptionType;
import org.moera.lib.node.types.principal.Principal;
import org.moera.node.domain.Domains;
import org.moera.node.model.event.FriendshipUpdatedEvent;
import org.moera.node.model.event.PostingSharingDeletedEvent;
import org.moera.node.model.event.StoryAddedEvent;
import org.moera.node.model.event.StoryEvent;
import org.moera.node.model.event.SubscriptionAddedEvent;
import org.moera.node.model.event.SubscriptionEvent;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.support.ExecutorSubscribableChannel;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EventManagerTest {

    @Test
    void deliversPayloadFieldsWithTypesDifferentFromConstructorParameters() {
        Fixture fixture = new Fixture();
        UUID postingId = UUID.randomUUID();
        UUID sharingId = UUID.randomUUID();
        PostingSharingDeletedEvent event = new PostingSharingDeletedEvent(postingId, sharingId, Principal.PUBLIC);
        fixture.eventManager.send(fixture.nodeId, event);

        fixture.deliver(fixture.publicSubscriber);

        assertEquals(1, fixture.delivered.size());
        PostingSharingDeletedEvent deliveredEvent = (PostingSharingDeletedEvent) fixture.delivered.getFirst().getEvent();
        assertEquals(postingId.toString(), deliveredEvent.getPostingId());
        assertEquals(sharingId.toString(), deliveredEvent.getSharingId());
    }

    @Test
    void protectsNestedContactsSeparatelyForEachDeliveryAndReplay() {
        Fixture fixture = new Fixture();
        SubscriptionInfo subscription = new SubscriptionInfo();
        subscription.setType(SubscriptionType.FEED);
        subscription.setContact(contact());
        SubscriptionAddedEvent event = new SubscriptionAddedEvent(subscription, Principal.PUBLIC);
        fixture.eventManager.send(fixture.nodeId, "request-id", event);

        fixture.deliver(fixture.publicSubscriber);
        fixture.deliver(fixture.adminSubscriber);
        fixture.publicSubscriber.setLastEventSeen(0);
        fixture.deliver(fixture.publicSubscriber);

        assertEquals(3, fixture.delivered.size());
        assertFalse(subscriptionContact(fixture.delivered.get(0)).getHasFriend());
        assertTrue(subscriptionContact(fixture.delivered.get(1)).getHasFriend());
        assertFalse(subscriptionContact(fixture.delivered.get(2)).getHasFriend());
        assertTrue(subscription.getContact().getHasFriend());
        assertNotSame(event, fixture.delivered.getFirst().getEvent());
        assertNotSame(subscription, ((SubscriptionEvent) fixture.delivered.getFirst().getEvent()).getSubscription());
        assertNotSame(subscription.getContact(), subscriptionContact(fixture.delivered.getFirst()));

        EventPacket first = fixture.delivered.getFirst();
        for (EventPacket packet : fixture.delivered) {
            assertEquals(fixture.nodeId, packet.getNodeId());
            assertEquals(1, packet.getOrdinal());
            assertEquals("request-id", packet.getCid());
            assertEquals(first.getQueueStartedAt(), packet.getQueueStartedAt());
            assertEquals(first.getSentAt(), packet.getSentAt());
        }
        assertNotSame(first, fixture.delivered.get(1));
        assertEquals(1, fixture.publicSubscriber.getLastEventSeen());
        assertEquals(1, fixture.adminSubscriber.getLastEventSeen());
    }

    @Test
    void isolatesEventsWithoutDefaultConstructorsAndRetainsTheirAccessFilter() {
        Fixture fixture = new Fixture();
        FriendInfo friend = new FriendInfo();
        friend.setNodeName("friend");
        friend.setContact(contact());
        FriendshipUpdatedEvent event = new FriendshipUpdatedEvent(friend);
        fixture.eventManager.send(fixture.nodeId, event);
        EventSubscriber friendSubscriber = fixture.subscriber("friend-session", 0);
        friendSubscriber.setClientScope(Scope.VIEW_PEOPLE.getMask());
        friendSubscriber.setClientName("friend");

        fixture.deliver(fixture.publicSubscriber);
        assertTrue(fixture.delivered.isEmpty());
        assertEquals(1, fixture.publicSubscriber.getLastEventSeen());
        fixture.deliver(friendSubscriber);
        fixture.deliver(fixture.adminSubscriber);

        assertEquals(2, fixture.delivered.size());
        FriendshipUpdatedEvent friendEvent = (FriendshipUpdatedEvent) fixture.delivered.get(0).getEvent();
        FriendshipUpdatedEvent adminEvent = (FriendshipUpdatedEvent) fixture.delivered.get(1).getEvent();
        assertFalse(friendEvent.getFriend().getContact().getHasFriend());
        assertTrue(adminEvent.getFriend().getContact().getHasFriend());
        assertTrue(friend.getContact().getHasFriend());
        assertFalse(friendEvent.isPermitted(fixture.publicSubscriber));
        assertTrue(friendEvent.isPermitted(friendSubscriber));
        assertTrue(friendEvent.isPermitted(fixture.adminSubscriber));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void keepsStorySourcesIsolatedRegardlessOfDeliveryOrder(boolean adminFirst) {
        Fixture fixture = new Fixture();
        StoryAddedEvent event = new StoryAddedEvent();
        event.setStoryType(StoryType.POSTING_ADDED);
        event.setExternalSourceUri(List.of("https://source.example/story"));
        fixture.eventManager.send(fixture.nodeId, event);

        fixture.deliver(adminFirst ? fixture.adminSubscriber : fixture.publicSubscriber);
        fixture.deliver(adminFirst ? fixture.publicSubscriber : fixture.adminSubscriber);

        StoryEvent adminEvent = (StoryEvent) fixture.delivered.get(adminFirst ? 0 : 1).getEvent();
        StoryEvent publicEvent = (StoryEvent) fixture.delivered.get(adminFirst ? 1 : 0).getEvent();
        assertEquals(List.of("https://source.example/story"), adminEvent.getExternalSourceUri());
        assertNull(publicEvent.getExternalSourceUri());
        assertEquals(List.of("https://source.example/story"), event.getExternalSourceUri());
    }

    @Test
    void doesNotDeliverEventsToAnotherNode() {
        Fixture fixture = new Fixture();
        StoryAddedEvent event = new StoryAddedEvent();
        event.setStoryType(StoryType.POSTING_ADDED);
        fixture.eventManager.send(UUID.randomUUID(), event);

        fixture.deliver(fixture.adminSubscriber);

        assertTrue(fixture.delivered.isEmpty());
        assertEquals(1, fixture.adminSubscriber.getLastEventSeen());
    }

    private static ContactInfo subscriptionContact(EventPacket packet) {
        return ((SubscriptionEvent) packet.getEvent()).getSubscription().getContact();
    }

    private static ContactInfo contact() {
        ContactInfo contact = new ContactInfo();
        contact.setNodeName("contact");
        contact.setHasFeedSubscriber(true);
        contact.setHasFeedSubscription(true);
        contact.setHasFriend(true);
        contact.setHasFriendOf(true);
        contact.setHasBlock(true);
        contact.setHasBlockBy(true);
        ContactOperations operations = new ContactOperations();
        operations.setViewFriend(Principal.ADMIN);
        contact.setOperations(operations);
        contact.setAdminOperations(new ContactOperations());
        return contact;
    }

    private static class Fixture {

        final UUID nodeId = UUID.randomUUID();
        final EventManager eventManager = new EventManager();
        final List<EventPacket> delivered = new ArrayList<>();
        final EventSubscriber publicSubscriber = subscriber("public-session", 0);
        final EventSubscriber adminSubscriber = subscriber("admin-session", Scope.ALL.getMask());

        Fixture() {
            ExecutorSubscribableChannel channel = new ExecutorSubscribableChannel();
            channel.subscribe(message -> delivered.add((EventPacket) message.getPayload()));
            ReflectionTestUtils.setField(eventManager, "messagingTemplate", new SimpMessagingTemplate(channel));
            ReflectionTestUtils.setField(eventManager, "domains", new Domains());
        }

        EventSubscriber subscriber(String sessionId, long adminScope) {
            EventSubscriber subscriber = new EventSubscriber();
            subscriber.setNodeId(nodeId);
            subscriber.setSessionId(sessionId);
            subscriber.setAdminScope(adminScope);
            subscriber.setFriendGroups(new String[0]);
            return subscriber;
        }

        void deliver(EventSubscriber subscriber) {
            ReflectionTestUtils.invokeMethod(eventManager, "deliver", subscriber);
        }

    }

}
