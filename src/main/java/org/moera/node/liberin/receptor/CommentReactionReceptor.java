package org.moera.node.liberin.receptor;

import org.moera.lib.node.types.AvatarImage;
import org.moera.lib.node.types.SearchContentUpdateType;
import org.moera.lib.node.types.principal.Principal;
import org.moera.lib.node.types.principal.PrincipalExpression;
import org.moera.lib.node.types.principal.PrincipalFilter;
import org.moera.node.data.Comment;
import org.moera.node.data.Entry;
import org.moera.node.data.Reaction;
import org.moera.node.liberin.Liberin;
import org.moera.node.liberin.LiberinMapping;
import org.moera.node.liberin.LiberinReceptor;
import org.moera.node.liberin.LiberinReceptorBase;
import org.moera.node.liberin.model.CommentReactionAddedLiberin;
import org.moera.node.liberin.model.CommentReactionDeletedLiberin;
import org.moera.node.liberin.model.CommentReactionTotalsUpdatedLiberin;
import org.moera.node.liberin.model.CommentReactionsDeletedAllLiberin;
import org.moera.node.model.AvatarImageUtil;
import org.moera.node.model.event.CommentReactionsChangedEvent;
import org.moera.node.model.notification.CommentReactionAddedNotificationUtil;
import org.moera.node.model.notification.CommentReactionDeletedAllNotificationUtil;
import org.moera.node.model.notification.CommentReactionDeletedNotificationUtil;
import org.moera.node.model.notification.SearchContentUpdatedNotificationUtil;
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
            send(
                Directions.single(
                    liberin.getNodeId(), comment.getOwnerName(), visibilityFilter(comment, deletedReaction)
                ),
                CommentReactionDeletedNotificationUtil.build(
                    comment.getPosting().getId(),
                    comment.getId(),
                    deletedReaction.getOwnerName(),
                    deletedReaction.getOwnerFullName(),
                    deletedReaction.getOwnerGender(),
                    AvatarImageUtil.build(
                        deletedReaction.getOwnerAvatarMediaFile(), deletedReaction.getOwnerAvatarShape()
                    ),
                    deletedReaction.isNegative()
                )
            );
            send(
                Directions.searchSubscribers(liberin.getNodeId(), visibilityFilter(comment, deletedReaction)),
                SearchContentUpdatedNotificationUtil.buildReactionUpdate(
                    SearchContentUpdateType.REACTION_DELETE,
                    comment.getPosting().getId(),
                    comment.getId(),
                    deletedReaction.getOwnerName()
                )
            );
        }

        if (addedReaction != null && addedReaction.getSignature() != null) {
            Entry posting = comment.getPosting();
            AvatarImage postingOwnerAvatar = AvatarImageUtil.build(
                posting.getOwnerAvatarMediaFile(), posting.getOwnerAvatarShape()
            );
            send(
                Directions.single(
                    liberin.getNodeId(), comment.getOwnerName(), visibilityFilter(comment, addedReaction)
                ),
                CommentReactionAddedNotificationUtil.build(
                    posting.getOwnerName(),
                    posting.getOwnerFullName(),
                    posting.getOwnerGender(),
                    postingOwnerAvatar,
                    posting.getId(),
                    comment.getId(),
                    posting.getCurrentRevision().getHeading(),
                    comment.getCurrentRevision().getHeading(),
                    addedReaction.getOwnerName(),
                    addedReaction.getOwnerFullName(),
                    addedReaction.getOwnerGender(),
                    AvatarImageUtil.build(
                        addedReaction.getOwnerAvatarMediaFile(), addedReaction.getOwnerAvatarShape()
                    ),
                    addedReaction.isNegative(), addedReaction.getEmoji()
                )
            );
            send(
                Directions.searchSubscribers(liberin.getNodeId(), visibilityFilter(comment, addedReaction)),
                SearchContentUpdatedNotificationUtil.buildReactionUpdate(
                    SearchContentUpdateType.REACTION_ADD,
                    posting.getId(),
                    comment.getId(),
                    addedReaction.getOwnerName()
                )
            );
        }

        send(liberin, new CommentReactionsChangedEvent(comment, generalVisibilityFilter(comment)));
    }

    @LiberinMapping
    public void deletedAll(CommentReactionsDeletedAllLiberin liberin) {
        Comment comment = liberin.getComment();

        send(
            Directions.single(liberin.getNodeId(), comment.getOwnerName(), generalVisibilityFilter(comment)),
            CommentReactionDeletedAllNotificationUtil.build(comment.getPosting().getId(), comment.getId())
        );
        send(
            Directions.searchSubscribers(liberin.getNodeId(), generalVisibilityFilter(comment)),
            SearchContentUpdatedNotificationUtil.buildReactionUpdate(
                SearchContentUpdateType.REACTIONS_DELETE_ALL,
                comment.getPosting().getId(),
                comment.getId(),
                null
            )
        );
        send(liberin, new CommentReactionsChangedEvent(comment, generalVisibilityFilter(comment)));
    }

    @LiberinMapping
    public void totalsUpdated(CommentReactionTotalsUpdatedLiberin liberin) {
        send(
            liberin,
            new CommentReactionsChangedEvent(liberin.getComment(), generalVisibilityFilter(liberin.getComment()))
        );
    }

    private PrincipalExpression generalVisibilityFilter(Comment comment) {
        return comment.getViewE().a()
            .and(comment.getPosting().getViewE())
            .and(comment.getPosting().getViewCommentsE());
    }

    private PrincipalFilter visibilityFilter(Comment comment, Reaction reaction) {
        return generalVisibilityFilter(comment)
            .and(comment.getViewReactionsE())
            .and(reaction.isNegative() ? comment.getViewNegativeReactionsE() : Principal.PUBLIC)
            .and(reaction.getViewE());
    }

}
