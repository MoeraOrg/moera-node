package org.moera.node.model;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.moera.lib.node.types.CommentInfo;
import org.moera.lib.node.types.CommentOperations;
import org.moera.lib.node.types.PostingInfo;
import org.moera.lib.node.types.PostingOperations;
import org.moera.lib.node.types.principal.AccessChecker;
import org.moera.lib.node.types.principal.AccessCheckers;
import org.moera.lib.node.types.principal.Principal;
import org.moera.lib.node.types.body.Body;
import org.moera.node.data.Comment;
import org.moera.node.data.EntryRevision;
import org.moera.node.data.Posting;
import org.moera.node.operations.MediaAttachmentsProvider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ExternalSourceInfoUtilTest {

    private static final List<String> EXTERNAL_SOURCE_URIS =
        List.of("https://one.example/item", "https://two.example/item");

    @Test
    void postingExternalSourcesAreVisibleByDefault() {
        Posting posting = posting();

        PostingInfo info = build(posting, AccessCheckers.PUBLIC);

        assertEquals(EXTERNAL_SOURCE_URIS, info.getExternalSourceUri());
    }

    @Test
    void postingExternalSourcesRespectViewExternalSourceOperation() {
        Posting posting = posting();
        PostingOperations operations = new PostingOperations();
        operations.setViewExternalSource(Principal.NONE);
        var postingText = new org.moera.lib.node.types.PostingText();
        postingText.setOperations(operations);
        PostingTextUtil.toEntry(postingText, posting);

        PostingInfo info = build(posting, AccessCheckers.ADMIN);

        assertNull(info.getExternalSourceUri());
        assertEquals(Principal.NONE, info.getOperations().getViewExternalSource(Principal.PUBLIC));
    }

    @Test
    void cachedPostingExternalSourcesRespectReceiverOperation() {
        Posting posting = posting();
        posting.setReceiverName("remote");
        posting.setReceiverViewExternalSourcePrincipal(Principal.NONE);

        PostingInfo info = build(posting, AccessCheckers.ADMIN);

        assertNull(info.getExternalSourceUri());
    }

    @Test
    void pickedPostingCopiesExternalSourceUrisAndOperation() {
        PostingInfo info = new PostingInfo();
        info.setId("remote-posting");
        info.setCreatedAt(1000L);
        info.setEditedAt(2000L);
        info.setTotalComments(0);
        info.setExternalSourceUri(EXTERNAL_SOURCE_URIS);
        PostingOperations operations = new PostingOperations();
        operations.setViewExternalSource(Principal.SUBSCRIBED);
        info.setOperations(operations);
        Posting posting = new Posting();

        PostingInfoUtil.toPickedPosting(info, posting, null);

        assertEquals(EXTERNAL_SOURCE_URIS, List.of(posting.getExternalSourceUri()));
        assertEquals(Principal.PRIVATE, posting.getViewExternalSourcePrincipal());
        assertEquals(Principal.SUBSCRIBED, posting.getReceiverViewExternalSourcePrincipal());
    }

    @Test
    void pickedPostingViewOverrideDoesNotAffectExternalSources() {
        PostingInfo info = new PostingInfo();
        info.setId("remote-posting");
        info.setCreatedAt(1000L);
        info.setEditedAt(2000L);
        info.setTotalComments(0);
        PostingOperations operations = new PostingOperations();
        operations.setViewExternalSource(Principal.PUBLIC);
        info.setOperations(operations);
        Posting posting = new Posting();

        PostingInfoUtil.toPickedPosting(info, posting, Principal.SECRET);

        assertEquals(Principal.PUBLIC, posting.getViewExternalSourcePrincipal());
        assertEquals(Principal.PUBLIC, posting.getReceiverViewExternalSourcePrincipal());
    }

    @Test
    void commentExternalSourcesAreVisibleByDefault() {
        Comment comment = comment();

        CommentInfo info = build(comment, AccessCheckers.PUBLIC);

        assertEquals(EXTERNAL_SOURCE_URIS, info.getExternalSourceUri());
    }

    @Test
    void commentExternalSourcesRespectViewExternalSourceOperation() {
        Comment comment = comment();
        CommentOperations operations = new CommentOperations();
        operations.setViewExternalSource(Principal.NONE);
        var commentText = new org.moera.lib.node.types.CommentText();
        commentText.setOperations(operations);
        CommentTextUtil.toEntry(commentText, comment);

        CommentInfo info = build(comment, AccessCheckers.ADMIN);

        assertNull(info.getExternalSourceUri());
        assertEquals(Principal.NONE, info.getOperations().getViewExternalSource(Principal.PUBLIC));
    }

    @Test
    void commentExternalSourcesRespectSeniorOperation() {
        Comment comment = comment();
        CommentOperations seniorOperations = new CommentOperations();
        seniorOperations.setViewExternalSource(Principal.NONE);
        var commentText = new org.moera.lib.node.types.CommentText();
        commentText.setSeniorOperations(seniorOperations);
        CommentTextUtil.toEntrySenior(commentText, comment);

        CommentInfo info = build(comment, AccessCheckers.ADMIN);

        assertNull(info.getExternalSourceUri());
        assertEquals(Principal.NONE, info.getSeniorOperations().getViewExternalSource(Principal.UNSET));
    }

    private static Posting posting() {
        Posting posting = new Posting();
        posting.setId(UUID.randomUUID());
        posting.setExternalSourceUri(EXTERNAL_SOURCE_URIS.toArray(String[]::new));
        EntryRevision revision = revision();
        revision.setEntry(posting);
        posting.setCurrentRevision(revision);
        return posting;
    }

    private static Comment comment() {
        Posting posting = posting();
        Comment comment = new Comment();
        comment.setId(UUID.randomUUID());
        comment.setPosting(posting);
        comment.setMoment(1L);
        comment.setExternalSourceUri(EXTERNAL_SOURCE_URIS.toArray(String[]::new));
        EntryRevision revision = revision();
        revision.setEntry(comment);
        revision.setParent(posting.getCurrentRevision());
        comment.setCurrentRevision(revision);
        return comment;
    }

    private static EntryRevision revision() {
        EntryRevision revision = new EntryRevision();
        revision.setId(UUID.randomUUID());
        revision.setBodyPreview(Body.EMPTY);
        revision.setBody(Body.EMPTY);
        return revision;
    }

    private static PostingInfo build(Posting posting, AccessChecker accessChecker) {
        return PostingInfoUtil.build(
            posting, MediaAttachmentsProvider.NONE, accessChecker, null, null
        );
    }

    private static CommentInfo build(Comment comment, AccessChecker accessChecker) {
        return CommentInfoUtil.build(
            comment, MediaAttachmentsProvider.NONE, accessChecker, null, null
        );
    }

}
