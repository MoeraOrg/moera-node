package org.moera.node.liberin.receptor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.moera.lib.node.types.PushContent;
import org.moera.lib.node.types.StoryType;
import org.moera.lib.node.types.notifications.Notification;
import org.moera.lib.node.types.notifications.PostingSharedNotification;
import org.moera.lib.node.types.notifications.PostingUnsharedNotification;
import org.moera.lib.node.types.principal.Principal;
import org.moera.node.data.Feed;
import org.moera.node.data.Posting;
import org.moera.node.data.Story;
import org.moera.node.global.UniversalContext;
import org.moera.node.liberin.Liberin;
import org.moera.node.liberin.model.StoryAddedLiberin;
import org.moera.node.liberin.model.StoryDeletedLiberin;
import org.moera.node.model.event.Event;
import org.moera.node.notification.send.Direction;
import org.moera.node.notification.send.Directions;
import org.moera.node.operations.FavorOperations;
import org.moera.node.operations.StoryOperations;
import org.moera.node.option.Options;
import org.moera.node.util.ExtendedDuration;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StorySharingNotificationsTest {

    @ParameterizedTest
    @MethodSource("entryVisibility")
    void sharingNotificationsRespectEntryVisibility(Principal view, boolean subscribed, boolean permitted) {
        TestReceptor receptor = receptor();
        Story story = story(Feed.TIMELINE, true);
        story.getEntry().setViewPrincipal(view);

        receptor.added(new StoryAddedLiberin(story));
        StoryDeletedLiberin deletion = new StoryDeletedLiberin(story);
        story.getEntry().setViewPrincipal(Principal.PUBLIC);
        receptor.deleted(deletion);

        int sharingNotifications = 0;
        for (int i = 0; i < receptor.notifications.size(); i++) {
            Notification notification = receptor.notifications.get(i);
            if (notification instanceof PostingSharedNotification || notification instanceof PostingUnsharedNotification) {
                sharingNotifications++;
                assertEquals(
                    permitted, receptor.directions.get(i).isPermitted(false, "owner", subscribed, new String[0])
                );
            }
        }
        assertEquals(2, sharingNotifications);
    }

    private static Stream<Arguments> entryVisibility() {
        return Stream.of(
            Arguments.of(Principal.PUBLIC, false, true),
            Arguments.of(Principal.PRIVATE, false, true),
            Arguments.of(Principal.ADMIN, false, false),
            Arguments.of(Principal.OWNER, false, true),
            Arguments.of(Principal.ofNode("other"), false, false),
            Arguments.of(Principal.SUBSCRIBED, false, false),
            Arguments.of(Principal.SUBSCRIBED, true, true)
        );
    }

    @Test
    void timelineRepublishOmitsRevisionAndKeepsStoryIdWhileSendingTheCurrentMoment() {
        TestReceptor receptor = receptor();
        Story story = story(Feed.TIMELINE, true);
        receptor.added(new StoryAddedLiberin(story));
        PostingSharedNotification shared = receptor.notifications.stream()
            .filter(PostingSharedNotification.class::isInstance).map(PostingSharedNotification.class::cast)
            .findFirst().orElseThrow();
        story.getEntry().setCurrentReceiverRevisionId("updated-revision");
        story.setMoment(200L);
        StoryDeletedLiberin deletion = new StoryDeletedLiberin(story);
        receptor.deleted(deletion);
        PostingUnsharedNotification unshared = receptor.notifications.stream()
            .filter(PostingUnsharedNotification.class::isInstance).map(PostingUnsharedNotification.class::cast)
            .findFirst().orElseThrow();

        assertEquals("original", shared.getPostingId());
        assertNull(shared.getPostingRevisionId());
        assertEquals(100L, shared.getRemoteStoryMoment());
        assertEquals(shared.getPostingId(), unshared.getPostingId());
        assertNull(unshared.getPostingRevisionId());
        assertEquals(shared.getRemoteFeedName(), unshared.getRemoteFeedName());
        assertEquals(shared.getRemoteStoryId(), unshared.getRemoteStoryId());
        assertEquals(200L, unshared.getRemoteStoryMoment());
        assertEquals(shared.getRemotePostingId(), unshared.getRemotePostingId());
        int index = receptor.notifications.indexOf(unshared);
        assertEquals(Directions.single(deletion.getNodeId(), "owner"), receptor.directions.get(index));
    }

    @Test
    void newsAndOriginalPostingsDoNotSendSharingNotifications() {
        TestReceptor receptor = receptor();
        receptor.added(new StoryAddedLiberin(story(Feed.NEWS, true)));
        receptor.added(new StoryAddedLiberin(story(Feed.TIMELINE, false)));
        receptor.deleted(new StoryDeletedLiberin(story(Feed.NEWS, true)));
        assertTrue(receptor.notifications.stream().noneMatch(notification ->
            notification instanceof PostingSharedNotification || notification instanceof PostingUnsharedNotification
        ));
    }

    private static Story story(String feed, boolean picked) {
        Posting posting = new Posting();
        posting.setId(UUID.randomUUID());
        posting.setOwnerName("owner");
        if (picked) {
            posting.setReceiverName("owner");
            posting.setReceiverEntryId("original");
            posting.setCurrentReceiverRevisionId("original-revision");
        }
        Story story = new Story(UUID.randomUUID(), UUID.randomUUID(), StoryType.POSTING_ADDED);
        story.setEntry(posting);
        story.setFeedName(feed);
        story.setMoment(100L);
        return story;
    }

    private static TestReceptor receptor() {
        TestReceptor receptor = new TestReceptor();
        UniversalContext context = mock(UniversalContext.class);
        Options options = mock(Options.class);
        when(options.getDuration("posting.published.notification.age")).thenReturn(ExtendedDuration.NEVER);
        when(context.getOptions()).thenReturn(options);
        ReflectionTestUtils.setField(receptor, "universalContext", context);
        ReflectionTestUtils.setField(receptor, "storyOperations", mock(StoryOperations.class));
        ReflectionTestUtils.setField(receptor, "favorOperations", mock(FavorOperations.class));
        return receptor;
    }

    private static class TestReceptor extends StoryReceptor {

        private final List<Notification> notifications = new ArrayList<>();
        private final List<Direction> directions = new ArrayList<>();

        @Override
        protected void send(Direction direction, Notification notification) {
            notifications.add(notification);
            directions.add(direction);
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

}
