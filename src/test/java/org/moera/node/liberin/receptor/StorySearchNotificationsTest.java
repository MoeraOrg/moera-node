package org.moera.node.liberin.receptor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.moera.lib.node.types.PushContent;
import org.moera.lib.node.types.SearchContentUpdateType;
import org.moera.lib.node.types.StoryType;
import org.moera.lib.node.types.SubscriptionType;
import org.moera.lib.node.types.notifications.Notification;
import org.moera.lib.node.types.notifications.SearchContentUpdatedNotification;
import org.moera.lib.node.types.principal.Principal;
import org.moera.node.data.Comment;
import org.moera.node.data.EntryRevision;
import org.moera.node.data.Feed;
import org.moera.node.data.Posting;
import org.moera.node.data.PostingRepository;
import org.moera.node.data.Story;
import org.moera.node.data.StoryRepository;
import org.moera.node.global.UniversalContext;
import org.moera.node.liberin.Liberin;
import org.moera.node.liberin.model.PostingDeletedLiberin;
import org.moera.node.liberin.model.PostingUpdatedLiberin;
import org.moera.node.liberin.model.StoryDeletedLiberin;
import org.moera.node.media.MediaOperations;
import org.moera.node.model.event.Event;
import org.moera.node.notification.send.Direction;
import org.moera.node.operations.FavorOperations;
import org.moera.node.operations.PostingOperations;
import org.moera.node.operations.StoryOperations;
import org.moera.node.option.Options;
import org.moera.node.util.ExtendedDuration;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StorySearchNotificationsTest {

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void deletionIdentifiesStoryAndOriginalPosting(boolean picked) {
        TestReceptor receptor = receptor();
        Story story = story(picked);
        UUID postingId = story.getEntry().getId();
        StoryDeletedLiberin deletion = deletion(story);
        story.setEntry(null);

        receptor.deleted(deletion);

        assertEquals(1, receptor.notifications.size());
        SearchContentUpdatedNotification notification = receptor.notifications.getFirst();
        assertEquals(SearchContentUpdateType.POSTING_DELETE, notification.getUpdateType());
        assertEquals(picked ? "original" : "local", notification.getPostingUpdate().getNodeName());
        assertEquals(
            picked ? "remote-posting" : postingId.toString(), notification.getPostingUpdate().getPostingId()
        );
        assertEquals(story.getId().toString(), notification.getPostingUpdate().getStoryId());
        assertEquals(Feed.TIMELINE, notification.getPostingUpdate().getFeedName());
        Direction direction = receptor.directions.getFirst();
        assertEquals(story.getNodeId(), direction.getNodeId());
        assertEquals(SubscriptionType.SEARCH, ReflectionTestUtils.getField(direction, "subscriptionType"));
    }

    @ParameterizedTest
    @MethodSource("visibility")
    void deletionUsesVisibilityCapturedBeforeStoryWasRemoved(Principal view, boolean subscribed, boolean permitted) {
        TestReceptor receptor = receptor();
        Story story = story(true);
        story.getEntry().setViewPrincipal(view);
        StoryDeletedLiberin deletion = deletion(story);
        story.getEntry().setViewPrincipal(Principal.PUBLIC);

        receptor.deleted(deletion);

        assertEquals(1, receptor.notifications.size());
        assertEquals(permitted, receptor.directions.getFirst().isPermitted(false, "search", subscribed, new String[0]));
    }

    private static Stream<Arguments> visibility() {
        return Stream.of(
            Arguments.of(Principal.PUBLIC, false, true),
            Arguments.of(Principal.PRIVATE, false, false),
            Arguments.of(Principal.ADMIN, false, false),
            Arguments.of(Principal.SUBSCRIBED, false, false),
            Arguments.of(Principal.SUBSCRIBED, true, true)
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {Feed.NEWS, Feed.EXPLORE, Feed.INSTANT})
    void deletionFromAdminFeedDoesNotNotifySearch(String feed) {
        TestReceptor receptor = receptor();
        Story story = story(true);
        story.setFeedName(feed);

        receptor.deleted(deletion(story));

        assertTrue(receptor.notifications.isEmpty());
    }

    @Test
    void deletionWithoutPostingDoesNotNotifySearch() {
        TestReceptor receptor = receptor();
        Story story = story(false);
        story.setEntry(null);

        receptor.deleted(deletion(story));

        assertTrue(receptor.notifications.isEmpty());
    }

    @Test
    void deletionOfCommentStoryDoesNotNotifySearchAboutPostingDeletion() {
        TestReceptor receptor = receptor();
        Story story = story(false);
        Comment comment = new Comment();
        comment.setId(UUID.randomUUID());
        comment.setParent(story.getEntry());
        story.setStoryType(StoryType.COMMENT_ADDED);
        story.setEntry(comment);

        receptor.deleted(deletion(story));

        assertTrue(receptor.notifications.isEmpty());
    }

    @ParameterizedTest
    @MethodSource("postingDeletionModes")
    void deletingOrHidingPostingNotifiesSearchOnlyAboutPosting(boolean picked, boolean hide) {
        TestReceptor receptor = receptor();
        Story story = story(picked);
        Posting posting = (Posting) story.getEntry();
        posting.setCurrentRevision(new EntryRevision());
        UniversalContext universalContext = mock(UniversalContext.class);
        when(universalContext.nodeId()).thenReturn(story.getNodeId());
        when(universalContext.nodeName()).thenReturn("local");
        Options options = mock(Options.class);
        when(options.getBool("posting.picked.hide-on-delete")).thenReturn(hide);
        when(options.getDuration("posting.deleted.lifetime")).thenReturn(ExtendedDuration.NEVER);
        when(options.getDuration("posting.revealed.notification.age")).thenReturn(ExtendedDuration.NEVER);
        when(universalContext.getOptions()).thenReturn(options);
        StoryRepository storyRepository = mock(StoryRepository.class);
        List<Story> stories = new ArrayList<>(List.of(story));
        when(storyRepository.findByEntryId(story.getNodeId(), posting.getId()))
            .thenAnswer(invocation -> List.copyOf(stories));
        doAnswer(invocation -> {
            stories.remove(invocation.getArgument(0));
            return null;
        }).when(storyRepository).delete(any(Story.class));
        StoryOperations storyOperations = new StoryOperations();
        ReflectionTestUtils.setField(storyOperations, "universalContext", universalContext);
        ReflectionTestUtils.setField(storyOperations, "storyRepository", storyRepository);
        PostingRepository postingRepository = mock(PostingRepository.class);
        when(postingRepository.saveAndFlush(posting)).thenReturn(posting);
        PostingOperations postingOperations = new PostingOperations();
        ReflectionTestUtils.setField(postingOperations, "universalContext", universalContext);
        ReflectionTestUtils.setField(postingOperations, "storyOperations", storyOperations);
        ReflectionTestUtils.setField(postingOperations, "postingRepository", postingRepository);
        ReflectionTestUtils.setField(postingOperations, "mediaOperations", mock(MediaOperations.class));

        if (picked) {
            postingOperations.deletePickedPosting(posting);
        } else {
            postingOperations.deletePostings(List.of(posting));
        }

        ArgumentCaptor<Liberin> captor = ArgumentCaptor.forClass(Liberin.class);
        verify(universalContext, atLeastOnce()).send(captor.capture());
        TestPostingReceptor postingReceptor = new TestPostingReceptor(receptor);
        ReflectionTestUtils.setField(postingReceptor, "universalContext", universalContext);
        ReflectionTestUtils.setField(postingReceptor, "storyRepository", storyRepository);
        for (Liberin liberin : captor.getAllValues()) {
            liberin.setNodeId(story.getNodeId());
            if (liberin instanceof StoryDeletedLiberin deletion) {
                receptor.deleted(deletion);
            } else if (liberin instanceof PostingDeletedLiberin deletion) {
                postingReceptor.deleted(deletion);
            } else if (liberin instanceof PostingUpdatedLiberin update) {
                postingReceptor.updated(update);
            }
        }

        assertEquals(1, receptor.notifications.size());
        SearchContentUpdatedNotification notification = receptor.notifications.getFirst();
        assertEquals(SearchContentUpdateType.POSTING_DELETE, notification.getUpdateType());
        assertEquals(picked ? "original" : "local", notification.getPostingUpdate().getNodeName());
        assertEquals(
            picked ? "remote-posting" : posting.getId().toString(), notification.getPostingUpdate().getPostingId()
        );
        assertNull(notification.getPostingUpdate().getStoryId());
        assertEquals(hide ? List.of(story) : List.of(), stories);
    }

    private static Stream<Arguments> postingDeletionModes() {
        return Stream.of(
            Arguments.of(false, false),
            Arguments.of(true, false),
            Arguments.of(true, true)
        );
    }

    private static Story story(boolean picked) {
        Posting posting = new Posting();
        posting.setId(UUID.randomUUID());
        posting.setOwnerName("owner");
        if (picked) {
            posting.setReceiverName("original");
            posting.setReceiverEntryId("remote-posting");
        }
        Story story = new Story(UUID.randomUUID(), UUID.randomUUID(), StoryType.POSTING_ADDED);
        story.setEntry(posting);
        story.setFeedName(Feed.TIMELINE);
        story.setMoment(100L);
        return story;
    }

    private static StoryDeletedLiberin deletion(Story story) {
        StoryDeletedLiberin deletion = new StoryDeletedLiberin(story);
        deletion.setNodeId(story.getNodeId());
        return deletion;
    }

    private static TestReceptor receptor() {
        TestReceptor receptor = new TestReceptor();
        UniversalContext universalContext = mock(UniversalContext.class);
        when(universalContext.nodeName()).thenReturn("local");
        ReflectionTestUtils.setField(receptor, "universalContext", universalContext);
        ReflectionTestUtils.setField(receptor, "storyOperations", mock(StoryOperations.class));
        ReflectionTestUtils.setField(receptor, "favorOperations", mock(FavorOperations.class));
        return receptor;
    }

    private static class TestReceptor extends StoryReceptor {

        private final List<SearchContentUpdatedNotification> notifications = new ArrayList<>();
        private final List<Direction> directions = new ArrayList<>();

        @Override
        protected void send(Direction direction, Notification notification) {
            if (notification instanceof SearchContentUpdatedNotification searchNotification) {
                notifications.add(searchNotification);
                directions.add(direction);
            }
        }

        @Override
        protected void send(Liberin liberin, Event event) {
        }

        @Override
        protected void send(Liberin liberin) {
        }

        @Override
        protected void send(PushContent content) {
        }

    }

    private static class TestPostingReceptor extends PostingReceptor {

        private final TestReceptor storyReceptor;

        TestPostingReceptor(TestReceptor storyReceptor) {
            this.storyReceptor = storyReceptor;
        }

        @Override
        protected void send(Direction direction, Notification notification) {
            if (direction.isPermitted(false, "search", false, new String[0])) {
                storyReceptor.send(direction, notification);
            }
        }

        @Override
        protected void send(Liberin liberin, Event event) {
        }

    }

}
