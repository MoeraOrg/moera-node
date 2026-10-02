package org.moera.node.liberin.receptor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.moera.lib.node.types.PushContent;
import org.moera.lib.node.types.PushContentType;
import org.moera.lib.node.types.StoryType;
import org.moera.node.data.EntrySharing;
import org.moera.node.data.Feed;
import org.moera.node.data.Posting;
import org.moera.node.data.Story;
import org.moera.node.data.StoryRepository;
import org.moera.node.liberin.Liberin;
import org.moera.node.liberin.model.StoryUpdatedLiberin;
import org.moera.node.model.event.Event;
import org.moera.node.model.event.StoryUpdatedEvent;
import org.moera.node.operations.StoryOperations;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StoryReceptorTest {

    private final StoryRepository storyRepository = mock(StoryRepository.class);

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

}
