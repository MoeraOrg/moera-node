package org.moera.node.rest.task;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.moera.lib.node.MoeraNode;
import org.moera.lib.node.types.FeedSliceInfo;
import org.moera.lib.node.types.PostingInfo;
import org.moera.lib.node.types.StoryInfo;
import org.moera.lib.node.types.StoryType;
import org.moera.node.api.node.NodeApi;
import org.moera.node.data.Feed;
import org.moera.node.data.Pick;
import org.moera.node.data.Posting;
import org.moera.node.data.PostingRepository;
import org.moera.node.data.StoryRepository;
import org.moera.node.global.UniversalContext;
import org.moera.node.picker.PickerPool;
import org.moera.node.util.Transaction;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.moera.node.util.MediaTransactionTestSupport.assertOutsideTransaction;
import static org.moera.node.util.MediaTransactionTestSupport.transaction;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class RemoteFeedFetchJobTest {

    private final UUID nodeId = UUID.randomUUID();
    private final PostingRepository postingRepository = mock(PostingRepository.class);
    private final StoryRepository storyRepository = mock(StoryRepository.class);
    private final PickerPool pickerPool = mock(PickerPool.class);
    private final UniversalContext universalContext = mock(UniversalContext.class);
    private final MoeraNode remote = mock(MoeraNode.class);
    private final RemoteFeedFetchJob job = new RemoteFeedFetchJob();

    @BeforeEach
    void setUp() throws Exception {
        NodeApi nodeApi = mock(NodeApi.class);
        when(nodeApi.at("remote")).thenReturn(remote);
        when(universalContext.nodeName()).thenReturn("local");
        when(remote.getFeedSlice(eq(Feed.TIMELINE), isNull(), any(), anyInt())).thenAnswer(invocation -> {
            assertOutsideTransaction();
            FeedSliceInfo sliceInfo = new FeedSliceInfo();
            sliceInfo.setStories(List.of());
            return sliceInfo;
        });
        Transaction tx = transaction();
        ReflectionTestUtils.setField(job, "nodeApi", nodeApi);
        ReflectionTestUtils.setField(job, "postingRepository", postingRepository);
        ReflectionTestUtils.setField(job, "storyRepository", storyRepository);
        ReflectionTestUtils.setField(job, "pickerPool", pickerPool);
        ReflectionTestUtils.setField(job, "universalContext", universalContext);
        ReflectionTestUtils.setField(job, "tx", tx);
        job.setNodeId(nodeId);
        ReflectionTestUtils.setField(
            job, "parameters", new RemoteFeedFetchJob.Parameters(Feed.NEWS, "remote", Feed.TIMELINE)
        );
    }

    @Test
    void initialFeedFetchRequestsFiveStories() throws Exception {
        job.execute();

        verify(remote).getFeedSlice(Feed.TIMELINE, null, null, 5);
    }

    @Test
    void existingPostingsReduceFetchLimitWithoutChangingRecommendation() throws Exception {
        Posting recommended = posting(true);
        Posting ordinary = posting(false);
        when(postingRepository.findByOwnerNameAndFeed(nodeId, "remote", Feed.NEWS))
            .thenReturn(List.of(recommended, ordinary));

        job.execute();

        assertTrue(recommended.isRecommended());
        verify(remote).getFeedSlice(Feed.TIMELINE, null, null, 3);
        verify(universalContext, never()).send(any());
    }

    @ParameterizedTest
    @ValueSource(ints = {5, 7})
    void fullFeedDoesNotFetchOrChangeRecommendations(int count) throws Exception {
        List<Posting> postings = IntStream.range(0, count).mapToObj(ignored -> posting(true)).toList();
        when(postingRepository.findByOwnerNameAndFeed(nodeId, "remote", Feed.NEWS)).thenReturn(postings);

        job.execute();

        postings.forEach(posting -> assertTrue(posting.isRecommended()));
        verify(universalContext, never()).send(any());
        verifyNoInteractions(remote, pickerPool);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void postingAlreadyInDestinationFeedIsNotPicked(boolean repost) throws Exception {
        Posting posting = posting(false);
        StoryInfo story = story("existing");
        if (repost) {
            story.getPosting().setReceiverName("original");
            story.getPosting().setReceiverPostingId("original-id");
        }
        String receiverName = repost ? "original" : "remote";
        String receiverId = repost ? "original-id" : "existing";
        when(postingRepository.findByReceiverId(nodeId, receiverName, receiverId)).thenReturn(Optional.of(posting));
        when(storyRepository.countByFeedAndTypeAndEntryId(nodeId, Feed.NEWS, StoryType.POSTING_ADDED, posting.getId()))
            .thenReturn(1);
        slice(List.of(story, story("new")));

        job.execute();

        ArgumentCaptor<Pick> captor = ArgumentCaptor.forClass(Pick.class);
        verify(pickerPool).pick(captor.capture());
        assertEquals("new", captor.getValue().getRemotePostingId());
        assertEquals(nodeId, captor.getValue().getNodeId());
        assertEquals(Feed.NEWS, captor.getValue().getFeedName());
    }

    @Test
    void cachedPostingOutsideDestinationFeedIsPicked() throws Exception {
        Posting posting = posting(false);
        when(postingRepository.findByReceiverId(nodeId, "remote", "cached")).thenReturn(Optional.of(posting));
        slice(List.of(story("cached")));

        job.execute();

        ArgumentCaptor<Pick> captor = ArgumentCaptor.forClass(Pick.class);
        verify(pickerPool).pick(captor.capture());
        assertEquals("cached", captor.getValue().getRemotePostingId());
        verify(universalContext, never()).send(any());
    }

    @Test
    void repostOfLocalOriginalIsNotPickedWithoutCheckingDestinationFeed() throws Exception {
        StoryInfo story = story("repost");
        story.getPosting().setReceiverName("local");
        story.getPosting().setReceiverPostingId(UUID.randomUUID().toString());
        slice(List.of(story));

        job.execute();

        verifyNoInteractions(pickerPool, storyRepository);
        verify(postingRepository, never()).findByNodeIdAndId(any(), any());
        verify(postingRepository, never()).findByReceiverId(any(), any(), any());
    }

    @Test
    void repostOfLocalOriginalDoesNotConsumeRemainingFetchLimit() throws Exception {
        when(postingRepository.findByOwnerNameAndFeed(nodeId, "remote", Feed.NEWS))
            .thenReturn(List.of(posting(false), posting(false), posting(false), posting(false)));
        StoryInfo own = story("own-repost");
        own.getPosting().setReceiverName("local");
        own.getPosting().setReceiverPostingId(UUID.randomUUID().toString());
        when(remote.getFeedSlice(Feed.TIMELINE, null, null, 1)).thenReturn(page(100, 1, own));
        when(remote.getFeedSlice(Feed.TIMELINE, null, 100L, 1))
            .thenReturn(page(-9007199254740991L, 0, story("older")));

        job.execute();

        ArgumentCaptor<Pick> captor = ArgumentCaptor.forClass(Pick.class);
        verify(pickerPool).pick(captor.capture());
        assertEquals("older", captor.getValue().getRemotePostingId());
        verify(remote).getFeedSlice(Feed.TIMELINE, null, 100L, 1);
    }

    @Test
    void skipsExistingPostingsAndFetchesRemainingPostingsFromOlderSlice() throws Exception {
        Posting existing = posting(false);
        when(postingRepository.findByOwnerNameAndFeed(nodeId, "remote", Feed.NEWS))
            .thenReturn(List.of(existing, posting(false)));
        when(postingRepository.findByReceiverId(nodeId, "remote", "existing")).thenReturn(Optional.of(existing));
        when(storyRepository.countByFeedAndTypeAndEntryId(nodeId, Feed.NEWS, StoryType.POSTING_ADDED, existing.getId()))
            .thenReturn(1);
        when(remote.getFeedSlice(Feed.TIMELINE, null, null, 3))
            .thenReturn(page(80, 2, story("existing"), story("newest")));
        when(remote.getFeedSlice(Feed.TIMELINE, null, 80L, 2))
            .thenReturn(page(-9007199254740991L, 0, story("older"), story("oldest")));

        job.execute();

        ArgumentCaptor<Pick> captor = ArgumentCaptor.forClass(Pick.class);
        verify(pickerPool, times(3)).pick(captor.capture());
        assertEquals(List.of("oldest", "older", "newest"), captor.getAllValues().stream()
            .map(Pick::getRemotePostingId).toList());
        verify(remote).getFeedSlice(Feed.TIMELINE, null, 80L, 2);
    }

    @Test
    void filteredStoriesDoNotConsumeFetchLimit() throws Exception {
        StoryInfo pinned = story("pinned");
        pinned.setPinned(true);
        StoryInfo otherType = new StoryInfo();
        otherType.setStoryType(StoryType.POSTING_UPDATED);
        when(remote.getFeedSlice(Feed.TIMELINE, null, null, 5))
            .thenReturn(page(100, 1, pinned, otherType));
        when(remote.getFeedSlice(Feed.TIMELINE, null, 100L, 5))
            .thenReturn(page(-9007199254740991L, 0, story("only-posting")));

        job.execute();

        ArgumentCaptor<Pick> captor = ArgumentCaptor.forClass(Pick.class);
        verify(pickerPool).pick(captor.capture());
        assertEquals("only-posting", captor.getValue().getRemotePostingId());
        verify(remote).getFeedSlice(Feed.TIMELINE, null, 100L, 5);
    }

    @Test
    void duplicateRepostsDoNotConsumeFetchLimit() throws Exception {
        when(postingRepository.findByOwnerNameAndFeed(nodeId, "remote", Feed.NEWS))
            .thenReturn(List.of(posting(false), posting(false), posting(false)));
        StoryInfo first = story("first-repost");
        first.getPosting().setReceiverName("original");
        first.getPosting().setReceiverPostingId("original-id");
        StoryInfo second = story("second-repost");
        second.getPosting().setReceiverName("original");
        second.getPosting().setReceiverPostingId("original-id");
        when(remote.getFeedSlice(Feed.TIMELINE, null, null, 2))
            .thenReturn(page(50, 2, first, second));
        when(remote.getFeedSlice(Feed.TIMELINE, null, 50L, 1))
            .thenReturn(page(-9007199254740991L, 0, second, story("older")));

        job.execute();

        ArgumentCaptor<Pick> captor = ArgumentCaptor.forClass(Pick.class);
        verify(pickerPool, times(2)).pick(captor.capture());
        assertEquals(List.of("older", "first-repost"), captor.getAllValues().stream()
            .map(Pick::getRemotePostingId).toList());
    }

    @Test
    void sliceWithMoreStoriesThanRequestedDoesNotExceedFetchLimit() throws Exception {
        when(postingRepository.findByOwnerNameAndFeed(nodeId, "remote", Feed.NEWS))
            .thenReturn(List.of(posting(false), posting(false), posting(false), posting(false)));
        slice(List.of(story("newest"), story("older")));

        job.execute();

        ArgumentCaptor<Pick> captor = ArgumentCaptor.forClass(Pick.class);
        verify(pickerPool).pick(captor.capture());
        assertEquals("newest", captor.getValue().getRemotePostingId());
    }

    @Test
    void emptySlicesContinueUntilCursorStopsAdvancing() throws Exception {
        when(remote.getFeedSlice(Feed.TIMELINE, null, null, 5)).thenReturn(page(100, 2));
        when(remote.getFeedSlice(Feed.TIMELINE, null, 100L, 5)).thenReturn(page(100, 2));

        job.execute();

        verify(remote).getFeedSlice(Feed.TIMELINE, null, 100L, 5);
        verify(remote, times(2)).getFeedSlice(eq(Feed.TIMELINE), isNull(), any(), anyInt());
        verifyNoInteractions(pickerPool);
    }

    private FeedSliceInfo page(long after, int totalInPast, StoryInfo... stories) {
        FeedSliceInfo page = new FeedSliceInfo();
        page.setBefore(9007199254740991L);
        page.setAfter(after);
        page.setTotalInPast(totalInPast);
        page.setStories(List.of(stories));
        return page;
    }

    private Posting posting(boolean recommended) {
        Posting posting = new Posting();
        posting.setId(UUID.randomUUID());
        posting.setNodeId(nodeId);
        posting.setOwnerName("remote");
        posting.setRecommended(recommended);
        return posting;
    }

    private StoryInfo story(String id) {
        PostingInfo posting = new PostingInfo();
        posting.setId(id);
        StoryInfo story = new StoryInfo();
        story.setStoryType(StoryType.POSTING_ADDED);
        story.setPosting(posting);
        return story;
    }

    private void slice(List<StoryInfo> stories) throws Exception {
        FeedSliceInfo sliceInfo = new FeedSliceInfo();
        sliceInfo.setStories(stories);
        when(remote.getFeedSlice(eq(Feed.TIMELINE), isNull(), isNull(), anyInt())).thenReturn(sliceInfo);
    }

}
