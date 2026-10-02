package org.moera.node.rest;

import java.util.Optional;
import java.util.UUID;

import org.hibernate.LazyInitializationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.moera.lib.node.types.Result;
import org.moera.lib.node.types.StoryType;
import org.moera.node.data.Comment;
import org.moera.node.data.Entry;
import org.moera.node.data.EntryType;
import org.moera.node.data.Feed;
import org.moera.node.data.Posting;
import org.moera.node.data.Story;
import org.moera.node.data.StoryRepository;
import org.moera.node.global.RequestContext;
import org.moera.node.global.UniversalContext;
import org.moera.node.liberin.model.StoryDeletedLiberin;
import org.moera.node.util.Transaction;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StoryControllerTest {

    @Mock
    private RequestContext requestContext;

    @Mock
    private StoryRepository storyRepository;

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
        ReflectionTestUtils.setField(storyController, "tx", tx);
        ReflectionTestUtils.setField(tx, "txManager", platformTransactionManager);
        ReflectionTestUtils.setField(tx, "universalContext", universalContext);
        when(platformTransactionManager.getTransaction(any())).thenAnswer(invocation -> {
            sessionOpen = true;
            return new SimpleTransactionStatus();
        });
        doAnswer(invocation -> {
            sessionOpen = false;
            return null;
        }).when(platformTransactionManager).commit(any());
    }

    @ParameterizedTest
    @EnumSource(EntryType.class)
    void deleteCapturesEntryDataBeforeSessionCloses(EntryType entryType) {
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

}
