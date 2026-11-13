package org.moera.node.rest;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Query;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.moera.lib.node.types.Result;
import org.moera.lib.node.types.StoryType;
import org.moera.node.data.Feed;
import org.moera.node.data.Posting;
import org.moera.node.data.PostingRepository;
import org.moera.node.data.Story;
import org.moera.node.data.StoryRepository;
import org.moera.node.global.RequestContext;
import org.moera.node.global.UniversalContext;
import org.moera.node.liberin.model.PostingPublishedAtUpdatedLiberin;
import org.moera.node.liberin.model.StoryDeletedLiberin;
import org.moera.node.operations.StoryOperations;
import org.moera.node.util.Util;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FeedControllerPublicationUpdatesTest {

    @ParameterizedTest
    @ValueSource(longs = {1000L, 2000L})
    void deletingFeedNotifiesWhenTheLastEarliestPublicationIsRemoved(long otherPublishedAt) {
        Posting posting = new Posting();
        posting.setId(UUID.randomUUID());
        posting.setNodeId(UUID.randomUUID());
        Story earliest = story(posting, 1000L);
        Story other = story(posting, otherPublishedAt);
        List<Story> stories = new ArrayList<>(List.of(earliest, other));
        EntityManager entityManager = mock(EntityManager.class);
        when(entityManager.getEntityManagerFactory()).thenReturn(mock(EntityManagerFactory.class));
        Query query = mock(Query.class);
        when(entityManager.createQuery(anyString())).thenReturn(query);
        when(query.getResultList()).thenReturn(List.of(other, earliest), List.of());
        StoryRepository storyRepository = mock(StoryRepository.class);
        when(storyRepository.findByEntryId(posting.getNodeId(), posting.getId()))
            .thenAnswer(invocation -> List.copyOf(stories));
        doAnswer(invocation -> {
            stories.remove(invocation.getArgument(0));
            return null;
        }).when(storyRepository).delete(any(Story.class));
        UniversalContext universalContext = mock(UniversalContext.class);
        when(universalContext.nodeId()).thenReturn(posting.getNodeId());
        StoryOperations storyOperations = new StoryOperations();
        ReflectionTestUtils.setField(storyOperations, "storyRepository", storyRepository);
        ReflectionTestUtils.setField(storyOperations, "entityManager", entityManager);
        ReflectionTestUtils.setField(storyOperations, "universalContext", universalContext);
        ReflectionTestUtils.setField(storyOperations, "postingRepository", mock(PostingRepository.class));
        RequestContext requestContext = mock(RequestContext.class);
        when(requestContext.nodeId()).thenReturn(posting.getNodeId());
        FeedController controller = new FeedController();
        ReflectionTestUtils.setField(controller, "requestContext", requestContext);
        ReflectionTestUtils.setField(controller, "entityManager", entityManager);
        ReflectionTestUtils.setField(controller, "storyRepository", storyRepository);
        ReflectionTestUtils.setField(controller, "storyOperations", storyOperations);

        assertSame(Result.OK, controller.deleteStories(Feed.TIMELINE, null, null, null, null, null, true));

        assertTrue(stories.isEmpty());
        verify(universalContext, times(2)).send(any(StoryDeletedLiberin.class));
        var captor = org.mockito.ArgumentCaptor.forClass(PostingPublishedAtUpdatedLiberin.class);
        verify(universalContext).send(captor.capture());
        assertEquals(posting.getId(), captor.getValue().getPostingId());
    }

    private static Story story(Posting posting, long publishedAt) {
        Story story = new Story(UUID.randomUUID(), posting.getNodeId(), StoryType.POSTING_ADDED);
        story.setFeedName(Feed.TIMELINE);
        story.setEntry(posting);
        story.setPublishedAt(Util.toTimestamp(publishedAt));
        story.setMoment(publishedAt);
        return story;
    }

}
