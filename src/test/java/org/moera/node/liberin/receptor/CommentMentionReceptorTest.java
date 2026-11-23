package org.moera.node.liberin.receptor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.moera.lib.node.types.body.Body;
import org.moera.lib.node.types.notifications.MentionCommentAddedNotification;
import org.moera.lib.node.types.notifications.MentionCommentDeletedNotification;
import org.moera.lib.node.types.notifications.Notification;
import org.moera.lib.node.types.principal.Principal;
import org.moera.node.data.Comment;
import org.moera.node.data.EntryRevision;
import org.moera.node.data.FavorType;
import org.moera.node.data.Posting;
import org.moera.node.global.UniversalContext;
import org.moera.node.instant.CommentInstants;
import org.moera.node.liberin.Liberin;
import org.moera.node.liberin.model.CommentAddedLiberin;
import org.moera.node.liberin.model.CommentDeletedLiberin;
import org.moera.node.liberin.model.CommentUpdatedLiberin;
import org.moera.node.model.event.Event;
import org.moera.node.notification.send.Direction;
import org.moera.node.operations.FavorOperations;
import org.moera.node.userlist.SheriffUserListOperations;
import org.moera.node.util.Transaction;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

class CommentMentionReceptorTest {

    private final Posting posting = new Posting();
    private final Comment comment = new Comment();
    private final List<Notification> mentions = new ArrayList<>();
    private final FavorOperations favorOperations = mock(FavorOperations.class);
    private final CommentReceptor receptor = new CommentReceptor() {

        @Override
        protected void send(Liberin liberin, Event event) {
        }

        @Override
        protected void send(Direction direction, Notification notification) {
            if (notification instanceof MentionCommentAddedNotification
                || notification instanceof MentionCommentDeletedNotification) {
                mentions.add(notification);
            }
        }
    };

    @BeforeEach
    void setUp() {
        posting.setId(UUID.randomUUID());
        posting.setNodeId(UUID.randomUUID());
        posting.setOwnerName("author_0");
        posting.setCurrentRevision(revision("A posting"));
        comment.setId(UUID.randomUUID());
        comment.setNodeId(posting.getNodeId());
        comment.setPosting(posting);
        comment.setMoment(1L);
        comment.setOwnerName("commenter_0");
        comment.setCurrentRevision(revision("<a data-nodename='alice_0'>Alice</a>"));
        ReflectionTestUtils.setField(receptor, "tx", mock(Transaction.class));
        ReflectionTestUtils.setField(receptor, "favorOperations", favorOperations);
        ReflectionTestUtils.setField(receptor, "universalContext", mock(UniversalContext.class));
        ReflectionTestUtils.setField(receptor, "sheriffUserListOperations", mock(SheriffUserListOperations.class));
    }

    @Test
    void privateCommentEditsUpdateFavorsOncePerMentionedUser() {
        comment.setViewPrincipal(Principal.ADMIN);
        var previous = revision("""
            <a data-nodename="alice_0">Alice</a><a data-nodename="removed_2">former mention</a>
            """);
        comment.setCurrentRevision(revision("""
            <a data-nodename="alice">same user</a>
            <a data-nodename="other">new user</a><a data-nodename="other_0">same new user</a>
            <a data-nodename="commenter">self</a><a href="https://moera.page/@linked/~/post/target">link</a>
            """));

        receptor.updated(new CommentUpdatedLiberin(comment, previous, comment.getViewE()));

        verify(favorOperations).addFavor(comment.getNodeId(), "other_0", FavorType.MENTION);
        verify(favorOperations).addFavor(comment.getNodeId(), "removed_2", FavorType.UNMENTION);
        verifyNoMoreInteractions(favorOperations);
    }

    @Test
    void signingTheCommentNotifiesMentionedPeers() {
        var unsigned = comment.getCurrentRevision();
        unsigned.setSignature(null);
        receptor.added(new CommentAddedLiberin(posting, comment));
        receptor.updated(new CommentUpdatedLiberin(comment, unsigned, Principal.ADMIN));
        assertTrue(mentions.isEmpty());
        verifyNoInteractions(favorOperations);

        var signed = revision("<a data-nodename='alice_0'>Alice</a>");
        comment.setCurrentRevision(signed);
        receptor.updated(new CommentUpdatedLiberin(comment, unsigned, comment.getViewE()));
        receptor.updated(new CommentUpdatedLiberin(comment, signed, comment.getViewE()));

        assertEquals(1, mentions.size());
        assertInstanceOf(MentionCommentAddedNotification.class, mentions.getFirst());
        verify(favorOperations).addFavor(comment.getNodeId(), "alice_0", FavorType.MENTION);
        verifyNoMoreInteractions(favorOperations);
    }

    @Test
    void signingAfterSeveralUnsignedEditsUsesTheLastSignedMentions() {
        var original = comment.getCurrentRevision();
        comment.addRevision(original);
        var firstEdit = revision("<a data-nodename='temp_0'>temporary</a>");
        firstEdit.setSignature(null);
        comment.setCurrentRevision(firstEdit);
        receptor.updated(new CommentUpdatedLiberin(comment, original, Principal.PUBLIC));
        var secondEdit = revision("<a data-nodename='other_0'>Other</a>");
        secondEdit.setSignature(null);
        comment.setCurrentRevision(secondEdit);
        receptor.updated(new CommentUpdatedLiberin(comment, firstEdit, Principal.PUBLIC));
        assertTrue(mentions.isEmpty());

        var signed = revision("<a data-nodename='alice_0'>Alice</a><a data-nodename='other_0'>Other</a>");
        comment.addRevision(signed);
        comment.setCurrentRevision(signed);
        receptor.updated(new CommentUpdatedLiberin(comment, secondEdit, Principal.PUBLIC));

        assertEquals(1, mentions.size());
        assertInstanceOf(MentionCommentAddedNotification.class, mentions.getFirst());
    }

    @Test
    void deletingACommentDuringAnUnsignedEditWithdrawsTheSignedMention() {
        comment.addRevision(comment.getCurrentRevision());
        var unsigned = revision("pending edit");
        unsigned.setSignature(null);
        comment.addRevision(unsigned);
        comment.setCurrentRevision(unsigned);
        ReflectionTestUtils.setField(receptor, "commentInstants", mock(CommentInstants.class));

        receptor.deleted(new CommentDeletedLiberin(comment, unsigned));

        assertEquals(1, mentions.size());
        assertInstanceOf(MentionCommentDeletedNotification.class, mentions.getFirst());
        verify(favorOperations).addFavor(comment.getNodeId(), "alice_0", FavorType.UNMENTION);
        verifyNoMoreInteractions(favorOperations);
    }

    @Test
    void changingCommentVisibilityWithdrawsAndRestoresItsExistingMention() {
        comment.setViewPrincipal(Principal.ADMIN);
        receptor.updated(new CommentUpdatedLiberin(comment, comment.getCurrentRevision(), Principal.PUBLIC));
        comment.setViewPrincipal(Principal.PUBLIC);
        receptor.updated(new CommentUpdatedLiberin(comment, comment.getCurrentRevision(), Principal.ADMIN));

        assertEquals(2, mentions.size());
        assertInstanceOf(MentionCommentDeletedNotification.class, mentions.getFirst());
        assertInstanceOf(MentionCommentAddedNotification.class, mentions.getLast());
        verifyNoInteractions(favorOperations);
    }

    private EntryRevision revision(String text) {
        Body body = new Body();
        body.setText(text);
        EntryRevision revision = new EntryRevision();
        revision.setId(UUID.randomUUID());
        revision.setParent(posting.getCurrentRevision());
        revision.setBody(body.getEncoded());
        revision.setBodyPreview(body.getEncoded());
        revision.setSignature(new byte[]{1});
        return revision;
    }

}
