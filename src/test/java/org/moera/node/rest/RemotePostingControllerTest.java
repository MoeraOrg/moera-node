package org.moera.node.rest;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.moera.lib.node.types.BlockedOperation;
import org.moera.lib.node.types.RemotePostingRepublishAttributes;
import org.moera.lib.node.types.PostingSourceText;
import org.moera.lib.node.types.Result;
import org.moera.lib.node.types.Scope;
import org.moera.lib.node.types.StoryAttributes;
import org.moera.lib.node.types.body.Body;
import org.moera.lib.node.types.principal.Principal;
import org.moera.lib.node.types.validate.ValidationFailure;
import org.moera.node.auth.AuthenticationException;
import org.moera.node.auth.UserBlockedException;
import org.moera.node.data.Pick;
import org.moera.node.global.RequestContext;
import org.moera.node.media.MediaOperations;
import org.moera.node.operations.BlockedUserOperations;
import org.moera.node.option.Options;
import org.moera.node.picker.PickerPool;
import org.moera.node.task.Jobs;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RemotePostingControllerTest {

    @Mock
    private RequestContext requestContext;

    @Mock
    private Options options;

    @Mock
    private BlockedUserOperations blockedUserOperations;

    @Mock
    private PickerPool pickerPool;

    @Mock
    private MediaOperations mediaOperations;

    @Mock
    private Jobs jobs;

    @InjectMocks
    private RemotePostingController remotePostingController;

    @Test
    void republishDownloadsPostingForEveryPublication() {
        when(requestContext.isAdmin(Scope.ADD_POST)).thenReturn(true);
        when(requestContext.isAdmin(Scope.UPDATE_FEEDS)).thenReturn(true);
        StoryAttributes timeline = publication("timeline", 1000L, true, true, false, true);
        StoryAttributes news = publication("news", 2000L, false, false, true, false);
        timeline.setExternalSourceUri(List.of("https://one.example/story", "https://two.example/story"));
        news.setExternalSourceUri(List.of("https://news.example/story"));
        RemotePostingRepublishAttributes attributes = attributes(List.of(timeline, news));
        attributes.setPostingView(Principal.PRIVATE);

        remotePostingController.republish("remote", "posting", attributes);

        ArgumentCaptor<Pick> captor = ArgumentCaptor.forClass(Pick.class);
        verify(pickerPool, times(2)).pick(captor.capture());
        assertPick(captor.getAllValues().get(0), "timeline", 1000L, true, true, false, true);
        assertPick(captor.getAllValues().get(1), "news", 2000L, false, false, true, false);
        Assertions.assertArrayEquals(
            new String[]{"https://one.example/story", "https://two.example/story"},
            captor.getAllValues().get(0).getExternalSourceUri()
        );
        Assertions.assertArrayEquals(
            new String[]{"https://news.example/story"},
            captor.getAllValues().get(1).getExternalSourceUri()
        );
    }

    @Test
    void republishRequiresPermissionToCreatePosting() {
        when(requestContext.getClientName(Scope.ADD_POST)).thenReturn("client");
        when(requestContext.getOptions()).thenReturn(options);
        when(options.getBool("posting.non-admin.allowed")).thenReturn(false);
        RemotePostingRepublishAttributes attributes = attributes(List.of());

        Assertions.assertThrows(
            AuthenticationException.class,
            () -> remotePostingController.republish("remote", "posting", attributes)
        );

        verifyNoInteractions(pickerPool);
    }

    @Test
    void republishRequiresClientPermissionToCreatePosting() {
        RemotePostingRepublishAttributes attributes = attributes(List.of());

        Assertions.assertThrows(
            AuthenticationException.class,
            () -> remotePostingController.republish("remote", "posting", attributes)
        );

        verifyNoInteractions(pickerPool);
    }

    @Test
    void republishAllowsClientWithPermissionWhenNonAdminPostingIsEnabled() {
        when(requestContext.getClientName(Scope.ADD_POST)).thenReturn("client");
        when(requestContext.getOptions()).thenReturn(options);
        when(options.getBool("posting.non-admin.allowed")).thenReturn(true);
        RemotePostingRepublishAttributes attributes = attributes(List.of());

        Result result = remotePostingController.republish("remote", "posting", attributes);

        Assertions.assertSame(Result.OK, result);
        verifyNoInteractions(pickerPool);
    }

    @Test
    void republishRequiresPermissionToUpdateFeeds() {
        when(requestContext.isAdmin(Scope.ADD_POST)).thenReturn(true);
        RemotePostingRepublishAttributes attributes = attributes(List.of(new StoryAttributes()));

        Assertions.assertThrows(
            AuthenticationException.class,
            () -> remotePostingController.republish("remote", "posting", attributes)
        );

        verifyNoInteractions(pickerPool);
    }

    @Test
    void republishIsRejectedWhenPostingIsBlocked() {
        when(requestContext.isAdmin(Scope.ADD_POST)).thenReturn(true);
        when(requestContext.isAdmin(Scope.UPDATE_FEEDS)).thenReturn(true);
        when(blockedUserOperations.isBlocked(BlockedOperation.POSTING)).thenReturn(true);
        RemotePostingRepublishAttributes attributes = attributes(List.of(new StoryAttributes()));

        Assertions.assertThrows(
            UserBlockedException.class,
            () -> remotePostingController.republish("remote", "posting", attributes)
        );

        verifyNoInteractions(pickerPool);
    }

    @Test
    void republishRequiresPublications() {
        RemotePostingRepublishAttributes attributes = new RemotePostingRepublishAttributes();
        attributes.setRemoteFeedName("remote-feed");

        ValidationFailure failure = Assertions.assertThrows(
            ValidationFailure.class,
            () -> remotePostingController.republish("remote", "posting", attributes)
        );

        Assertions.assertEquals("posting.publications.missing", failure.getErrorCode());
        verifyNoInteractions(pickerPool);
    }

    @Test
    void republishRequiresRemoteFeedName() {
        RemotePostingRepublishAttributes attributes = attributes(List.of());
        attributes.setRemoteFeedName("");

        ValidationFailure failure = Assertions.assertThrows(
            ValidationFailure.class,
            () -> remotePostingController.republish("remote", "posting", attributes)
        );

        Assertions.assertEquals("posting.remote-feed-name.blank", failure.getErrorCode());
        verifyNoInteractions(pickerPool);
    }

    @Test
    void republishRejectsNullPublication() {
        RemotePostingRepublishAttributes attributes = attributes(Arrays.asList((StoryAttributes) null));

        ValidationFailure failure = Assertions.assertThrows(
            ValidationFailure.class,
            () -> remotePostingController.republish("remote", "posting", attributes)
        );

        Assertions.assertEquals("posting.publications.missing", failure.getErrorCode());
        verifyNoInteractions(pickerPool);
    }

    @Test
    void republishRejectsNullExternalSourceBeforeQueueingPicks() {
        StoryAttributes publication = new StoryAttributes();
        publication.setExternalSourceUri(Arrays.asList("https://valid.example/story", null));
        RemotePostingRepublishAttributes attributes = attributes(List.of(publication));

        ValidationFailure failure = Assertions.assertThrows(
            ValidationFailure.class,
            () -> remotePostingController.republish("remote", "posting", attributes)
        );

        Assertions.assertEquals("story.external-source-uri.missing", failure.getErrorCode());
        verifyNoInteractions(pickerPool);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void remotePostingSubmissionRejectsNullExternalSourceBeforeSchedulingJobs(boolean updating) {
        StoryAttributes publication = new StoryAttributes();
        publication.setExternalSourceUri(Arrays.asList("https://valid.example/story", null));
        PostingSourceText text = new PostingSourceText();
        text.setBodySrc(new Body(Body.EMPTY));
        text.setPublications(List.of(publication));

        ValidationFailure failure = Assertions.assertThrows(ValidationFailure.class, () -> {
            if (updating) {
                remotePostingController.put("remote", "posting", text);
            } else {
                remotePostingController.post("remote", text);
            }
        });

        Assertions.assertEquals("story.external-source-uri.missing", failure.getErrorCode());
        verifyNoInteractions(jobs);
    }

    private static RemotePostingRepublishAttributes attributes(List<StoryAttributes> publications) {
        RemotePostingRepublishAttributes attributes = new RemotePostingRepublishAttributes();
        attributes.setRemoteFeedName("remote-feed");
        attributes.setPublications(publications);
        return attributes;
    }

    private static StoryAttributes publication(
        String feedName,
        long publishAt,
        boolean pinned,
        boolean viewed,
        boolean read,
        boolean satisfied
    ) {
        StoryAttributes publication = new StoryAttributes();
        publication.setFeedName(feedName);
        publication.setPublishAt(publishAt);
        publication.setPinned(pinned);
        publication.setViewed(viewed);
        publication.setRead(read);
        publication.setSatisfied(satisfied);
        return publication;
    }

    private static void assertPick(
        Pick pick,
        String feedName,
        long publishAt,
        boolean pinned,
        boolean viewed,
        boolean read,
        boolean satisfied
    ) {
        Assertions.assertEquals("remote", pick.getRemoteNodeName());
        Assertions.assertEquals("posting", pick.getRemotePostingId());
        Assertions.assertEquals("remote-feed", pick.getRemoteFeedName());
        Assertions.assertEquals(feedName, pick.getFeedName());
        Assertions.assertEquals(publishAt, pick.getPublishAt().toInstant().getEpochSecond());
        Assertions.assertEquals(pinned, pick.isPinned());
        Assertions.assertEquals(viewed, pick.isViewed());
        Assertions.assertEquals(read, pick.isRead());
        Assertions.assertEquals(satisfied, pick.isSatisfied());
        Assertions.assertEquals(Principal.PRIVATE, pick.getPostingViewPrincipal());
        Assertions.assertTrue(pick.isAllowMultiple());
    }

}
