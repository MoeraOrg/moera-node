package org.moera.node.liberin.receptor;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.moera.lib.node.types.BodyFormat;
import org.moera.lib.node.types.PushContent;
import org.moera.lib.node.types.StoryType;
import org.moera.lib.node.types.notifications.Notification;
import org.moera.lib.node.types.principal.Principal;
import org.moera.node.data.EntryRevision;
import org.moera.node.data.Favor;
import org.moera.node.data.FavorRepository;
import org.moera.node.data.FavorType;
import org.moera.node.data.Feed;
import org.moera.node.data.Posting;
import org.moera.node.data.Story;
import org.moera.node.global.UniversalContext;
import org.moera.node.liberin.Liberin;
import org.moera.node.liberin.model.StoryAddedLiberin;
import org.moera.node.liberin.model.StoryDeletedLiberin;
import org.moera.node.model.event.Event;
import org.moera.node.notification.send.Direction;
import org.moera.node.operations.FavorOperations;
import org.moera.node.operations.StoryOperations;
import org.moera.node.option.Options;
import org.moera.node.util.ExtendedDuration;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StoryFavorsTest {

    @Mock
    private UniversalContext universalContext;

    @Mock
    private StoryOperations storyOperations;

    @Mock
    private FavorRepository favorRepository;

    @Spy
    private FavorOperations favorOperations = new FavorOperations();

    @InjectMocks
    private TestReceptor storyReceptor;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(favorOperations, "favorRepository", favorRepository);
        Options options = mock(Options.class);
        when(options.getDuration("posting.published.notification.age")).thenReturn(ExtendedDuration.NEVER);
        when(universalContext.getOptions()).thenReturn(options);
    }

    @ParameterizedTest
    @MethodSource("visibility")
    void publicationAndUnpublicationFavorAuthorRegardlessOfVisibility(Principal view) {
        Story story = story(true);
        story.getEntry().setViewPrincipal(view);

        addAndDelete(story, false);

        assertPublicationFavors(story);
    }

    private static Stream<Principal> visibility() {
        return Stream.of(Principal.PUBLIC, Principal.PRIVATE, Principal.ADMIN, Principal.ofNode("someone-else"));
    }

    @Test
    void originalPostingPublicationAlsoFavorsItsAuthor() {
        Story story = story(false);

        addAndDelete(story, false);

        assertPublicationFavors(story);
    }

    @Test
    void postingDeletionRecordsUnpublicationFromCapturedOwner() {
        Story story = story(true);

        addAndDelete(story, true);

        assertPublicationFavors(story);
    }

    @ParameterizedTest
    @ValueSource(strings = {Feed.NEWS, Feed.EXPLORE, Feed.INSTANT})
    void adminFeedsDoNotRecordPublicationFavors(String feedName) {
        Story story = story(true);
        story.setFeedName(feedName);

        addAndDelete(story, false);

        verifyNoInteractions(favorRepository);
    }

    @Test
    void otherStoryTypesDoNotRecordPublicationFavors() {
        Story story = story(true);
        story.setStoryType(StoryType.POSTING_SHARED);

        addAndDelete(story, false);

        verifyNoInteractions(favorRepository);
    }

    private void addAndDelete(Story story, boolean postingDeleted) {
        StoryAddedLiberin added = new StoryAddedLiberin(story);
        added.setNodeId(story.getNodeId());
        storyReceptor.added(added);
        StoryDeletedLiberin deleted = new StoryDeletedLiberin(story, postingDeleted);
        deleted.setNodeId(story.getNodeId());
        story.setEntry(null);
        storyReceptor.deleted(deleted);
    }

    private void assertPublicationFavors(Story story) {
        ArgumentCaptor<Favor> captor = ArgumentCaptor.forClass(Favor.class);
        verify(favorRepository, times(2)).save(captor.capture());
        List<Favor> favors = captor.getAllValues();
        assertEquals(List.of(FavorType.PUBLICATION, FavorType.UNPUBLICATION), favors.stream()
            .map(Favor::getFavorType).toList());
        for (Favor favor : favors) {
            assertEquals(story.getNodeId(), favor.getNodeId());
            assertEquals("author", favor.getNodeName());
        }
    }

    private static Story story(boolean picked) {
        Posting posting = new Posting();
        posting.setId(UUID.randomUUID());
        posting.setOwnerName("author");
        EntryRevision revision = new EntryRevision();
        revision.setId(UUID.randomUUID());
        revision.setEntry(posting);
        revision.setBodyPreview("{\"text\":\"post\"}");
        revision.setBody("{\"text\":\"post\"}");
        revision.setBodyFormat(BodyFormat.MESSAGE.getValue());
        posting.setCurrentRevision(revision);
        if (picked) {
            posting.setReceiverName("hosting-node");
            posting.setReceiverEntryId("remote-posting");
        }
        Story story = new Story(UUID.randomUUID(), UUID.randomUUID(), StoryType.POSTING_ADDED);
        story.setFeedName(Feed.TIMELINE);
        story.setMoment(100L);
        story.setEntry(posting);
        return story;
    }

    private static class TestReceptor extends StoryReceptor {

        @Override
        protected void send(Direction direction, Notification notification) {
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
