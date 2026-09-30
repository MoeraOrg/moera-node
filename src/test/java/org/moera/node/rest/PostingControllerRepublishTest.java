package org.moera.node.rest;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.moera.lib.node.types.BlockedOperation;
import org.moera.lib.node.types.PostingRepublishAttributes;
import org.moera.lib.node.types.Result;
import org.moera.lib.node.types.Scope;
import org.moera.lib.node.types.StoryAttributes;
import org.moera.lib.node.types.principal.Principal;
import org.moera.lib.node.types.validate.ValidationFailure;
import org.moera.node.auth.AuthenticationException;
import org.moera.node.auth.UserBlockedException;
import org.moera.node.data.EntryRevision;
import org.moera.node.data.Posting;
import org.moera.node.data.PostingRepository;
import org.moera.node.global.RequestContext;
import org.moera.node.liberin.model.PostingUpdatedLiberin;
import org.moera.node.model.ObjectNotFoundFailure;
import org.moera.node.operations.BlockedUserOperations;
import org.moera.node.operations.StoryOperations;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostingControllerRepublishTest {

    private static final UUID POSTING_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Mock
    private RequestContext requestContext;

    @Mock
    private PostingRepository postingRepository;

    @Mock
    private StoryOperations storyOperations;

    @Mock
    private EntityManager entityManager;

    @Mock
    private BlockedUserOperations blockedUserOperations;

    @InjectMocks
    private PostingController postingController;

    @Test
    void republishChangesVisibilityAndEmitsUpdateBeforePublishing() {
        Posting posting = posting();
        allowPosting(posting);
        when(requestContext.isPrincipal(posting.getEditE(), Scope.UPDATE_POST)).thenReturn(true);
        PostingRepublishAttributes attributes = attributes();
        attributes.setPostingView(Principal.PRIVATE);
        Principal previousView = posting.getViewE();

        Assertions.assertSame(Result.OK, postingController.republish(POSTING_ID.toString(), attributes));

        Assertions.assertEquals(Principal.PRIVATE, posting.getViewPrincipal());
        ArgumentCaptor<PostingUpdatedLiberin> captor = ArgumentCaptor.forClass(PostingUpdatedLiberin.class);
        verify(requestContext).send(captor.capture());
        Assertions.assertSame(posting, captor.getValue().getPosting());
        Assertions.assertSame(posting.getCurrentRevision(), captor.getValue().getLatestRevision());
        Assertions.assertEquals(previousView, captor.getValue().getLatestViewPrincipal());
        InOrder order = inOrder(requestContext, storyOperations);
        order.verify(requestContext).send(any(PostingUpdatedLiberin.class));
        order.verify(storyOperations).publish(posting, attributes.getPublications(), true);
    }

    @Test
    void republishWithoutVisibilityChangeOnlyPublishes() {
        Posting posting = posting();
        allowPosting(posting);
        PostingRepublishAttributes attributes = attributes();

        postingController.republish(POSTING_ID.toString(), attributes);

        Assertions.assertEquals(Principal.PUBLIC, posting.getViewPrincipal());
        verify(requestContext, never()).send(any());
        verify(storyOperations).publish(posting, attributes.getPublications(), true);
    }

    @Test
    void republishWithUnchangedVisibilityDoesNotEmitUpdate() {
        Posting posting = posting();
        allowPosting(posting);
        PostingRepublishAttributes attributes = attributes();
        attributes.setPostingView(Principal.PUBLIC);

        postingController.republish(POSTING_ID.toString(), attributes);

        verify(requestContext, never()).send(any());
        verify(storyOperations).publish(posting, attributes.getPublications(), true);
    }

    @Test
    void republishRejectsInvisiblePosting() {
        Posting posting = posting();
        when(postingRepository.findFullByNodeIdAndId(any(), eq(POSTING_ID))).thenReturn(Optional.of(posting));
        PostingRepublishAttributes attributes = attributes();

        Assertions.assertThrows(
            ObjectNotFoundFailure.class,
            () -> postingController.republish(POSTING_ID.toString(), attributes)
        );
        verifyNoInteractions(storyOperations);
    }

    @Test
    void republishRequiresEditPermissionToChangeVisibility() {
        Posting posting = posting();
        when(postingRepository.findFullByNodeIdAndId(any(), eq(POSTING_ID))).thenReturn(Optional.of(posting));
        when(requestContext.isPrincipal(posting.getViewE(), Scope.VIEW_CONTENT)).thenReturn(true);
        PostingRepublishAttributes attributes = attributes();
        attributes.setPostingView(Principal.PRIVATE);

        Assertions.assertThrows(
            AuthenticationException.class,
            () -> postingController.republish(POSTING_ID.toString(), attributes)
        );
        verifyNoInteractions(storyOperations);
    }

    @Test
    void republishWithoutVisibilityChangeOnlyRequiresFeedPermission() {
        Posting posting = posting();
        when(postingRepository.findFullByNodeIdAndId(any(), eq(POSTING_ID))).thenReturn(Optional.of(posting));
        when(requestContext.isPrincipal(posting.getViewE(), Scope.VIEW_CONTENT)).thenReturn(true);
        when(requestContext.isAdmin(Scope.UPDATE_FEEDS)).thenReturn(true);
        PostingRepublishAttributes attributes = attributes();

        postingController.republish(POSTING_ID.toString(), attributes);

        verify(storyOperations).publish(posting, attributes.getPublications(), true);
    }

    @Test
    void republishRequiresFeedPermission() {
        Posting posting = posting();
        when(postingRepository.findFullByNodeIdAndId(any(), eq(POSTING_ID))).thenReturn(Optional.of(posting));
        when(requestContext.isPrincipal(posting.getViewE(), Scope.VIEW_CONTENT)).thenReturn(true);
        PostingRepublishAttributes attributes = attributes();

        Assertions.assertThrows(
            AuthenticationException.class,
            () -> postingController.republish(POSTING_ID.toString(), attributes)
        );
        verifyNoInteractions(storyOperations);
    }

    @Test
    void republishRejectsBlockedPosting() {
        Posting posting = posting();
        allowPosting(posting);
        when(blockedUserOperations.isBlocked(BlockedOperation.POSTING)).thenReturn(true);
        PostingRepublishAttributes attributes = attributes();

        Assertions.assertThrows(
            UserBlockedException.class,
            () -> postingController.republish(POSTING_ID.toString(), attributes)
        );
        verifyNoInteractions(storyOperations);
    }

    @Test
    void republishRejectsNullPublication() {
        PostingRepublishAttributes attributes = attributes();
        attributes.setPublications(Arrays.asList((StoryAttributes) null));

        ValidationFailure failure = Assertions.assertThrows(
            ValidationFailure.class,
            () -> postingController.republish(POSTING_ID.toString(), attributes)
        );

        Assertions.assertEquals("posting.publications.missing", failure.getErrorCode());
        verifyNoInteractions(storyOperations);
    }

    private void allowPosting(Posting posting) {
        when(postingRepository.findFullByNodeIdAndId(any(), eq(POSTING_ID))).thenReturn(Optional.of(posting));
        when(requestContext.isPrincipal(posting.getViewE(), Scope.VIEW_CONTENT)).thenReturn(true);
        when(requestContext.isAdmin(Scope.UPDATE_FEEDS)).thenReturn(true);
    }

    private static Posting posting() {
        Posting posting = new Posting();
        posting.setId(POSTING_ID);
        posting.setOwnerName("owner");
        posting.setViewPrincipal(Principal.PUBLIC);
        posting.setCurrentRevision(new EntryRevision());
        return posting;
    }

    private static PostingRepublishAttributes attributes() {
        PostingRepublishAttributes attributes = new PostingRepublishAttributes();
        StoryAttributes publication = new StoryAttributes();
        publication.setFeedName("timeline");
        attributes.setPublications(List.of(publication));
        return attributes;
    }

}
