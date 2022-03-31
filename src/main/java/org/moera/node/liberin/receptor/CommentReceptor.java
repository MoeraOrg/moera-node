package org.moera.node.liberin.receptor;

import java.util.UUID;

import org.moera.node.data.Comment;
import org.moera.node.data.Posting;
import org.moera.node.liberin.LiberinMapping;
import org.moera.node.liberin.LiberinReceptor;
import org.moera.node.liberin.LiberinReceptorBase;
import org.moera.node.liberin.model.CommentAddedLiberin;
import org.moera.node.liberin.model.CommentDeletedLiberin;
import org.moera.node.liberin.model.CommentUpdatedLiberin;
import org.moera.node.model.AvatarImage;
import org.moera.node.model.event.CommentAddedEvent;
import org.moera.node.model.event.CommentDeletedEvent;
import org.moera.node.model.event.CommentUpdatedEvent;
import org.moera.node.model.event.PostingCommentsChangedEvent;
import org.moera.node.model.notification.PostingCommentAddedNotification;
import org.moera.node.model.notification.PostingCommentDeletedNotification;
import org.moera.node.model.notification.PostingCommentsUpdatedNotification;
import org.moera.node.notification.send.Directions;

@LiberinReceptor
public class CommentReceptor extends LiberinReceptorBase {

    @LiberinMapping
    public void added(CommentAddedLiberin liberin) {
        Comment comment = liberin.getComment();
        Posting posting = liberin.getPosting();

        if (comment.getCurrentRevision().getSignature() != null) {
            UUID repliedToId = comment.getRepliedTo() != null ? comment.getRepliedTo().getId() : null;
            send(Directions.postingCommentsSubscribers(posting.getNodeId(), posting.getId()),
                    new PostingCommentAddedNotification(posting.getId(), posting.getCurrentRevision().getHeading(),
                            comment.getId(), comment.getOwnerName(), comment.getOwnerFullName(),
                            new AvatarImage(comment.getOwnerAvatarMediaFile(), comment.getOwnerAvatarShape()),
                            comment.getCurrentRevision().getHeading(), repliedToId));
        }

        send(liberin, new CommentAddedEvent(comment));
        send(liberin, new PostingCommentsChangedEvent(posting));
        send(Directions.postingSubscribers(posting.getNodeId(), posting.getId()),
                new PostingCommentsUpdatedNotification(posting.getId(), posting.getTotalChildren()));
    }

    @LiberinMapping
    public void updated(CommentUpdatedLiberin liberin) {
        Comment comment = liberin.getComment();
        UUID postingId = comment.getPosting().getId();

        if (comment.getCurrentRevision().getSignature() != null) {
            UUID repliedToId = comment.getRepliedTo() != null ? comment.getRepliedTo().getId() : null;
            send(Directions.postingCommentsSubscribers(comment.getNodeId(), postingId),
                    new PostingCommentAddedNotification(postingId,
                            comment.getPosting().getCurrentRevision().getHeading(), comment.getId(),
                            comment.getOwnerName(), comment.getOwnerFullName(),
                            new AvatarImage(comment.getOwnerAvatarMediaFile(), comment.getOwnerAvatarShape()),
                            comment.getCurrentRevision().getHeading(), repliedToId));
        }

        send(liberin, new CommentUpdatedEvent(comment));
    }

    @LiberinMapping
    public void updated(CommentDeletedLiberin liberin) {
        Comment comment = liberin.getComment();
        UUID postingId = comment.getPosting().getId();

        send(Directions.postingCommentsSubscribers(comment.getNodeId(), postingId),
                new PostingCommentDeletedNotification(postingId, comment.getId(), comment.getOwnerName(),
                        comment.getOwnerFullName(),
                        new AvatarImage(comment.getOwnerAvatarMediaFile(), comment.getOwnerAvatarShape())));
        send(liberin, new CommentDeletedEvent(comment));
    }

}
