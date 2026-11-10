package org.moera.node.liberin.receptor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.moera.lib.node.types.PushContent;
import org.moera.lib.node.types.PushContentType;
import org.moera.lib.node.types.StoryType;
import org.moera.lib.node.types.StoryAttributes;
import org.moera.lib.node.types.principal.Principal;
import org.moera.lib.node.types.notifications.Notification;
import org.moera.lib.node.types.notifications.StoryAddedNotification;
import org.moera.lib.node.types.notifications.SearchContentUpdatedNotification;
import org.moera.lib.node.types.SearchContentUpdateType;
import org.moera.node.global.UniversalContext;
import org.moera.node.data.EntrySharing;
import org.moera.node.data.Feed;
import org.moera.node.data.Posting;
import org.moera.node.data.Story;
import org.moera.node.data.StoryRepository;
import org.moera.node.liberin.Liberin;
import org.moera.node.liberin.model.StoryUpdatedLiberin;
import org.moera.node.model.event.Event;
import org.moera.node.model.event.EventType;
import org.moera.node.model.event.StoryUpdatedEvent;
import org.moera.node.operations.StoryOperations;
import org.moera.node.notification.send.Direction;
import org.moera.node.option.Options;
import org.moera.node.util.ExtendedDuration;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class StoryReceptorTest {

    private final StoryRepository storyRepository = mock(StoryRepository.class);

    @ParameterizedTest
    @CsvSource({
        "public, admin, , STORY_DELETED",
        "admin, public, , STORY_ADDED",
        "public, signed, reader, STORY_UPDATED",
        "public, signed, , STORY_DELETED",
        "signed, public, reader, STORY_UPDATED",
        "signed, public, , STORY_ADDED",
        "unset, admin, , STORY_DELETED"
    })
    void changingVisibilityNotifiesLostNewAndRetainedReaders(
        String previous, String current, String reader, EventType expected
    ) {
        TestReceptor receptor = receptor();
        Story story = story();
        story.setStoryType(StoryType.POSTING_ADDED);
        story.setFeedName(Feed.TIMELINE);
        story.setViewPrincipal(new Principal(previous));
        when(storyRepository.findByNodeIdAndId(story.getNodeId(), story.getId())).thenReturn(Optional.of(story));
        UniversalContext context = mock(UniversalContext.class);
        Options options = mock(Options.class);
        when(options.getDuration("posting.published.notification.age")).thenReturn(ExtendedDuration.ALWAYS);
        when(context.getOptions()).thenReturn(options);
        when(context.nodeName()).thenReturn("local");
        ReflectionTestUtils.setField(receptor, "universalContext", context);
        doAnswer(invocation -> {
            StoryUpdatedLiberin liberin = invocation.getArgument(0);
            liberin.setNodeId(story.getNodeId());
            receptor.updated(liberin);
            return null;
        }).when(context).send(any(StoryUpdatedLiberin.class));
        StoryOperations operations = new StoryOperations();
        ReflectionTestUtils.setField(operations, "universalContext", context);
        StoryAttributes attributes = new StoryAttributes();
        var permissions = new org.moera.lib.node.types.StoryOperations();
        permissions.setView(new Principal(current));
        attributes.setOperations(permissions);

        operations.update(story, attributes);

        var events = receptor.events.stream()
            .filter(event -> event.getFilter().includes(false, reader, false, null))
            .map(Event::getType).toList();
        assertEquals(List.of(expected), events);
        var notifications = receptor.deliveries.stream()
            .filter(delivery -> delivery.direction().isPermitted(false, reader, false, new String[0]))
            .map(Delivery::notification).toList();
        if (expected == EventType.STORY_ADDED) {
            assertEquals(1, notifications.stream().filter(StoryAddedNotification.class::isInstance).count());
        }
        if (expected == EventType.STORY_ADDED || expected == EventType.STORY_DELETED) {
            var search = notifications.stream().filter(SearchContentUpdatedNotification.class::isInstance)
                .map(SearchContentUpdatedNotification.class::cast).toList();
            assertEquals(1, search.size());
            assertEquals(
                expected == EventType.STORY_ADDED
                    ? SearchContentUpdateType.POSTING_ADD : SearchContentUpdateType.POSTING_DELETE,
                search.getFirst().getUpdateType()
            );
            assertEquals(story.getId().toString(), search.getFirst().getPostingUpdate().getStoryId());
        }
    }

    @Test
    void updatedLoadsStoryByIdBeforeBuildingEventAndPush() {
        TestReceptor receptor = receptor();
        Story current = story();
        current.setMoment(200L);
        current.setRemoteOwnerName("alice");
        EntrySharing sharing = new EntrySharing();
        sharing.setRemoteStoryMoment(123L);
        current.setSharing(sharing);
        when(storyRepository.findByNodeIdAndId(current.getNodeId(), current.getId()))
            .thenReturn(Optional.of(current));
        StoryUpdatedLiberin liberin = new StoryUpdatedLiberin(current.getId());
        liberin.setNodeId(current.getNodeId());

        receptor.updated(liberin);

        assertEquals(1, receptor.events.size());
        StoryUpdatedEvent event = assertInstanceOf(StoryUpdatedEvent.class, receptor.events.getFirst());
        assertEquals(current.getId().toString(), event.getId());
        assertEquals(200L, event.getMoment());
        assertEquals("alice", event.getSummaryNodeName());
        assertEquals(1, receptor.pushes.size());
        PushContent push = receptor.pushes.getFirst();
        assertEquals(PushContentType.STORY_ADDED, push.getType());
        assertEquals(123L, push.getStory().getRemoteStoryMoment());
        assertEquals(200L, push.getStory().getMoment());
    }

    @Test
    void updatedSkipsStoryDeletedBeforeDelivery() {
        TestReceptor receptor = receptor();
        UUID nodeId = UUID.randomUUID();
        UUID storyId = UUID.randomUUID();
        StoryUpdatedLiberin liberin = new StoryUpdatedLiberin(storyId);
        liberin.setNodeId(nodeId);
        when(storyRepository.findByNodeIdAndId(nodeId, storyId)).thenReturn(Optional.empty());

        receptor.updated(liberin);

        assertTrue(receptor.events.isEmpty());
        assertTrue(receptor.pushes.isEmpty());
        assertTrue(receptor.liberins.isEmpty());
    }

    private static Story story() {
        Story story = new Story(UUID.randomUUID(), UUID.randomUUID(), StoryType.POSTING_SHARED);
        story.setFeedName(Feed.INSTANT);
        story.setMoment(100L);
        Posting posting = new Posting();
        posting.setId(UUID.randomUUID());
        story.setEntry(posting);
        return story;
    }

    private TestReceptor receptor() {
        TestReceptor receptor = new TestReceptor();
        ReflectionTestUtils.setField(receptor, "storyRepository", storyRepository);
        ReflectionTestUtils.setField(receptor, "storyOperations", mock(StoryOperations.class));
        return receptor;
    }

    private static class TestReceptor extends StoryReceptor {

        private final List<Event> events = new ArrayList<>();
        private final List<PushContent> pushes = new ArrayList<>();
        private final List<Liberin> liberins = new ArrayList<>();
        private final List<Delivery> deliveries = new ArrayList<>();

        @Override
        protected void send(Direction direction, Notification notification) {
            deliveries.add(new Delivery(direction, notification));
        }

        @Override
        protected void send(Liberin liberin, Event event) {
            events.add(event);
        }

        @Override
        protected void send(PushContent content) {
            pushes.add(content);
        }

        @Override
        protected void send(Liberin liberin) {
            liberins.add(liberin);
        }

    }

    private record Delivery(Direction direction, Notification notification) {
    }

}
