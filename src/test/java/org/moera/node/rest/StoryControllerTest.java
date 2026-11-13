package org.moera.node.rest;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;

import org.hibernate.LazyInitializationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.moera.lib.node.types.principal.AccessCheckers;
import org.moera.lib.node.types.Result;
import org.moera.lib.node.types.StoryAttributes;
import org.moera.lib.node.types.StoryInfo;
import org.moera.lib.node.types.StoryType;
import org.moera.lib.node.types.Scope;
import org.moera.lib.node.types.principal.Principal;
import org.moera.lib.node.types.validate.ValidationFailure;
import org.moera.node.data.Comment;
import org.moera.node.data.Entry;
import org.moera.node.data.EntryRevision;
import org.moera.node.data.EntryType;
import org.moera.node.data.Feed;
import org.moera.node.data.Posting;
import org.moera.node.data.PostingRepository;
import org.moera.node.data.Story;
import org.moera.node.data.StoryRepository;
import org.moera.node.global.RequestContext;
import org.moera.node.global.UniversalContext;
import org.moera.node.liberin.model.PostingPublishedAtUpdatedLiberin;
import org.moera.node.liberin.model.StoryDeletedLiberin;
import org.moera.node.liberin.model.StoryUpdatedLiberin;
import org.moera.node.model.StoryAttributesUtil;
import org.moera.node.model.StoryInfoUtil;
import org.moera.node.operations.EntryOperations;
import org.moera.node.operations.PostingInfoOperations;
import org.moera.node.operations.PostingOperations;
import org.moera.node.operations.StoryOperations;
import org.moera.node.util.Transaction;
import org.moera.node.util.Util;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StoryControllerTest {

    @Mock
    private RequestContext requestContext;

    @Mock
    private StoryRepository storyRepository;

    @Mock
    private PostingRepository postingRepository;

    @Mock
    private PostingOperations postingOperations;

    @Mock
    private EntryOperations entryOperations;

    @Mock
    private PostingInfoOperations postingInfoOperations;

    @Mock
    private EntityManager entityManager;

    @Mock
    private PlatformTransactionManager platformTransactionManager;

    @Mock
    private UniversalContext universalContext;

    private final Transaction tx = new Transaction();

    @InjectMocks
    private StoryController storyController;

    private boolean sessionOpen;

    @BeforeEach
    void setUp() {
        lenient().when(postingInfoOperations.withLinkedPostings(any())).thenAnswer(invocation -> invocation.getArgument(0));
        useRealStoryOperations();
        ReflectionTestUtils.setField(storyController, "tx", tx);
        ReflectionTestUtils.setField(tx, "txManager", platformTransactionManager);
        ReflectionTestUtils.setField(tx, "universalContext", universalContext);
        when(platformTransactionManager.getTransaction(any())).thenAnswer(invocation -> {
            sessionOpen = true;
            return new SimpleTransactionStatus();
        });
    }

    @ParameterizedTest
    @CsvSource({"timeline, news", "news, timeline", "timeline, ''"})
    void putRejectsFeedNameChangeBeforeApplyingAttributes(String feedName, String newFeedName) {
        Story story = postingStory(feedName, 1000L);
        StoryAttributes attributes = new StoryAttributes();
        attributes.setFeedName(newFeedName);
        attributes.setPublishAt(900L);
        attributes.setPinned(true);
        attributes.setViewed(true);
        attributes.setRead(true);

        ValidationFailure failure = assertThrows(
            ValidationFailure.class,
            () -> storyController.put(story.getId().toString(), attributes)
        );

        assertEquals("story.feed-name.cannot-modify", failure.getErrorCode());
        assertEquals(feedName, story.getFeedName());
        assertEquals(1000L, Util.toEpochSecond(story.getPublishedAt()));
        assertEquals(42L, story.getMoment());
        assertFalse(story.isPinned());
        assertFalse(story.isViewed());
        assertFalse(story.isRead());
        verifyNoInteractions(entityManager);
        verify(requestContext, never()).send(any());
        verify(universalContext, never()).send(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {Feed.TIMELINE, Feed.NEWS})
    void putAcceptsUnchangedFeedName(String feedName) {
        Story story = postingStory(feedName, 1000L);
        StoryAttributes attributes = new StoryAttributes();
        attributes.setFeedName(feedName);
        attributes.setViewed(true);

        StoryInfo info = storyController.put(story.getId().toString(), attributes);

        assertEquals(feedName, info.getFeedName());
        assertEquals(feedName, story.getFeedName());
        assertTrue(story.isViewed());
        assertEquals(42L, story.getMoment());
        verifyNoInteractions(entityManager);
        verify(universalContext).send(any(StoryUpdatedLiberin.class));
        verify(universalContext, never()).send(any(PostingPublishedAtUpdatedLiberin.class));
    }

    @ParameterizedTest
    @CsvSource({
        "timeline, 1000, 2000, 900, true",
        "timeline, 1000, 2000, 1500, true",
        "timeline, 1000, 2000, 2500, true",
        "timeline, 2000, 1000, 3000, false",
        "timeline, 2000, 1000, 900, true",
        "timeline, 1000, 1000, 2000, false",
        "timeline, 1000, 2000, 1000, false",
        "timeline, 1000, , 900, true",
        "timeline, 1000, , 1100, true",
        "news, 500, 1000, 100, false",
        "timeline, 1000, 2000, , false"
    })
    void putNotifiesPostingOnlyWhenEarliestPublicationChanges(
        String feedName,
        long publishedAt,
        Long otherPublishedAt,
        Long newPublishedAt,
        boolean notify
    ) {
        Story story = postingStory(feedName, publishedAt);
        UUID nodeId = story.getNodeId();
        Entry posting = story.getEntry();
        EntryRevision revision = posting.getCurrentRevision();
        List<Story> stories = new ArrayList<>(List.of(story));
        if (otherPublishedAt != null) {
            Story other = new Story(UUID.randomUUID(), nodeId, StoryType.POSTING_ADDED);
            other.setEntry(posting);
            other.setFeedName(Feed.TIMELINE);
            other.setPublishedAt(Util.toTimestamp(otherPublishedAt));
            stories.add(other);
        }
        if (newPublishedAt != null && Feed.TIMELINE.equals(feedName)) {
            when(storyRepository.findByEntryId(nodeId, posting.getId())).thenReturn(stories);
        }
        StoryAttributes attributes = new StoryAttributes();
        attributes.setPublishAt(newPublishedAt);
        attributes.setPinned(true);
        attributes.setViewed(true);
        attributes.setRead(true);

        storyController.put(story.getId().toString(), attributes);

        ArgumentCaptor<PostingPublishedAtUpdatedLiberin> captor =
            ArgumentCaptor.forClass(PostingPublishedAtUpdatedLiberin.class);
        verify(universalContext, times(notify ? 1 : 0)).send(captor.capture());
        if (notify) {
            assertEquals(posting.getId(), captor.getValue().getPostingId());
        }
        assertEquals(1000L, Util.toEpochSecond(posting.getEditedAt()));
        assertSame(revision, posting.getCurrentRevision());
    }

    @Test
    void putSeesPublicationCommittedWhileWaitingForPostingLock() {
        Story story = postingStory(Feed.TIMELINE, 1000L);
        AtomicLong otherPublishedAt = new AtomicLong(1000L);
        doAnswer(invocation -> {
            otherPublishedAt.set(2000L);
            return null;
        }).when(entityManager).lock(story.getEntry(), LockModeType.PESSIMISTIC_WRITE);
        when(storyRepository.findByEntryId(story.getNodeId(), story.getEntry().getId())).thenAnswer(invocation -> {
            Story other = new Story(UUID.randomUUID(), story.getNodeId(), StoryType.POSTING_ADDED);
            other.setEntry(story.getEntry());
            other.setFeedName(Feed.TIMELINE);
            other.setPublishedAt(Util.toTimestamp(otherPublishedAt.get()));
            return List.of(story, other);
        });
        StoryAttributes attributes = new StoryAttributes();
        attributes.setPublishAt(3000L);

        storyController.put(story.getId().toString(), attributes);

        ArgumentCaptor<PostingPublishedAtUpdatedLiberin> captor =
            ArgumentCaptor.forClass(PostingPublishedAtUpdatedLiberin.class);
        verify(universalContext).send(captor.capture());
        assertEquals(story.getEntry().getId(), captor.getValue().getPostingId());
    }

    @Test
    void putReloadsStoryChangedWhileWaitingForPostingLock() {
        Story story = postingStory(Feed.TIMELINE, 1000L);
        doAnswer(invocation -> {
            story.setPublishedAt(Util.toTimestamp(2000L));
            return null;
        }).when(entityManager).refresh(story);
        Story other = new Story(UUID.randomUUID(), story.getNodeId(), StoryType.POSTING_ADDED);
        other.setEntry(story.getEntry());
        other.setFeedName(Feed.TIMELINE);
        other.setPublishedAt(Util.toTimestamp(1500L));
        doNothing().when(entityManager).refresh(other);
        when(storyRepository.findByEntryId(story.getNodeId(), story.getEntry().getId()))
            .thenReturn(List.of(story, other));
        StoryAttributes attributes = new StoryAttributes();
        attributes.setPublishAt(1000L);

        storyController.put(story.getId().toString(), attributes);

        ArgumentCaptor<PostingPublishedAtUpdatedLiberin> captor =
            ArgumentCaptor.forClass(PostingPublishedAtUpdatedLiberin.class);
        verify(universalContext).send(captor.capture());
        assertEquals(story.getEntry().getId(), captor.getValue().getPostingId());
        var order = org.mockito.Mockito.inOrder(entityManager, storyRepository);
        order.verify(entityManager).lock(story.getEntry(), LockModeType.PESSIMISTIC_WRITE);
        order.verify(storyRepository).findByEntryId(story.getNodeId(), story.getEntry().getId());
        order.verify(entityManager).refresh(story);
        order.verify(entityManager).refresh(other);
    }

    private Story postingStory(String feedName, long publishedAt) {
        UUID nodeId = UUID.randomUUID();
        Posting posting = new Posting();
        posting.setId(UUID.randomUUID());
        posting.setNodeId(nodeId);
        posting.setOwnerName("alice");
        posting.setEditedAt(Util.toTimestamp(1000L));
        EntryRevision revision = new EntryRevision();
        revision.setId(UUID.randomUUID());
        revision.setBodyPreview("{}");
        revision.setBody("{}");
        posting.setCurrentRevision(revision);
        Story story = new Story(UUID.randomUUID(), nodeId, StoryType.POSTING_ADDED);
        story.setEntry(posting);
        story.setFeedName(feedName);
        story.setPublishedAt(Util.toTimestamp(publishedAt));
        story.setMoment(42L);
        when(requestContext.nodeId()).thenReturn(nodeId);
        when(requestContext.isPrincipal(any(), any())).thenReturn(true);
        when(storyRepository.findByNodeIdAndId(nodeId, story.getId())).thenReturn(Optional.of(story));
        return story;
    }

    @ParameterizedTest
    @EnumSource(EntryType.class)
    void deleteCapturesEntryDataBeforeSessionCloses(EntryType entryType) {
        doAnswer(invocation -> {
            sessionOpen = false;
            return null;
        }).when(platformTransactionManager).commit(any());
        UUID nodeId = UUID.randomUUID();
        Posting posting = new Posting();
        posting.setId(UUID.randomUUID());
        Entry entry = spy(entryType == EntryType.POSTING ? posting : new Comment());
        if (entryType == EntryType.COMMENT) {
            entry.setId(UUID.randomUUID());
            entry.setParent(posting);
        }
        entry.setOwnerName("alice");
        entry.setReceiverName("original");
        entry.setReceiverEntryId("remote-posting");
        doAnswer(invocation -> {
            if (!sessionOpen) {
                throw new LazyInitializationException("Entry proxy has no session");
            }
            return invocation.callRealMethod();
        }).when(entry).getEntryType();
        Story story = new Story();
        story.setId(UUID.randomUUID());
        story.setStoryType(StoryType.POSTING_ADDED);
        story.setFeedName(Feed.TIMELINE);
        story.setMoment(42L);
        story.setEntry(entry);
        when(requestContext.nodeId()).thenReturn(nodeId);
        when(storyRepository.findByNodeIdAndId(nodeId, story.getId())).thenReturn(Optional.of(story));

        assertSame(Result.OK, storyController.delete(story.getId().toString()));

        assertFalse(sessionOpen);
        verify(storyRepository).delete(story);
        ArgumentCaptor<StoryDeletedLiberin> captor = ArgumentCaptor.forClass(StoryDeletedLiberin.class);
        verify(requestContext).send(captor.capture());
        StoryDeletedLiberin deleted = captor.getValue();
        assertEquals(story.getId(), deleted.getId());
        assertEquals(StoryType.POSTING_ADDED, deleted.getStoryType());
        assertEquals(Feed.TIMELINE, deleted.getFeedName());
        assertEquals(42, deleted.getMoment());
        assertEquals(posting.getId(), deleted.getPostingId());
        assertEquals(entryType == EntryType.COMMENT ? entry.getId() : null, deleted.getCommentId());
        assertEquals("alice", deleted.getPostingOwnerName());
        assertEquals("original", deleted.getReceiverName());
        assertEquals("remote-posting", deleted.getReceiverPostingId());
        assertTrue(deleted.getViewFilter().includes(false, null, false, new String[0]));
    }

    @ParameterizedTest
    @CsvSource({
        "timeline, 1000, 2000, true",
        "timeline, 2000, 1000, false",
        "timeline, 1000, 1000, false",
        "timeline, 1000, , true",
        "news, 500, 1000, false",
        "news, 500, , false"
    })
    void deleteNotifiesPostingOnlyWhenEarliestPublicationChanges(
        String feedName,
        long publishedAt,
        Long otherPublishedAt,
        boolean notify
    ) {
        Story story = storyForDeletion(feedName, publishedAt);
        List<Story> stories = new ArrayList<>(List.of(story));
        if (otherPublishedAt != null) {
            Story other = new Story(UUID.randomUUID(), story.getNodeId(), StoryType.POSTING_ADDED);
            other.setEntry(story.getEntry());
            other.setFeedName(Feed.TIMELINE);
            other.setPublishedAt(Util.toTimestamp(otherPublishedAt));
            stories.add(other);
        }
        if (Feed.TIMELINE.equals(feedName)) {
            when(storyRepository.findByEntryId(story.getNodeId(), story.getEntry().getId())).thenReturn(stories);
        }

        assertSame(Result.OK, storyController.delete(story.getId().toString()));

        verify(storyRepository).delete(story);
        verify(requestContext).send(any(StoryDeletedLiberin.class));
        var captor = ArgumentCaptor.forClass(PostingPublishedAtUpdatedLiberin.class);
        verify(universalContext, times(notify ? 1 : 0)).send(captor.capture());
        if (notify) {
            assertEquals(story.getEntry().getId(), captor.getValue().getPostingId());
        }
    }

    @Test
    void deleteSeesPublicationCommittedWhileWaitingForPostingLock() {
        Story story = storyForDeletion(Feed.TIMELINE, 1000L);
        Story other = new Story(UUID.randomUUID(), story.getNodeId(), StoryType.POSTING_ADDED);
        other.setEntry(story.getEntry());
        other.setFeedName(Feed.TIMELINE);
        other.setPublishedAt(Util.toTimestamp(1000L));
        doAnswer(invocation -> {
            other.setPublishedAt(Util.toTimestamp(2000L));
            return null;
        }).when(entityManager).lock(story.getEntry(), LockModeType.PESSIMISTIC_WRITE);
        when(storyRepository.findByEntryId(story.getNodeId(), story.getEntry().getId()))
            .thenReturn(List.of(story, other));

        storyController.delete(story.getId().toString());

        verify(universalContext).send(any(PostingPublishedAtUpdatedLiberin.class));
    }

    @Test
    void deleteRefreshesPublicationDatesLoadedBeforeThePostingLock() {
        Story story = storyForDeletion(Feed.TIMELINE, 1000L);
        Story other = new Story(UUID.randomUUID(), story.getNodeId(), StoryType.POSTING_ADDED);
        other.setEntry(story.getEntry());
        other.setFeedName(Feed.TIMELINE);
        other.setPublishedAt(Util.toTimestamp(1000L));
        doNothing().when(entityManager).refresh(story);
        doAnswer(invocation -> {
            other.setPublishedAt(Util.toTimestamp(2000L));
            return null;
        }).when(entityManager).refresh(other);
        when(storyRepository.findByEntryId(story.getNodeId(), story.getEntry().getId()))
            .thenReturn(List.of(story, other));

        storyController.delete(story.getId().toString());

        verify(universalContext).send(any(PostingPublishedAtUpdatedLiberin.class));
        var order = org.mockito.Mockito.inOrder(entityManager, storyRepository);
        order.verify(entityManager, times(2)).lock(story.getEntry(), LockModeType.PESSIMISTIC_WRITE);
        order.verify(storyRepository).findByEntryId(story.getNodeId(), story.getEntry().getId());
        order.verify(entityManager).refresh(story);
        order.verify(entityManager).refresh(other);
        order.verify(storyRepository).delete(story);
    }

    private Story storyForDeletion(String feedName, long publishedAt) {
        UUID nodeId = UUID.randomUUID();
        Posting posting = new Posting();
        posting.setId(UUID.randomUUID());
        posting.setNodeId(nodeId);
        Story story = new Story(UUID.randomUUID(), nodeId, StoryType.POSTING_ADDED);
        story.setEntry(posting);
        story.setFeedName(feedName);
        story.setPublishedAt(Util.toTimestamp(publishedAt));
        story.setMoment(42L);
        when(requestContext.nodeId()).thenReturn(nodeId);
        when(storyRepository.findByNodeIdAndId(nodeId, story.getId())).thenReturn(Optional.of(story));
        return story;
    }

    private void useRealStoryOperations() {
        StoryOperations operations = new StoryOperations();
        ReflectionTestUtils.setField(operations, "storyRepository", storyRepository);
        ReflectionTestUtils.setField(operations, "postingRepository", postingRepository);
        ReflectionTestUtils.setField(operations, "postingOperations", postingOperations);
        ReflectionTestUtils.setField(operations, "universalContext", universalContext);
        ReflectionTestUtils.setField(operations, "entityManager", entityManager);
        ReflectionTestUtils.setField(storyController, "storyOperations", operations);
    }

    @ParameterizedTest
    @MethodSource("externalSourceUpdates")
    void putUpdatesExternalSourcesWithoutChangingMoment(List<String> sources, List<String> expected) {
        UUID nodeId = UUID.randomUUID();
        Story story = new Story(UUID.randomUUID(), nodeId, StoryType.SUBSCRIBER_ADDED);
        story.setFeedName(Feed.INSTANT);
        story.setMoment(42L);
        StoryAttributes initial = new StoryAttributes();
        initial.setExternalSourceUri(List.of("https://old.example/story"));
        StoryAttributesUtil.toStory(initial, story);
        when(requestContext.nodeId()).thenReturn(nodeId);
        when(storyRepository.findByNodeIdAndId(nodeId, story.getId())).thenReturn(Optional.of(story));
        StoryAttributes attributes = new StoryAttributes();
        when(requestContext.isPrincipal(Principal.ADMIN, Scope.VIEW_FEEDS)).thenReturn(false);
        when(requestContext.isPrincipal(Principal.ADMIN, Scope.VIEW_CONTENT)).thenReturn(true);
        attributes.setExternalSourceUri(sources);
        attributes.setViewed(true);

        StoryInfo info = storyController.put(story.getId().toString(), attributes);

        assertEquals(expected, info.getExternalSourceUri());
        assertEquals(
            expected,
            StoryInfoUtil.build(story, AccessCheckers.ADMIN, ignored -> null, null).getExternalSourceUri()
        );
        assertEquals(42L, story.getMoment());
        assertTrue(story.isViewed());
    }

    private static Stream<Arguments> externalSourceUpdates() {
        return Stream.of(
            Arguments.of(
                List.of("https://one.example/story", "https://two.example/story"),
                List.of("https://one.example/story", "https://two.example/story")
            ),
            Arguments.of(null, List.of("https://old.example/story")),
            Arguments.of(List.of(), null)
        );
    }

}
