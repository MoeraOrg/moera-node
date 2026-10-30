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
import org.moera.lib.node.types.StorySummaryData;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

class StoryInfoUtilTest {

    @ParameterizedTest
    @EnumSource(value = StoryType.class, names = {"POSTING_REFERRED", "POSTING_SHARED"})
    void structuredSharingSummaryIsPreservedWithoutLegacyTextInApiAndEvents(StoryType storyType) {
        Story story = story(storyType);
        StorySummaryData summary = new StorySummaryData();
        summary.setSharings(List.of(StorySummaryNodeUtil.build("alice_0", "Alice", null, "female")));
        summary.setTotalSharings(1);
        summary.setPosting(StorySummaryEntryUtil.build("author_0", "Author", null, null, "Referenced example"));
        if (storyType == StoryType.POSTING_REFERRED) {
            summary.setReferences(List.of(
                StorySummaryEntryUtil.build("alice_0", "Alice", null, "female", "Source example")
            ));
        }
        story.setSummaryData(summary);

        StoryInfo info = StoryInfoUtil.build(story, AccessCheckers.ADMIN, ignored -> null, null);

        assertNull(info.getSummary());
        ObjectMapper objectMapper = new ObjectMapper();
        var expectedSummary = objectMapper.valueToTree(summary);
        var apiJson = objectMapper.valueToTree(info);
        assertFalse(apiJson.hasNonNull("summary"));
        assertEquals(expectedSummary, apiJson.path("summaryData"));
        for (StoryEvent event : List.of(
            new StoryAddedEvent(story, true, null), new StoryUpdatedEvent(story, true, null)
        )) {
            for (StoryEvent version : List.of(event, (StoryEvent) event.copy())) {
                var eventJson = objectMapper.valueToTree(version);
                assertFalse(eventJson.hasNonNull("summary"));
                assertEquals(expectedSummary, eventJson.path("summaryData"));
            }
        }
    }

    @Test
    void referredPostingReturnsTheReferringPostingAndActorInApiAndEvents() {
        Story story = story(StoryType.POSTING_REFERRED);
        story.setRemoteOwnerName("alice_0");
        story.setRemoteOwnerFullName("Alice");
        EntrySharing sharing = new EntrySharing();
        sharing.setRemoteNodeName("alice_0");
        sharing.setRemotePostingId("referring-post");
        story.setSharing(sharing);

        StoryInfo info = StoryInfoUtil.build(story, AccessCheckers.ADMIN, ignored -> null, null);

        assertEquals(story.getEntry().getId().toString(), info.getPostingId());
        assertEquals("alice_0", info.getSummaryNodeName());
        assertEquals("Alice", info.getSummaryFullName());
        assertEquals("alice_0", info.getRemoteNodeName());
        assertEquals("referring-post", info.getRemotePostingId());
        assertNull(info.getRemoteStoryMoment());
        ObjectMapper objectMapper = new ObjectMapper();
        for (StoryEvent event : List.of(
            new StoryAddedEvent(story, true, null), new StoryUpdatedEvent(story, true, null)
        )) {
            var json = objectMapper.valueToTree(event.copy());
            assertEquals("posting-referred", json.path("storyType").asString());
            assertEquals("referring-post", json.path("remotePostingId").asString());
            assertEquals("alice_0", json.path("summaryNodeName").asString());
        }
    }

    @Test
    void sharedPostingReturnsRemoteStoryMoment() {
        Story story = story(StoryType.POSTING_SHARED);
        EntrySharing sharing = new EntrySharing();
        sharing.setRemoteStoryMoment(123456789L);
        story.setSharing(sharing);

        StoryInfo info = StoryInfoUtil.build(story, AccessCheckers.ADMIN, st -> null, null);

        assertEquals(123456789L, info.getRemoteStoryMoment());
    }

    @ParameterizedTest
    @EnumSource(value = EventType.class, names = {"STORY_ADDED", "STORY_UPDATED"})
    void sharedPostingEventsReturnRemoteStoryMoment(EventType eventType) {
        Story story = story(StoryType.POSTING_SHARED);
        EntrySharing sharing = new EntrySharing();
        sharing.setRemoteStoryMoment(123456789L);
        story.setSharing(sharing);
        StoryEvent event = eventType == EventType.STORY_ADDED
            ? new StoryAddedEvent(story, true, null)
            : new StoryUpdatedEvent(story, true, null);
        ObjectMapper objectMapper = new ObjectMapper();

        assertTrue(objectMapper.valueToTree(event).has("remoteStoryMoment"));
        assertEquals(123456789L, objectMapper.valueToTree(event).path("remoteStoryMoment").asLong());
        assertEquals(123456789L, objectMapper.valueToTree(event.copy()).path("remoteStoryMoment").asLong());
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
