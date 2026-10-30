package org.moera.node.operations;

import java.util.List;
import java.util.UUID;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.moera.lib.node.types.principal.AccessCheckers;
import org.moera.lib.node.types.StoryAttributes;
import org.moera.lib.node.types.StoryInfo;
import org.moera.lib.node.types.StoryType;
import org.moera.node.data.Feed;
import org.moera.node.data.Posting;
import org.moera.node.data.Story;
import org.moera.node.data.StoryRepository;
import org.moera.node.global.UniversalContext;
import org.moera.node.liberin.model.PostingPublishedAtUpdatedLiberin;
import org.moera.node.model.StoryInfoUtil;
import org.moera.node.userlist.SheriffUserListOperations;
import org.moera.node.util.Util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StoryOperationsTest {

    @Mock
    private UniversalContext universalContext;

    @Mock
    private StoryRepository storyRepository;

    @Mock
    private SheriffUserListOperations sheriffUserListOperations;

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private StoryOperations storyOperations;

    @ParameterizedTest
    @CsvSource({"timeline, timeline", "news, news", ", timeline"})
    void publicationStoresFeedAndExternalSourcesIndependentlyOfPosting(String feedName, String expectedFeedName) {
        Posting posting = new Posting();
        posting.setId(UUID.randomUUID());
        posting.setExternalSourceUri(new String[]{"https://posting.example/post"});
        StoryAttributes attributes = new StoryAttributes();
        attributes.setFeedName(feedName);
        attributes.setExternalSourceUri(List.of("https://one.example/story", "https://two.example/story"));
        when(universalContext.nodeId()).thenReturn(UUID.randomUUID());
        when(storyRepository.saveAndFlush(any(Story.class))).thenAnswer(invocation -> invocation.getArgument(0));

        storyOperations.publish(posting, List.of(attributes));

        assertEquals(1, posting.getStories().size());
        Story story = posting.getStories().iterator().next();
        assertEquals(expectedFeedName, story.getFeedName());
        StoryInfo info = StoryInfoUtil.build(story, AccessCheckers.ADMIN, ignored -> null, null);
        assertEquals(List.of("https://one.example/story", "https://two.example/story"), info.getExternalSourceUri());
        assertArrayEquals(new String[]{"https://posting.example/post"}, posting.getExternalSourceUri());
    }

    @ParameterizedTest
    @CsvSource({
        "timeline, 1000, 900, true",
        "timeline, 1000, 1000, false",
        "timeline, 1000, 1100, false",
        "timeline, , 900, true",
        ", 1000, 900, true",
        "news, 1000, 900, false",
        "news, , 900, false"
    })
    void publishNotifiesPostingOnlyWhenEarliestPublicationChanges(
        String feedName,
        Long earliestPublishedAt,
        long publishedAt,
        boolean notify
    ) {
        Posting posting = posting();
        if (feedName == null || Feed.TIMELINE.equals(feedName)) {
            List<Story> stories = earliestPublishedAt != null
                ? List.of(story(posting, Feed.TIMELINE, earliestPublishedAt), story(posting, Feed.NEWS, 100L))
                : List.of(story(posting, Feed.NEWS, 100L));
            when(storyRepository.findByEntryId(posting.getNodeId(), posting.getId())).thenReturn(stories);
        }

        storyOperations.publish(posting, List.of(publication(feedName, publishedAt)));

        assertEquals(1, posting.getStories().size());
        assertEquals(publishedAt, Util.toEpochSecond(posting.getStories().iterator().next().getPublishedAt()));
        verifyPublicationUpdate(posting, notify);
    }

    @Test
    void publishNotifiesOnceForSeveralEarlierPublications() {
        Posting posting = posting();
        when(storyRepository.findByEntryId(posting.getNodeId(), posting.getId()))
            .thenReturn(List.of(story(posting, Feed.TIMELINE, 1000L)));

        storyOperations.publish(posting, List.of(publication(Feed.TIMELINE, 900L), publication(Feed.TIMELINE, 800L)));

        assertEquals(2, posting.getStories().size());
        verifyPublicationUpdate(posting, true);
    }

    @Test
    void publishSeesPublicationCommittedWhileWaitingForPostingLock() {
        Posting posting = posting();
        Story earlier = story(posting, Feed.TIMELINE, 1000L);
        doAnswer(invocation -> {
            earlier.setPublishedAt(Util.toTimestamp(500L));
            return null;
        }).when(entityManager).lock(posting, LockModeType.PESSIMISTIC_WRITE);
        when(storyRepository.findByEntryId(posting.getNodeId(), posting.getId())).thenReturn(List.of(earlier));

        storyOperations.publish(posting, List.of(publication(Feed.TIMELINE, 900L)));

        verify(universalContext, never()).send(any(PostingPublishedAtUpdatedLiberin.class));
    }

    private Posting posting() {
        Posting posting = new Posting();
        posting.setId(UUID.randomUUID());
        posting.setNodeId(UUID.randomUUID());
        when(universalContext.nodeId()).thenReturn(posting.getNodeId());
        when(storyRepository.saveAndFlush(any(Story.class))).thenAnswer(invocation -> invocation.getArgument(0));
        return posting;
    }

    private static Story story(Posting posting, String feedName, long publishedAt) {
        Story story = new Story(UUID.randomUUID(), posting.getNodeId(), StoryType.POSTING_ADDED);
        story.setEntry(posting);
        story.setFeedName(feedName);
        story.setPublishedAt(Util.toTimestamp(publishedAt));
        return story;
    }

    private static StoryAttributes publication(String feedName, long publishedAt) {
        StoryAttributes attributes = new StoryAttributes();
        attributes.setFeedName(feedName);
        attributes.setPublishAt(publishedAt);
        return attributes;
    }

    private void verifyPublicationUpdate(Posting posting, boolean notify) {
        var captor = ArgumentCaptor.forClass(PostingPublishedAtUpdatedLiberin.class);
        verify(universalContext, times(notify ? 1 : 0)).send(captor.capture());
        if (notify) {
            assertEquals(posting.getId(), captor.getValue().getPostingId());
        }
    }

}
