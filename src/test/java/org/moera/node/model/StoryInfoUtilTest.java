package org.moera.node.model;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.moera.lib.node.types.principal.AccessCheckers;
import org.moera.lib.node.types.Scope;
import org.moera.lib.node.types.StoryInfo;
import org.moera.lib.node.types.StoryType;
import org.moera.node.data.EntrySharing;
import org.moera.node.data.Posting;
import org.moera.node.data.Story;
import org.moera.node.event.EventSubscriber;
import org.moera.node.model.event.EventType;
import org.moera.node.model.event.StoryAddedEvent;
import org.moera.node.model.event.StoryEvent;
import org.moera.node.model.event.StoryUpdatedEvent;
import org.moera.node.util.Util;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

class StoryInfoUtilTest {

    @Test
    void sharedPostingReturnsRemoteStoryMoment() {
        Story story = story(StoryType.POSTING_SHARED);
        EntrySharing sharing = new EntrySharing();
        sharing.setRemoteStoryMoment(123456789L);
        story.setSharing(sharing);

        StoryInfo info = StoryInfoUtil.build(story, AccessCheckers.ADMIN, st -> null, null);

        assertEquals(123456789L, info.getRemoteStoryMoment());
    }

    @Test
    void sharedPostingWithoutSharingHasNoRemoteStoryMoment() {
        Story story = story(StoryType.POSTING_SHARED);

        StoryInfo info = StoryInfoUtil.build(story, AccessCheckers.ADMIN, st -> null, null);

        assertNull(info.getRemoteStoryMoment());
    }

    @Test
    void sharedPostingWithoutRemoteMomentHasNoRemoteStoryMoment() {
        Story story = story(StoryType.POSTING_SHARED);
        story.setSharing(new EntrySharing());

        StoryInfo info = StoryInfoUtil.build(story, AccessCheckers.ADMIN, st -> null, null);

        assertNull(info.getRemoteStoryMoment());
    }

    @ParameterizedTest
    @EnumSource(value = StoryType.class, names = {"REACTION_ADDED_POSITIVE", "REACTION_ADDED_NEGATIVE"})
    void reactionsDoNotReturnRemoteStoryMoment(StoryType storyType) {
        Story story = story(storyType);
        EntrySharing sharing = new EntrySharing();
        sharing.setRemoteStoryMoment(123456789L);
        story.setSharing(sharing);

        StoryInfo info = StoryInfoUtil.build(story, AccessCheckers.ADMIN, st -> null, null);

        assertNull(info.getRemoteStoryMoment());
    }

    @ParameterizedTest
    @MethodSource("storyAccess")
    void externalSourcesRequireAdminContentScope(long adminScope, boolean sourcesVisible, boolean feedFlagsVisible) {
        Story story = story(StoryType.POSTING_ADDED);
        story.setExternalSourceUri(new String[]{"https://one.example/story", "https://two.example/story"});
        EventSubscriber subscriber = subscriber(adminScope);

        StoryInfo info = StoryInfoUtil.build(story, subscriber, ignored -> null, null);

        assertEquals(
            sourcesVisible ? List.of("https://one.example/story", "https://two.example/story") : null,
            info.getExternalSourceUri()
        );
        assertEquals(feedFlagsVisible ? Boolean.FALSE : null, info.getViewed());
    }

    @ParameterizedTest
    @MethodSource("storyAccess")
    void storyEventsProtectExternalSourcesForEachScope(
        long adminScope,
        boolean sourcesVisible,
        boolean feedFlagsVisible
    ) {
        Story story = story(StoryType.POSTING_ADDED);
        story.setExternalSourceUri(new String[]{"https://one.example/story", "https://two.example/story"});
        EventSubscriber subscriber = subscriber(adminScope);
        ObjectMapper objectMapper = new ObjectMapper();

        for (StoryEvent event : List.of(
            new StoryAddedEvent(story, feedFlagsVisible, null),
            new StoryUpdatedEvent(story, feedFlagsVisible, null)
        )) {
            event.protect(subscriber);

            if (sourcesVisible) {
                assertEquals(
                    objectMapper.valueToTree(List.of("https://one.example/story", "https://two.example/story")),
                    objectMapper.valueToTree(event).get("externalSourceUri")
                );
            } else {
                assertFalse(objectMapper.valueToTree(event).has("externalSourceUri"));
            }
            assertEquals(feedFlagsVisible ? Boolean.FALSE : null, event.getViewed());
        }
    }

    @ParameterizedTest
    @EnumSource(value = EventType.class, names = {"STORY_ADDED", "STORY_UPDATED"})
    void protectingEventCopiesPreservesSourcesForAuthorizedSubscribers(EventType eventType) {
        Story story = story(StoryType.POSTING_ADDED);
        story.setExternalSourceUri(new String[]{"https://one.example/story", "https://two.example/story"});
        StoryEvent event = eventType == EventType.STORY_ADDED
            ? new StoryAddedEvent(story, true, null)
            : new StoryUpdatedEvent(story, true, null);

        StoryEvent publicEvent = (StoryEvent) event.copy();
        publicEvent.protect(subscriber(Scope.VIEW_FEEDS.getMask()));
        assertNull(publicEvent.getExternalSourceUri());

        StoryEvent adminEvent = (StoryEvent) event.copy();
        adminEvent.protect(subscriber(Scope.VIEW_FEEDS.getMask() | Scope.VIEW_CONTENT.getMask()));
        assertEquals(List.of("https://one.example/story", "https://two.example/story"), adminEvent.getExternalSourceUri());

        assertNull(publicEvent.getExternalSourceUri());
        assertEquals(List.of("https://one.example/story", "https://two.example/story"), event.getExternalSourceUri());
    }

    private static Stream<Arguments> storyAccess() {
        return Stream.of(
            Arguments.of(0L, false, false),
            Arguments.of(Scope.VIEW_FEEDS.getMask(), false, true),
            Arguments.of(Scope.VIEW_CONTENT.getMask(), true, false),
            Arguments.of(Scope.VIEW_FEEDS.getMask() | Scope.VIEW_CONTENT.getMask(), true, true)
        );
    }

    private static EventSubscriber subscriber(long adminScope) {
        EventSubscriber subscriber = new EventSubscriber();
        subscriber.setAdminScope(adminScope);
        subscriber.setClientScope(Scope.VIEW_CONTENT.getMask());
        subscriber.setClientName("client");
        return subscriber;
    }

    private static Story story(StoryType storyType) {
        Story story = new Story(UUID.randomUUID(), UUID.randomUUID(), storyType);
        story.setCreatedAt(Util.now());
        story.setPublishedAt(Util.now());
        story.setMoment(987654321L);
        story.setSummary("");
        Posting posting = new Posting();
        posting.setId(UUID.randomUUID());
        story.setEntry(posting);
        return story;
    }

}
