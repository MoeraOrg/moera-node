package org.moera.node.liberin.receptor;

import java.util.Set;
import java.util.UUID;

import javax.inject.Inject;

import org.moera.node.data.Comment;
import org.moera.node.data.Entry;
import org.moera.node.data.EntryRepository;
import org.moera.node.data.Posting;
import org.moera.node.data.Reaction;
import org.moera.node.instant.PostingReactionInstants;
import org.moera.node.liberin.Liberin;
import org.moera.node.liberin.LiberinMapping;
import org.moera.node.liberin.LiberinReceptor;
import org.moera.node.liberin.LiberinReceptorBase;
import org.moera.node.liberin.model.PostingReactionAddedLiberin;
import org.moera.node.liberin.model.PostingReactionDeletedLiberin;
import org.moera.node.liberin.model.PostingReactionTotalsUpdatedLiberin;
import org.moera.node.liberin.model.PostingReactionsDeletedAllLiberin;
import org.moera.node.model.AvatarImage;
import org.moera.node.model.ReactionTotalsInfo;
import org.moera.node.model.event.PostingReactionsChangedEvent;
import org.moera.node.model.notification.PostingReactionAddedNotification;
import org.moera.node.model.notification.PostingReactionDeletedAllNotification;
import org.moera.node.model.notification.PostingReactionDeletedNotification;
import org.moera.node.model.notification.PostingReactionsUpdatedNotification;
import org.moera.node.notification.send.Directions;
import org.moera.node.operations.ReactionTotalOperations;

@LiberinReceptor
public class PostingReactionReceptor extends LiberinReceptorBase {

    @Inject
    private EntryRepository entryRepository;

    @Inject
    private ReactionTotalOperations reactionTotalOperations;

    @Inject
    private PostingReactionInstants postingReactionInstants;

    @LiberinMapping
    public void added(PostingReactionAddedLiberin liberin) {
        Posting posting = liberin.getPosting();

        updated(liberin, posting, liberin.getAddedReaction(), liberin.getDeletedReaction(),
                liberin.getReactionTotals());
    }

    @LiberinMapping
    public void deleted(PostingReactionDeletedLiberin liberin) {
        Posting posting = liberin.getPosting();

        updated(liberin, posting, null, liberin.getReaction(), liberin.getReactionTotals());
    }

    private void updated(Liberin liberin, Posting posting, Reaction addedReaction, Reaction deletedReaction,
                         ReactionTotalsInfo reactionTotals) {
        if (deletedReaction != null) {
            AvatarImage ownerAvatar = new AvatarImage(deletedReaction.getOwnerAvatarMediaFile(),
                    deletedReaction.getOwnerAvatarShape());
            if (posting.getParentMedia() == null) {
                // FIXME if not our node is author of the posting
                send(Directions.single(liberin.getNodeId(), posting.getOwnerName()),
                        new PostingReactionDeletedNotification(null, null, null,
                                posting.getId(), deletedReaction.getOwnerName(), deletedReaction.getOwnerFullName(),
                                ownerAvatar, deletedReaction.isNegative()));
                // FIXME else
                postingReactionInstants.deleted(posting.getId(), deletedReaction.getOwnerName(),
                        deletedReaction.isNegative());
            } else {
                Set<Entry> entries = entryRepository.findByMediaId(posting.getParentMedia().getId());
                for (Entry entry : entries) {
                    UUID parentPostingId = entry instanceof Comment
                            ? ((Comment) entry).getPosting().getId()
                            : entry.getId();
                    UUID parentCommentId = entry instanceof Comment ? entry.getId() : null;
                    send(Directions.single(liberin.getNodeId(), posting.getOwnerName()),
                            new PostingReactionDeletedNotification(parentPostingId, parentCommentId,
                                    posting.getParentMedia().getId(), posting.getId(), deletedReaction.getOwnerName(),
                                    deletedReaction.getOwnerFullName(),
                                    ownerAvatar, deletedReaction.isNegative()));
                }
            }
        }

        if (addedReaction != null && addedReaction.getSignature() != null) {
            AvatarImage ownerAvatar = new AvatarImage(addedReaction.getOwnerAvatarMediaFile(),
                    addedReaction.getOwnerAvatarShape());
            if (posting.getParentMedia() == null) {
                // FIXME if not our node is author of the posting
                send(Directions.single(liberin.getNodeId(), posting.getOwnerName()),
                        new PostingReactionAddedNotification(null, null, null, null,
                                posting.getId(), posting.getCurrentRevision().getHeading(),
                                addedReaction.getOwnerName(), addedReaction.getOwnerFullName(),
                                ownerAvatar, addedReaction.isNegative(), addedReaction.getEmoji()));
                // FIXME else
                postingReactionInstants.added(posting, addedReaction.getOwnerName(), addedReaction.getOwnerFullName(),
                        ownerAvatar, addedReaction.isNegative(), addedReaction.getEmoji());
            } else {
                Set<Entry> entries = entryRepository.findByMediaId(posting.getParentMedia().getId());
                for (Entry entry : entries) {
                    UUID parentPostingId = entry instanceof Comment
                            ? ((Comment) entry).getPosting().getId()
                            : entry.getId();
                    UUID parentCommentId = entry instanceof Comment ? entry.getId() : null;
                    send(Directions.single(liberin.getNodeId(), posting.getOwnerName()),
                            new PostingReactionAddedNotification(parentPostingId, parentCommentId,
                                    posting.getParentMedia().getId(), entry.getCurrentRevision().getHeading(),
                                    posting.getId(), posting.getCurrentRevision().getHeading(),
                                    addedReaction.getOwnerName(), addedReaction.getOwnerFullName(),
                                    ownerAvatar, addedReaction.isNegative(), addedReaction.getEmoji()));
                }
            }
        }

        send(liberin, new PostingReactionsChangedEvent(posting));
        send(Directions.postingSubscribers(posting.getNodeId(), posting.getId()),
                new PostingReactionsUpdatedNotification(posting.getId(), reactionTotals));
    }

    @LiberinMapping
    public void deletedAll(PostingReactionsDeletedAllLiberin liberin) {
        Posting posting = liberin.getPosting();

        send(liberin, new PostingReactionsChangedEvent(posting));
        var totalsInfo = reactionTotalOperations.getInfo(posting);
        send(Directions.postingSubscribers(posting.getNodeId(), posting.getId()),
                new PostingReactionsUpdatedNotification(posting.getId(), totalsInfo.getPublicInfo()));

        if (posting.getParentMedia() == null) {
            // FIXME if not our node is author of the posting
            send(Directions.single(liberin.getNodeId(), posting.getOwnerName()),
                    new PostingReactionDeletedAllNotification(null, null, null,
                            posting.getId()));
            // FIXME else
            postingReactionInstants.deletedAll(posting.getId());
        } else {
            Set<Entry> entries = entryRepository.findByMediaId(posting.getParentMedia().getId());
            for (Entry entry : entries) {
                UUID parentPostingId = entry instanceof Comment ? ((Comment) entry).getPosting().getId() : entry.getId();
                UUID parentCommentId = entry instanceof Comment ? entry.getId() : null;
                send(Directions.single(liberin.getNodeId(), posting.getOwnerName()),
                        new PostingReactionDeletedAllNotification(parentPostingId, parentCommentId,
                                posting.getParentMedia().getId(), posting.getId()));
            }
        }
    }

    @LiberinMapping
    public void totalsUpdated(PostingReactionTotalsUpdatedLiberin liberin) {
        Posting posting = liberin.getPosting();

        send(liberin, new PostingReactionsChangedEvent(posting));
        send(Directions.postingSubscribers(posting.getNodeId(), posting.getId()),
                new PostingReactionsUpdatedNotification(posting.getId(), liberin.getTotals()));
    }

}