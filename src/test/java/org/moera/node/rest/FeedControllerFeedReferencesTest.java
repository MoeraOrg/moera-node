package org.moera.node.rest;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.moera.lib.node.types.Scope;
import org.moera.lib.node.types.StoryInfo;
import org.moera.lib.node.types.principal.Principal;
import org.moera.node.data.EntryRevision;
import org.moera.node.data.Feed;
import org.moera.node.data.Posting;
import org.moera.node.data.Story;
import org.moera.node.global.RequestContext;
import org.moera.node.operations.EntryOperations;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeedControllerFeedReferencesTest {

    @Mock
    private RequestContext requestContext;

    @Mock
    private EntryOperations entryOperations;

    @InjectMocks
    private FeedController feedController;

    @Test
    void postingInFeedIncludesReferencesFromAllFeedsForAdmin() {
        Story timeline = postingWithStories();
        when(requestContext.isPrincipal(Principal.ADMIN, Scope.VIEW_FEEDS)).thenReturn(true);
        when(entryOperations.getMediaAttachments(any(), any())).thenReturn(java.util.List.of());

        StoryInfo storyInfo = ReflectionTestUtils.invokeMethod(feedController, "buildStoryInfo", timeline);

        Assertions.assertNotNull(storyInfo);
        Assertions.assertEquals(
            Set.of(Feed.TIMELINE, Feed.NEWS),
            storyInfo.getPosting().getFeedReferences().stream()
                .map(reference -> reference.getFeedName())
                .collect(Collectors.toSet())
        );
    }

    @Test
    void postingInFeedHidesAdminFeedReferencesFromPublicClient() {
        Story timeline = postingWithStories();
        when(entryOperations.getMediaAttachments(any(), any())).thenReturn(java.util.List.of());

        StoryInfo storyInfo = ReflectionTestUtils.invokeMethod(feedController, "buildStoryInfo", timeline);

        Assertions.assertNotNull(storyInfo);
        Assertions.assertEquals(
            Set.of(Feed.TIMELINE),
            storyInfo.getPosting().getFeedReferences().stream()
                .map(reference -> reference.getFeedName())
                .collect(Collectors.toSet())
        );
    }

    @Test
    void earliestPublicationUsesOldestTimelineReference() {
        Story timeline = postingWithStories();
        timeline.setPublishedAt(Timestamp.from(Instant.ofEpochSecond(3000)));
        Story earlierTimeline = story(Feed.TIMELINE, 3L);
        earlierTimeline.setPublishedAt(Timestamp.from(Instant.ofEpochSecond(1000)));
        timeline.getEntry().addStory(earlierTimeline);
        timeline.getEntry().getStories().stream()
            .filter(story -> Feed.NEWS.equals(story.getFeedName()))
            .forEach(story -> story.setPublishedAt(Timestamp.from(Instant.ofEpochSecond(500))));
        when(entryOperations.getMediaAttachments(any(), any())).thenReturn(java.util.List.of());

        StoryInfo storyInfo = ReflectionTestUtils.invokeMethod(feedController, "buildStoryInfo", timeline);

        Assertions.assertNotNull(storyInfo);
        Assertions.assertEquals(1000L, storyInfo.getPosting().getEarliestPublishedAt());
    }

    @Test
    void postingWithoutTimelineReferenceHasNoEarliestPublication() {
        Story timeline = postingWithStories();
        Story news = timeline.getEntry().getStories().stream()
            .filter(story -> Feed.NEWS.equals(story.getFeedName()))
            .findFirst()
            .orElseThrow();
        news.setPublishedAt(Timestamp.from(Instant.ofEpochSecond(500)));
        timeline.getEntry().removeStory(timeline);
        when(entryOperations.getMediaAttachments(any(), any())).thenReturn(java.util.List.of());

        StoryInfo storyInfo = ReflectionTestUtils.invokeMethod(feedController, "buildStoryInfo", news);

        Assertions.assertNotNull(storyInfo);
        Assertions.assertNull(storyInfo.getPosting().getEarliestPublishedAt());
    }

    private static Story postingWithStories() {
        Posting posting = new Posting();
        posting.setId(UUID.randomUUID());
        posting.setOwnerName("owner");
        EntryRevision revision = new EntryRevision();
        revision.setId(UUID.randomUUID());
        revision.setBodyPreview("{}");
        revision.setBody("{}");
        revision.setBodySrc("body");
        posting.setCurrentRevision(revision);

        Story timeline = story(Feed.TIMELINE, 1L);
        Story news = story(Feed.NEWS, 2L);
        posting.addStory(timeline);
        posting.addStory(news);
        return timeline;
    }

    private static Story story(String feedName, long moment) {
        Story story = new Story();
        story.setId(UUID.randomUUID());
        story.setFeedName(feedName);
        story.setMoment(moment);
        return story;
    }

}
