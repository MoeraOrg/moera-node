package org.moera.node.liberin.receptor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.moera.lib.node.types.PushContent;
import org.moera.lib.node.types.StoryAttributes;
import org.moera.lib.node.types.StoryType;
import org.moera.lib.node.types.notifications.Notification;
import org.moera.lib.node.types.notifications.NotificationType;
import org.moera.lib.node.types.notifications.PostingSharedNotification;
import org.moera.lib.node.types.notifications.PostingSharingUpdatedNotification;
import org.moera.lib.node.types.notifications.PostingUnsharedNotification;
import org.moera.lib.node.types.principal.Principal;
import org.moera.node.data.Feed;
import org.moera.node.data.Posting;
import org.moera.node.data.Story;
import org.moera.node.data.StoryRepository;
import org.moera.node.global.UniversalContext;
import org.moera.node.liberin.Liberin;
import org.moera.node.liberin.model.StoryAddedLiberin;
import org.moera.node.liberin.model.StoryDeletedLiberin;
import org.moera.node.liberin.model.StoryUpdatedLiberin;
import org.moera.node.model.event.Event;
import org.moera.node.notification.send.Direction;
import org.moera.node.notification.send.Directions;
import org.moera.node.operations.FavorOperations;
import org.moera.node.operations.StoryOperations;
import org.moera.node.option.Options;
import org.moera.node.util.ExtendedDuration;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StorySharingNotificationsTest {

    @ParameterizedTest
    @MethodSource("entryVisibility")
    void sharingNotificationsRespectEntryVisibility(Principal view, boolean subscribed, boolean permitted) {
        TestReceptor receptor = receptor();
        Story story = story(Feed.TIMELINE, true);
        story.getEntry().setViewPrincipal(view);

        receptor.added(new StoryAddedLiberin(story));
        StoryDeletedLiberin deletion = new StoryDeletedLiberin(story);
        story.getEntry().setViewPrincipal(Principal.PUBLIC);
        receptor.deleted(deletion);

        assertEquals(2, receptor.deliveries.size());
        for (Delivery delivery : receptor.deliveries) {
            assertEquals(permitted, delivery.direction().isPermitted(false, "owner", subscribed, new String[0]));
        }
    }

    private static Stream<Arguments> entryVisibility() {
        return Stream.of(
            Arguments.of(Principal.PUBLIC, false, true),
            Arguments.of(Principal.PRIVATE, false, true),
            Arguments.of(Principal.ADMIN, false, false),
            Arguments.of(Principal.OWNER, false, true),
            Arguments.of(Principal.ofNode("other"), false, false),
            Arguments.of(Principal.SUBSCRIBED, false, false),
            Arguments.of(Principal.SUBSCRIBED, true, true)
        );
    }

    @Test
    void timelineRepublishOmitsRevisionAndKeepsStoryIdWhileSendingTheCurrentMoment() {
        TestReceptor receptor = receptor();
        Story story = story(Feed.TIMELINE, true);
        story.getEntry().setReceiverName("original-node");
        receptor.added(new StoryAddedLiberin(story));
        var shared = assertInstanceOf(PostingSharedNotification.class, receptor.deliveries.getFirst().notification());
        story.getEntry().setCurrentReceiverRevisionId("updated-revision");
        story.setMoment(200L);
        StoryDeletedLiberin deletion = new StoryDeletedLiberin(story);
        receptor.deleted(deletion);
        var unshared = assertInstanceOf(
            PostingUnsharedNotification.class, receptor.deliveries.getLast().notification()
        );

        assertEquals("original", shared.getPostingId());
        assertNull(shared.getPostingRevisionId());
        assertEquals(100L, shared.getRemoteStoryMoment());
        assertEquals(shared.getPostingId(), unshared.getPostingId());
        assertNull(unshared.getPostingRevisionId());
        assertEquals(shared.getRemoteFeedName(), unshared.getRemoteFeedName());
        assertEquals(shared.getRemoteStoryId(), unshared.getRemoteStoryId());
        assertEquals(200L, unshared.getRemoteStoryMoment());
        assertEquals(shared.getRemotePostingId(), unshared.getRemotePostingId());
        var original = Directions.single(deletion.getNodeId(), "original-node");
        assertEquals(List.of(original, original), receptor.deliveries.stream().map(Delivery::direction).toList());
    }

    @ParameterizedTest
    @CsvSource({
        "public, unset,      ,        false, POSTING_SHARING_UPDATED",
        "admin,  unset,      ,        false, ",
        "private,unset,      ,        false, ",
        "admin,  public,     ,        false, POSTING_SHARING_UPDATED",
        "public, private,    ,        false, ",
        "public, subscribed, ,        false, ",
        "public, subscribed, ,        true,  POSTING_SHARING_UPDATED",
        "public, public,     signed,  false, POSTING_SHARING_UPDATED",
        "public, private,    public,  false, POSTING_SHARED",
        "public, public,     private, false, POSTING_UNSHARED",
        "public, private,    admin,   false, "
    })
    void movingRepublishNotifiesOriginalNodeAccordingToStoryVisibility(
        String entryView, String previousView, String newView, boolean subscribed, NotificationType expected
    ) {
        TestReceptor receptor = receptor();
        Story story = story(Feed.TIMELINE, true);
        story.getEntry().setReceiverName("original-node");
        story.getEntry().setViewPrincipal(new Principal(entryView));
        story.setViewPrincipal(new Principal(previousView));
        StoryAttributes attributes = new StoryAttributes();
        attributes.setPinned(true);
        if (newView != null) {
            var permissions = new org.moera.lib.node.types.StoryOperations();
            permissions.setView(new Principal(newView));
            attributes.setOperations(permissions);
        }

        update(receptor, story, attributes);

        assertNotEquals(100L, story.getMoment());
        List<Notification> received = new ArrayList<>();
        for (Delivery delivery : receptor.deliveries) {
            assertEquals(Directions.single(story.getNodeId(), "original-node"), delivery.direction());
            if (delivery.direction().isPermitted(false, "original-node", subscribed, new String[0])) {
                received.add(delivery.notification());
            }
        }
        assertEquals(
            expected != null ? List.of(expected) : List.of(), received.stream().map(Notification::getType).toList()
        );
        if (expected == NotificationType.POSTING_SHARING_UPDATED) {
            var update = assertInstanceOf(PostingSharingUpdatedNotification.class, received.getFirst());
            assertEquals("original", update.getPostingId());
            assertNull(update.getPostingRevisionId());
            assertEquals(Feed.TIMELINE, update.getRemoteFeedName());
            assertEquals(story.getId().toString(), update.getRemoteStoryId());
            assertEquals(story.getMoment(), update.getRemoteStoryMoment());
            assertEquals(story.getEntry().getId().toString(), update.getRemotePostingId());
        } else if (expected == NotificationType.POSTING_SHARED) {
            var shared = assertInstanceOf(PostingSharedNotification.class, received.getFirst());
            assertEquals("original", shared.getPostingId());
            assertEquals(story.getId().toString(), shared.getRemoteStoryId());
            assertEquals(story.getMoment(), shared.getRemoteStoryMoment());
            assertEquals(story.getEntry().getId().toString(), shared.getRemotePostingId());
        } else if (expected == NotificationType.POSTING_UNSHARED) {
            var unshared = assertInstanceOf(PostingUnsharedNotification.class, received.getFirst());
            assertEquals(100L, unshared.getRemoteStoryMoment());
        }
    }

    @Test
    void editingPublicationDateUpdatesSharingOnlyWhenTheMomentChanges() {
        Story story = story(Feed.TIMELINE, true);
        TestReceptor moved = receptor();
        StoryAttributes publication = new StoryAttributes();
        publication.setPublishAt(42L);
        update(moved, story, publication);
        assertNotEquals(100L, story.getMoment());
        assertEquals(1, moved.deliveries.size());
        var notification = assertInstanceOf(
            PostingSharingUpdatedNotification.class, moved.deliveries.getFirst().notification()
        );
        assertEquals(story.getMoment(), notification.getRemoteStoryMoment());

        TestReceptor read = receptor();
        StoryAttributes reading = new StoryAttributes();
        reading.setRead(true);
        update(read, story, reading);
        assertTrue(read.deliveries.isEmpty());
    }

    private static void update(TestReceptor receptor, Story story, StoryAttributes attributes) {
        StoryRepository repository = mock(StoryRepository.class);
        when(repository.findByNodeIdAndId(story.getNodeId(), story.getId())).thenReturn(Optional.of(story));
        ReflectionTestUtils.setField(receptor, "storyRepository", repository);
        UniversalContext context = mock(UniversalContext.class);
        StoryOperations operations = new StoryOperations();
        ReflectionTestUtils.setField(operations, "universalContext", context);
        ReflectionTestUtils.setField(operations, "storyRepository", repository);
        ReflectionTestUtils.setField(operations, "entityManager", mock(EntityManager.class));
        operations.update(story, attributes);

        var updated = ArgumentCaptor.forClass(StoryUpdatedLiberin.class);
        verify(context).send(updated.capture());
        updated.getValue().setNodeId(story.getNodeId());
        receptor.updated(updated.getValue());
    }

    @Test
    void newsAndOriginalPostingsDoNotSendSharingNotifications() {
        TestReceptor receptor = receptor();
        receptor.added(new StoryAddedLiberin(story(Feed.NEWS, true)));
        receptor.added(new StoryAddedLiberin(story(Feed.TIMELINE, false)));
        receptor.deleted(new StoryDeletedLiberin(story(Feed.NEWS, true)));
        StoryAttributes attributes = new StoryAttributes();
        attributes.setPinned(true);
        update(receptor, story(Feed.NEWS, true), attributes);
        update(receptor, story(Feed.TIMELINE, false), attributes);
        assertTrue(receptor.deliveries.isEmpty());
    }

    private static Story story(String feed, boolean picked) {
        Posting posting = new Posting();
        posting.setId(UUID.randomUUID());
        posting.setOwnerName("owner");
        if (picked) {
            posting.setReceiverName("owner");
            posting.setReceiverEntryId("original");
            posting.setCurrentReceiverRevisionId("original-revision");
        }
        Story story = new Story(UUID.randomUUID(), UUID.randomUUID(), StoryType.POSTING_ADDED);
        story.setEntry(posting);
        story.setFeedName(feed);
        story.setMoment(100L);
        return story;
    }

    private static TestReceptor receptor() {
        TestReceptor receptor = new TestReceptor();
        UniversalContext context = mock(UniversalContext.class);
        Options options = mock(Options.class);
        when(options.getDuration("posting.published.notification.age")).thenReturn(ExtendedDuration.NEVER);
        when(context.getOptions()).thenReturn(options);
        ReflectionTestUtils.setField(receptor, "universalContext", context);
        ReflectionTestUtils.setField(receptor, "storyOperations", mock(StoryOperations.class));
        ReflectionTestUtils.setField(receptor, "favorOperations", mock(FavorOperations.class));
        return receptor;
    }

    private static class TestReceptor extends StoryReceptor {

        private final List<Delivery> deliveries = new ArrayList<>();

        @Override
        protected void send(Direction direction, Notification notification) {
            if (
                notification instanceof PostingSharedNotification
                || notification instanceof PostingSharingUpdatedNotification
                || notification instanceof PostingUnsharedNotification
            ) {
                deliveries.add(new Delivery(direction, notification));
            }
        }

        @Override
        protected void send(Liberin liberin, Event event) {
        }

        @Override
        protected void send(Liberin liberin) {
        }

        @Override
        protected void send(PushContent content) {
        }

    }

    private record Delivery(Direction direction, Notification notification) {
    }

}
