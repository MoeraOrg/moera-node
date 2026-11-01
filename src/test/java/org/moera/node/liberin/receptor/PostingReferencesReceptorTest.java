package org.moera.node.liberin.receptor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.moera.lib.node.types.PostingInfo;
import org.moera.lib.node.types.SheriffMark;
import org.moera.lib.node.types.SharingType;
import org.moera.lib.node.types.body.Body;
import org.moera.lib.node.types.notifications.Notification;
import org.moera.lib.node.types.notifications.PostingSharedNotification;
import org.moera.lib.node.types.notifications.PostingUnsharedNotification;
import org.moera.lib.node.types.principal.Principal;
import org.moera.node.data.EntryRevision;
import org.moera.node.data.Favor;
import org.moera.node.data.FavorRepository;
import org.moera.node.data.LinkedEntry;
import org.moera.node.data.LinkedEntryRepository;
import org.moera.node.data.Posting;
import org.moera.node.data.StoryRepository;
import org.moera.node.global.UniversalContext;
import org.moera.node.liberin.Liberin;
import org.moera.node.model.event.Event;
import org.moera.node.option.Options;
import org.moera.node.util.ExtendedDuration;
import org.moera.node.userlist.SheriffUserListOperations;
import org.moera.node.friends.FriendCache;
import org.moera.node.friends.SubscribedCache;
import org.moera.node.liberin.model.PostingAddedLiberin;
import org.moera.node.liberin.model.PostingDeletedLiberin;
import org.moera.node.liberin.model.PostingRestoredLiberin;
import org.moera.node.liberin.model.PostingUpdatedLiberin;
import org.moera.node.notification.send.Direction;
import org.moera.node.notification.send.Directions;
import org.moera.node.operations.FavorOperations;
import org.moera.node.text.PostingReferencesExtractor;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PostingReferencesReceptorTest {

    private final TestReceptor receptor = new TestReceptor();
    private final Posting posting = new Posting();
    private final List<Favor> favors = new ArrayList<>();
    private final Map<UUID, List<LinkedEntry>> references = new HashMap<>();
    private final SubscribedCache subscribedCache = mock(SubscribedCache.class);
    private final SheriffUserListOperations sheriffUserListOperations = mock(SheriffUserListOperations.class);

    @BeforeEach
    void setUp() {
        FavorRepository repository = mock(FavorRepository.class);
        doAnswer(invocation -> {
            Favor favor = invocation.getArgument(0);
            favors.add(favor);
            return favor;
        }).when(repository).save(any(Favor.class));
        FavorOperations operations = new FavorOperations();
        ReflectionTestUtils.setField(operations, "favorRepository", repository);
        ReflectionTestUtils.setField(receptor, "favorOperations", operations);
        ReflectionTestUtils.setField(receptor, "subscribedCache", subscribedCache);
        ReflectionTestUtils.setField(receptor, "friendCache", mock(FriendCache.class));
        ReflectionTestUtils.setField(receptor, "sheriffUserListOperations", sheriffUserListOperations);
        UniversalContext context = mock(UniversalContext.class);
        Options options = mock(Options.class);
        when(context.getOptions()).thenReturn(options);
        when(options.getDuration("posting.revealed.notification.age")).thenReturn(ExtendedDuration.NEVER);
        ReflectionTestUtils.setField(receptor, "universalContext", context);
        ReflectionTestUtils.setField(receptor, "storyRepository", mock(StoryRepository.class));
        LinkedEntryRepository linkedEntryRepository = mock(LinkedEntryRepository.class);
        when(linkedEntryRepository.findByRevisionId(any(), any())).thenAnswer(invocation ->
            references.getOrDefault(invocation.getArgument(1), List.of())
        );
        ReflectionTestUtils.setField(receptor, "linkedEntryRepository", linkedEntryRepository);
        posting.setId(UUID.randomUUID());
        posting.setNodeId(UUID.randomUUID());
        posting.setOwnerName("author_0");
        posting.setViewPrincipal(Principal.PUBLIC);
        posting.setCurrentRevision(revision("<a href=\"https://moera.page/@alice/~/post/target\">a post</a>"));
    }

    @Test
    void addedReferenceSendsOneNotificationWithTheSourcePostingAndFavorsItsAuthor() {
        posting.setCurrentRevision(revision("""
            <a href="https://moera.page/@alice/~/post/target">a post</a>
            <a href="https://web.moera.org/@alice_0/alice.example/post/target">the same post</a>
            """));
        posting.getCurrentRevision().setHeading("A post with a reference");
        posting.setOwnerFullName("Author");
        posting.setOwnerSourceUri("https://author.example/");
        posting.setOwnerGender("female");
        SheriffMark mark = new SheriffMark();
        mark.setSheriffName("sheriff_0");
        doAnswer(invocation -> {
            PostingInfo info = invocation.getArgument(0);
            info.setSheriffs(List.of("sheriff_0"));
            info.setSheriffMarks(List.of(mark));
            return null;
        }).when(sheriffUserListOperations).fillSheriffListMarks(any(PostingInfo.class));

        receptor.added(new PostingAddedLiberin(posting));

        assertEquals(1, receptor.notifications.size());
        var notification = assertInstanceOf(PostingSharedNotification.class, receptor.notifications.getFirst());
        assertEquals(SharingType.REFER, notification.getSharingType());
        assertEquals("target", notification.getPostingId());
        assertEquals(posting.getId().toString(), notification.getRemotePostingId());
        var json = new ObjectMapper().valueToTree(notification);
        assertEquals("A post with a reference", json.path("remotePostingHeading").asString(null));
        assertEquals("author_0", json.path("remotePostingOwnerName").asString(null));
        assertEquals("Author", json.path("remotePostingOwnerFullName").asString(null));
        assertEquals("https://author.example/", json.path("remotePostingOwnerSourceUri").asString(null));
        assertEquals("female", json.path("remotePostingOwnerGender").asString(null));
        assertEquals("sheriff_0", json.path("remotePostingSheriffs").path(0).asString(null));
        assertEquals("sheriff_0", json.path("remotePostingSheriffMarks").path(0).path("sheriffName").asString(null));
        assertFalse(json.has("remotePosting"));
        assertNull(notification.getPostingRevisionId());
        assertNull(notification.getRemoteFeedName());
        assertNull(notification.getRemoteStoryId());
        assertNull(notification.getRemoteStoryMoment());
        assertEquals(List.of(Directions.single(posting.getNodeId(), "alice_0")), receptor.directions);
        assertFavors("REFERENCE");
    }

    @Test
    void editingOnlyTheUrlSpellingDoesNotRepeatNotificationOrFavor() {
        EntryRevision previous = posting.getCurrentRevision();
        posting.setCurrentRevision(revision("<a href=\"https://web.moera.org/@alice_0/alice.example/post/target\">post</a>"));

        receptor.updated(new PostingUpdatedLiberin(posting, previous, Principal.PUBLIC));

        assertTrue(receptor.notifications.isEmpty());
        assertTrue(favors.isEmpty());
    }

    @Test
    void editingTheTargetUnsharesTheOldPostingAndSharesTheNewOne() {
        EntryRevision previous = posting.getCurrentRevision();
        posting.setCurrentRevision(revision("<a href=\"https://moera.page/@alice/~/post/another\">another post</a>"));

        receptor.updated(new PostingUpdatedLiberin(posting, previous, Principal.PUBLIC));

        assertEquals(2, receptor.notifications.size());
        var shared = receptor.notifications.stream().filter(PostingSharedNotification.class::isInstance)
            .map(PostingSharedNotification.class::cast).findFirst().orElseThrow();
        var unshared = receptor.notifications.stream().filter(PostingUnsharedNotification.class::isInstance)
            .map(PostingUnsharedNotification.class::cast).findFirst().orElseThrow();
        assertEquals("another", shared.getPostingId());
        assertEquals("target", unshared.getPostingId());
        assertEquals(SharingType.REFER, unshared.getSharingType());
        assertEquals(posting.getId().toString(), unshared.getRemotePostingId());
        assertNull(unshared.getPostingRevisionId());
        assertNull(unshared.getRemoteStoryId());
        assertFavors("REFERENCE", "UNREFERENCE");
    }

    @Test
    void deletionAndRestorationReverseAndRestoreTheReference() {
        receptor.deleted(new PostingDeletedLiberin(posting, posting.getCurrentRevision()));
        receptor.restored(new PostingRestoredLiberin(posting));

        assertEquals(2, receptor.notifications.size());
        assertInstanceOf(PostingUnsharedNotification.class, receptor.notifications.getFirst());
        assertInstanceOf(PostingSharedNotification.class, receptor.notifications.getLast());
        assertFavors("UNREFERENCE", "REFERENCE");
    }

    @Test
    void privateReferencesStillFavorTheAuthorWithoutNotifyingThem() {
        posting.setViewPrincipal(Principal.ADMIN);

        receptor.added(new PostingAddedLiberin(posting));

        assertTrue(receptor.notifications.isEmpty());
        assertFavors("REFERENCE");
    }

    @Test
    void visibilityChangesUpdateNotificationsWithoutRepeatingFavors() {
        EntryRevision previous = posting.getCurrentRevision();
        posting.setViewPrincipal(Principal.ADMIN);
        receptor.updated(new PostingUpdatedLiberin(posting, previous, Principal.PUBLIC));
        posting.setViewPrincipal(Principal.PUBLIC);
        receptor.updated(new PostingUpdatedLiberin(posting, previous, Principal.ADMIN));

        assertEquals(2, receptor.notifications.size());
        assertInstanceOf(PostingUnsharedNotification.class, receptor.notifications.getFirst());
        assertInstanceOf(PostingSharedNotification.class, receptor.notifications.getLast());
        assertTrue(favors.isEmpty());
    }

    @Test
    void subscriptionVisibilityUsesTheReferencedNodesAccess() {
        posting.setViewPrincipal(Principal.SUBSCRIBED);
        when(subscribedCache.isSubscribed("alice_0")).thenReturn(true);

        receptor.added(new PostingAddedLiberin(posting));

        assertEquals(1, receptor.notifications.size());
    }

    @Test
    void unsignedPreviousRevisionHasNotAnnouncedItsReferences() {
        EntryRevision previous = posting.getCurrentRevision();
        previous.setSignature(null);

        receptor.updated(new PostingUpdatedLiberin(posting, previous, Principal.PUBLIC));

        assertEquals(1, receptor.notifications.size());
        assertInstanceOf(PostingSharedNotification.class, receptor.notifications.getFirst());
        assertFavors("REFERENCE");
    }

    @Test
    void pickedPostingsAndSelfReferencesDoNotCreateReferences() {
        posting.setReceiverName("source_0");
        receptor.added(new PostingAddedLiberin(posting));
        receptor.deleted(new PostingDeletedLiberin(posting, posting.getCurrentRevision()));
        receptor.updated(new PostingUpdatedLiberin(posting, posting.getCurrentRevision(), Principal.PUBLIC));
        receptor.restored(new PostingRestoredLiberin(posting));
        posting.setReceiverName(null);
        posting.setCurrentRevision(revision("<a href=\"https://moera.page/@author/~/post/own-post\">own post</a>"));
        receptor.added(new PostingAddedLiberin(posting));

        assertTrue(receptor.notifications.isEmpty());
        assertTrue(favors.isEmpty());
    }

    @Test
    void changingAnAnchorToPlainTextUnsharesTheReference() {
        EntryRevision previous = posting.getCurrentRevision();
        posting.setCurrentRevision(revision("<p>https://moera.page/@alice/~/post/target</p>"));

        receptor.updated(new PostingUpdatedLiberin(posting, previous, Principal.PUBLIC));

        assertEquals(1, receptor.notifications.size());
        var unshared = assertInstanceOf(PostingUnsharedNotification.class, receptor.notifications.getFirst());
        assertEquals("target", unshared.getPostingId());
        assertEquals(SharingType.REFER, unshared.getSharingType());
        assertFavors("UNREFERENCE");
    }

    private void assertFavors(String... types) {
        assertEquals(List.of(types), favors.stream().map(Favor::getFavorType).map(Enum::name).toList());
        for (Favor favor : favors) {
            assertEquals(posting.getNodeId(), favor.getNodeId());
            assertEquals("alice_0", favor.getNodeName());
            assertEquals(favor.getFavorType().name().equals("REFERENCE") ? 10f : -10f, favor.getValue());
            assertEquals(7 * 24, favor.getDecayHours());
        }
    }

    @Test
    void notificationsUseSavedReferencesWithoutExtractingTheBodyAgain() {
        posting.getCurrentRevision().setBody(Body.EMPTY);

        receptor.added(new PostingAddedLiberin(posting));

        assertEquals(1, receptor.notifications.size());
        var notification = assertInstanceOf(PostingSharedNotification.class, receptor.notifications.getFirst());
        assertEquals("target", notification.getPostingId());
        assertFavors("REFERENCE");
    }

    private EntryRevision revision(String text) {
        Body body = new Body();
        body.setText(text);
        EntryRevision revision = new EntryRevision();
        revision.setId(UUID.randomUUID());
        revision.setBody(body.getEncoded());
        revision.setBodyPreview(body.getEncoded());
        revision.setSignature(new byte[]{1});
        for (var reference : PostingReferencesExtractor.extract(body)) {
            LinkedEntry link = new LinkedEntry();
            link.setId(UUID.randomUUID());
            link.setReceiverNodeName(reference.nodeName());
            link.setReceiverEntryId(reference.postingId());
            revision.addLinkedEntry(link);
        }
        references.put(revision.getId(), List.copyOf(revision.getLinkedEntries()));
        return revision;
    }

    private static class TestReceptor extends PostingReceptor {

        private final List<Notification> notifications = new ArrayList<>();
        private final List<Direction> directions = new ArrayList<>();

        @Override
        protected void send(Liberin liberin, Event event) {
        }

        @Override
        protected void send(Direction direction, Notification notification) {
            if (notification instanceof PostingSharedNotification || notification instanceof PostingUnsharedNotification) {
                directions.add(direction);
                notifications.add(notification);
            }
        }

    }

}
