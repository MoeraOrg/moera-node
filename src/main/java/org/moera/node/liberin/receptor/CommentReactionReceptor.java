package org.moera.node.liberin.receptor;

import org.moera.node.data.Comment;
import org.moera.node.data.Reaction;
import org.moera.node.liberin.Liberin;
import org.moera.node.liberin.LiberinMapping;
import org.moera.node.liberin.LiberinReceptor;
import org.moera.node.liberin.LiberinReceptorBase;
import org.moera.node.liberin.model.CommentReactionAddedLiberin;
import org.moera.node.liberin.model.CommentReactionDeletedLiberin;
import org.moera.node.liberin.model.CommentReactionTotalsUpdatedLiberin;
import org.moera.node.liberin.model.CommentReactionsDeletedAllLiberin;
import org.moera.node.model.AvatarImage;
import org.moera.node.model.event.CommentReactionsChangedEvent;
import org.moera.node.model.notification.CommentReactionAddedNotification;
import org.moera.node.model.notification.CommentReactionDeletedAllNotification;
import org.moera.node.model.notification.CommentReactionDeletedNotification;
import org.moera.node.notification.send.Directions;

@LiberinReceptor
public class CommentReactionReceptor extends LiberinReceptorBase {

    @LiberinMapping
    public void added(CommentReactionAddedLiberin liberin) {
        updated(liberin, liberin.getComment(), liberin.getAddedReaction(), liberin.getDeletedReaction());
    }

    @LiberinMapping
    public void deleted(CommentReactionDeletedLiberin liberin) {
        updated(liberin, liberin.getComment(), null, liberin.getReaction());
    }

    private void updated(Liberin liberin, Comment comment, Reaction addedReaction, Reaction deletedReaction) {
        if (deletedReaction != null) {
            send(Directions.single(liberin.getNodeId(), comment.getOwnerName()),
                    new CommentReactionDeletedNotification(comment.getPosting().getId(), comment.getId(),
                            deletedReaction.getOwnerName(), deletedReaction.getOwnerFullName(),
                            new AvatarImage(
                                    deletedReaction.getOwnerAvatarMediaFile(), deletedReaction.getOwnerAvatarShape()),
                            deletedReaction.isNegative()));
        }

        if (addedReaction != null && addedReaction.getSignature() != null) {
            send(Directions.single(liberin.getNodeId(), comment.getOwnerName()),
                    new CommentReactionAddedNotification(comment.getPosting().getId(), comment.getId(),
                            comment.getPosting().getCurrentRevision().getHeading(),
                            comment.getCurrentRevision().getHeading(), addedReaction.getOwnerName(),
                            addedReaction.getOwnerFullName(),
                            new AvatarImage(
                                    addedReaction.getOwnerAvatarMediaFile(), addedReaction.getOwnerAvatarShape()),
                            addedReaction.isNegative(), addedReaction.getEmoji()));
        }

        send(liberin, new CommentReactionsChangedEvent(comment));
    }

    @LiberinMapping
    public void deletedAll(CommentReactionsDeletedAllLiberin liberin) {
        Comment comment = liberin.getComment();

        send(Directions.single(liberin.getNodeId(), comment.getOwnerName()),
                new CommentReactionDeletedAllNotification(comment.getPosting().getId(), comment.getId()));
        send(liberin, new CommentReactionsChangedEvent(comment));
    }

    @LiberinMapping
    public void totalsUpdated(CommentReactionTotalsUpdatedLiberin liberin) {
        send(liberin, new CommentReactionsChangedEvent(liberin.getComment()));
    }

}
