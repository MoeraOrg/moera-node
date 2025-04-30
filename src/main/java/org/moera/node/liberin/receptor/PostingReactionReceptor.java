package org.moera.node.liberin.receptor;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import jakarta.inject.Inject;

import org.moera.lib.node.types.AvatarImage;
import org.moera.lib.node.types.ReactionTotalsInfo;
import org.moera.lib.node.types.SearchContentUpdateType;
import org.moera.lib.node.types.principal.Principal;
import org.moera.lib.node.types.principal.PrincipalExpression;
import org.moera.lib.node.types.principal.PrincipalFilter;
import org.moera.node.data.Comment;
import org.moera.node.data.Entry;
import org.moera.node.data.EntryRepository;
import org.moera.node.data.Posting;
import org.moera.node.data.Reaction;
import org.moera.node.friends.FriendCache;
import org.moera.node.friends.SubscribedCache;
import org.moera.node.instant.PostingReactionInstants;
import org.moera.node.liberin.Liberin;
import org.moera.node.liberin.LiberinMapping;
import org.moera.node.liberin.LiberinReceptor;
import org.moera.node.liberin.LiberinReceptorBase;
import org.moera.node.liberin.model.PostingReactionAddedLiberin;
import org.moera.node.liberin.model.PostingReactionDeletedLiberin;
import org.moera.node.liberin.model.PostingReactionTotalsUpdatedLiberin;
import org.moera.node.liberin.model.PostingReactionsDeletedAllLiberin;
import org.moera.node.model.AvatarImageUtil;
import org.moera.node.model.event.PostingReactionsChangedEvent;
import org.moera.node.model.notification.PostingReactionAddedNotificationUtil;
import org.moera.node.model.notification.PostingReactionDeletedAllNotificationUtil;
import org.moera.node.model.notification.PostingReactionDeletedNotificationUtil;
import org.moera.node.model.notification.PostingReactionsUpdatedNotificationUtil;
import org.moera.node.model.notification.SearchContentUpdatedNotificationUtil;
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

    @Inject
    private FriendCache friendCache;

    @Inject
    private SubscribedCache subscribedCache;

    @LiberinMapping
    public void added(PostingReactionAddedLiberin liberin) {
        Posting posting = liberin.getPosting();

        updated(
            liberin, posting, liberin.getAddedReaction(), liberin.getDeletedReaction(), liberin.getReactionTotals()
        );
    }

    @LiberinMapping
    public void deleted(PostingReactionDeletedLiberin liberin) {
        Posting posting = liberin.getPosting();

        updated(liberin, posting, null, liberin.getReaction(), liberin.getReactionTotals());
    }

    private void updated(
        Liberin liberin,
        Posting posting,
        Reaction addedReaction,
        Reaction deletedReaction,
        ReactionTotalsInfo reactionTotals
    ) {
        if (deletedReaction != null) {
            AvatarImage ownerAvatar = AvatarImageUtil.build(
                deletedReaction.getOwnerAvatarMediaFile(), deletedReaction.getOwnerAvatarShape()
            );
            if (posting.getParentMedia() == null) {
                if (!Objects.equals(posting.getOwnerName(), universalContext.nodeName())) {
                    send(
                        Directions.single(
                            liberin.getNodeId(), posting.getOwnerName(), visibilityFilter(posting, deletedReaction)
                        ),
                        PostingReactionDeletedNotificationUtil.build(
                            null,
                            null,
                            null,
                            posting.getId(),
                            deletedReaction.getOwnerName(),
                            deletedReaction.getOwnerFullName(),
                            deletedReaction.getOwnerGender(),
                            ownerAvatar,
                            deletedReaction.isNegative()
                        )
                    );
                } else {
                    if (
                        visibilityFilter(posting, deletedReaction).includes(
                            true,
                            posting.getOwnerName(),
                            subscribedCache.isSubscribed(posting.getOwnerName()),
                            friendCache.getClientGroupIds(posting.getOwnerName())
                        )
                    ) {
                        postingReactionInstants.deleted(
                            posting.getId(), deletedReaction.getOwnerName(), deletedReaction.isNegative()
                        );
                    }
                }
                send(
                    Directions.searchSubscribers(liberin.getNodeId(), visibilityFilter(posting, deletedReaction)),
                    SearchContentUpdatedNotificationUtil.buildReactionUpdate(
                        SearchContentUpdateType.REACTION_DELETE,
                        posting.getId(),
                        deletedReaction.getOwnerName()
                    )
                );
            } else {
                Set<Entry> entries = entryRepository.findByMediaId(posting.getParentMedia().getId());
                for (Entry entry : entries) {
                    UUID parentPostingId = entry instanceof Comment comment
                        ? comment.getPosting().getId()
                        : entry.getId();
                    UUID parentCommentId = entry instanceof Comment ? entry.getId() : null;
                    send(
                        Directions.single(
                            liberin.getNodeId(), posting.getOwnerName(), visibilityFilter(posting, deletedReaction)
                        ),
                        PostingReactionDeletedNotificationUtil.build(
                            parentPostingId,
                            parentCommentId,
                            posting.getParentMedia().getId(),
                            posting.getId(),
                            deletedReaction.getOwnerName(),
                            deletedReaction.getOwnerFullName(),
                            deletedReaction.getOwnerGender(),
                            ownerAvatar,
                            deletedReaction.isNegative()
                        )
                    );
                }
            }
        }

        if (addedReaction != null && addedReaction.getSignature() != null) {
            AvatarImage ownerAvatar = AvatarImageUtil.build(
                addedReaction.getOwnerAvatarMediaFile(), addedReaction.getOwnerAvatarShape()
            );
            if (posting.getParentMedia() == null) {
                if (!Objects.equals(posting.getOwnerName(), universalContext.nodeName())) {
                    AvatarImage postingOwnerAvatar = AvatarImageUtil.build(
                        posting.getOwnerAvatarMediaFile(), posting.getOwnerAvatarShape()
                    );
                    send(
                        Directions.single(
                            liberin.getNodeId(), posting.getOwnerName(), visibilityFilter(posting, addedReaction)
                        ),
                        PostingReactionAddedNotificationUtil.build(
                            posting.getOwnerName(),
                            posting.getOwnerFullName(),
                            posting.getOwnerGender(),
                            postingOwnerAvatar,
                            null,
                            null,
                            null,
                            null,
                            posting.getId(),
                            posting.getCurrentRevision().getHeading(),
                            addedReaction.getOwnerName(),
                            addedReaction.getOwnerFullName(),
                            addedReaction.getOwnerGender(),
                            ownerAvatar,
                            addedReaction.isNegative(),
                            addedReaction.getEmoji()
                        )
                    );
                } else {
                    if (
                        visibilityFilter(posting, addedReaction).includes(
                            true,
                            posting.getOwnerName(),
                            subscribedCache.isSubscribed(posting.getOwnerName()),
                            friendCache.getClientGroupIds(posting.getOwnerName())
                        )
                    ) {
                        postingReactionInstants.added(
                            posting,
                            addedReaction.getOwnerName(),
                            addedReaction.getOwnerFullName(),
                            addedReaction.getOwnerGender(),
                            ownerAvatar,
                            addedReaction.isNegative(),
                            addedReaction.getEmoji()
                        );
                    }
                }
                send(
                    Directions.searchSubscribers(liberin.getNodeId(), visibilityFilter(posting, addedReaction)),
                    SearchContentUpdatedNotificationUtil.buildReactionUpdate(
                        SearchContentUpdateType.REACTION_ADD,
                        posting.getId(),
                        addedReaction.getOwnerName()
                    )
                );
            } else {
                Set<Entry> entries = entryRepository.findByMediaId(posting.getParentMedia().getId());
                for (Entry entry : entries) {
                    Entry parentPosting = entry instanceof Comment comment ? comment.getPosting() : entry;
                    AvatarImage parentPostingAvatar = AvatarImageUtil.build(
                        parentPosting.getOwnerAvatarMediaFile(), parentPosting.getOwnerAvatarShape()
                    );
                    UUID parentCommentId = entry instanceof Comment ? entry.getId() : null;
                    send(
                        Directions.single(
                            liberin.getNodeId(), posting.getOwnerName(), visibilityFilter(posting, addedReaction)
                        ),
                        PostingReactionAddedNotificationUtil.build(
                            parentPosting.getOwnerName(),
                            parentPosting.getOwnerFullName(),
                            parentPosting.getOwnerGender(),
                            parentPostingAvatar,
                            parentPosting.getId(),
                            parentCommentId,
                            posting.getParentMedia().getId(),
                            entry.getCurrentRevision().getHeading(),
                            posting.getId(),
                            posting.getCurrentRevision().getHeading(),
                            addedReaction.getOwnerName(),
                            addedReaction.getOwnerFullName(),
                            addedReaction.getOwnerGender(),
                            ownerAvatar,
                            addedReaction.isNegative(),
                            addedReaction.getEmoji()
                        )
                    );
                }
            }
        }

        send(liberin, new PostingReactionsChangedEvent(posting));
        send(
            Directions.postingSubscribers(posting.getNodeId(), posting.getId()),
            PostingReactionsUpdatedNotificationUtil.build(posting.getId(), reactionTotals)
        );
    }

    @LiberinMapping
    public void deletedAll(PostingReactionsDeletedAllLiberin liberin) {
        Posting posting = liberin.getPosting();

        send(liberin, new PostingReactionsChangedEvent(posting));
        var totalsInfo = reactionTotalOperations.getInfo(posting);
        send(
            Directions.postingSubscribers(posting.getNodeId(), posting.getId()),
            PostingReactionsUpdatedNotificationUtil.build(posting.getId(), totalsInfo.getPublicInfo())
        );

        if (posting.getParentMedia() == null) {
            if (!Objects.equals(posting.getOwnerName(), universalContext.nodeName())) {
                send(
                    Directions.single(liberin.getNodeId(), posting.getOwnerName(), generalVisibilityFilter(posting)),
                    PostingReactionDeletedAllNotificationUtil.build(null, null, null, posting.getId())
                );
            } else {
                if (
                    generalVisibilityFilter(posting).includes(
                        true,
                        posting.getOwnerName(),
                        subscribedCache.isSubscribed(posting.getOwnerName()),
                        friendCache.getClientGroupIds(posting.getOwnerName())
                    )
                ) {
                    postingReactionInstants.deletedAll(posting.getId());
                }
            }
            send(
                Directions.searchSubscribers(liberin.getNodeId(), generalVisibilityFilter(posting)),
                SearchContentUpdatedNotificationUtil.buildReactionUpdate(
                    SearchContentUpdateType.REACTIONS_DELETE_ALL,
                    posting.getId(),
                    null
                )
            );
        } else {
            Set<Entry> entries = entryRepository.findByMediaId(posting.getParentMedia().getId());
            for (Entry entry : entries) {
                UUID parentPostingId = entry instanceof Comment comment ? comment.getPosting().getId() : entry.getId();
                UUID parentCommentId = entry instanceof Comment ? entry.getId() : null;
                send(
                    Directions.single(liberin.getNodeId(), posting.getOwnerName(), generalVisibilityFilter(posting)),
                    PostingReactionDeletedAllNotificationUtil.build(
                        parentPostingId, parentCommentId, posting.getParentMedia().getId(), posting.getId()
                    )
                );
            }
        }
    }

    @LiberinMapping
    public void totalsUpdated(PostingReactionTotalsUpdatedLiberin liberin) {
        Posting posting = liberin.getPosting();

        send(liberin, new PostingReactionsChangedEvent(posting));
        send(
            Directions.postingSubscribers(posting.getNodeId(), posting.getId()),
            PostingReactionsUpdatedNotificationUtil.build(posting.getId(), liberin.getTotals())
        );
    }

    private PrincipalExpression generalVisibilityFilter(Posting posting) {
        return posting.getViewE().a()
            .and(posting.getViewReactionsE());
    }

    private PrincipalFilter visibilityFilter(Posting posting, Reaction reaction) {
        return generalVisibilityFilter(posting)
            .and(reaction.isNegative() ? posting.getViewNegativeReactionsE() : Principal.PUBLIC)
            .and(reaction.getViewE());
    }

}
