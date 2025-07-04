package org.moera.node.liberin.receptor;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import org.moera.lib.node.types.CommentInfo;
import org.moera.lib.node.types.PostingInfo;
import org.moera.lib.node.types.SearchContentUpdateType;
import org.moera.lib.node.types.body.Body;
import org.moera.lib.node.types.principal.AccessCheckers;
import org.moera.lib.node.types.principal.Principal;
import org.moera.lib.node.types.principal.PrincipalExpression;
import org.moera.lib.node.types.principal.PrincipalFilter;
import org.moera.node.data.Comment;
import org.moera.node.data.CommentRepository;
import org.moera.node.data.Entry;
import org.moera.node.data.EntryRevision;
import org.moera.node.data.Posting;
import org.moera.node.friends.FriendCache;
import org.moera.node.friends.SubscribedCache;
import org.moera.node.instant.CommentInstants;
import org.moera.node.liberin.LiberinMapping;
import org.moera.node.liberin.LiberinReceptor;
import org.moera.node.liberin.LiberinReceptorBase;
import org.moera.node.liberin.model.CommentAddedLiberin;
import org.moera.node.liberin.model.CommentDeletedLiberin;
import org.moera.node.liberin.model.CommentHeadingUpdatedLiberin;
import org.moera.node.liberin.model.CommentMediaTextUpdatedLiberin;
import org.moera.node.liberin.model.CommentUpdatedLiberin;
import org.moera.node.model.AvatarImageUtil;
import org.moera.node.model.CommentInfoUtil;
import org.moera.node.model.PostingInfoUtil;
import org.moera.node.model.event.CommentAddedEvent;
import org.moera.node.model.event.CommentDeletedEvent;
import org.moera.node.model.event.CommentUpdatedEvent;
import org.moera.node.model.event.PostingCommentsChangedEvent;
import org.moera.node.model.notification.MentionCommentAddedNotificationUtil;
import org.moera.node.model.notification.MentionCommentDeletedNotificationUtil;
import org.moera.node.model.notification.PostingCommentAddedNotificationUtil;
import org.moera.node.model.notification.PostingCommentDeletedNotificationUtil;
import org.moera.node.model.notification.PostingCommentsUpdatedNotificationUtil;
import org.moera.node.model.notification.ReplyCommentAddedNotificationUtil;
import org.moera.node.model.notification.ReplyCommentDeletedNotificationUtil;
import org.moera.node.model.notification.SearchContentUpdatedNotificationUtil;
import org.moera.node.notification.send.Directions;
import org.moera.node.operations.MediaAttachmentsProvider;
import org.moera.node.operations.UserListOperations;
import org.moera.node.text.MentionsExtractor;
import org.moera.node.util.Transaction;

@LiberinReceptor
public class CommentReceptor extends LiberinReceptorBase {

    @Inject
    private CommentRepository commentRepository;

    @Inject
    private CommentInstants commentInstants;

    @Inject
    private FriendCache friendCache;

    @Inject
    private SubscribedCache subscribedCache;

    @Inject
    private UserListOperations userListOperations;

    @Inject
    private Transaction tx;

    @Inject
    private EntityManager entityManager;

    @LiberinMapping
    public void added(CommentAddedLiberin liberin) {
        Comment comment = liberin.getComment();
        Posting posting = liberin.getPosting();

        notifySubscribersCommentAdded(posting, comment);
        notifyReplyAdded(posting, comment);
        notifyMentioned(
            posting,
            comment,
            comment.getCurrentRevision(),
            comment.getViewE(),
            null,
            Principal.PUBLIC
        );

        send(liberin, new CommentAddedEvent(comment, visibilityFilter(posting, comment)));
        send(liberin, new PostingCommentsChangedEvent(posting, generalVisibilityFilter(posting)));
        send(
            Directions.postingSubscribers(posting.getNodeId(), posting.getId(), generalVisibilityFilter(posting)),
            PostingCommentsUpdatedNotificationUtil.build(posting.getId(), posting.getTotalChildren())
        );
        send(
            Directions.searchSubscribers(posting.getNodeId(), visibilityFilter(posting, comment)),
            SearchContentUpdatedNotificationUtil.buildCommentUpdate(SearchContentUpdateType.COMMENT_ADD, comment)
        );
    }

    @LiberinMapping
    public void updated(CommentUpdatedLiberin liberin) {
        Comment comment = liberin.getComment();
        Entry posting = comment.getPosting();

        notifySubscribersCommentAdded(posting, comment);
        notifyReplyAdded(posting, comment);
        notifyMentioned(
            posting,
            comment,
            comment.getCurrentRevision(),
            comment.getViewE(),
            liberin.getLatestRevision(),
            liberin.getLatestViewE()
        );

        // TODO other notifications should also be sent here
        PrincipalExpression addedFilter = visibilityFilter(posting, comment).a().andNot(liberin.getLatestViewE());
        send(
            Directions.searchSubscribers(posting.getNodeId(), addedFilter),
            SearchContentUpdatedNotificationUtil.buildCommentUpdate(SearchContentUpdateType.COMMENT_ADD, comment)
        );
        send(liberin, new CommentAddedEvent(comment, addedFilter));

        PrincipalExpression updatedFilter = visibilityFilter(posting, comment).a().and(liberin.getLatestViewE());
        send(
            Directions.searchSubscribers(posting.getNodeId(), updatedFilter),
            SearchContentUpdatedNotificationUtil.buildCommentUpdate(SearchContentUpdateType.COMMENT_UPDATE, comment)
        );
        send(liberin, new CommentUpdatedEvent(comment, updatedFilter));

        PrincipalExpression deletedFilter = visibilityFilter(posting, comment).a().not().and(liberin.getLatestViewE());
        send(
            Directions.postingCommentsSubscribers(comment.getNodeId(), posting.getId(), deletedFilter),
            PostingCommentDeletedNotificationUtil.build(
                posting.getId(),
                comment.getId(),
                comment.getOwnerName(),
                comment.getOwnerFullName(),
                comment.getOwnerGender(),
                AvatarImageUtil.build(comment.getOwnerAvatarMediaFile(), comment.getOwnerAvatarShape())
            )
        );
        send(
            Directions.searchSubscribers(posting.getNodeId(), deletedFilter),
            SearchContentUpdatedNotificationUtil.buildCommentUpdate(SearchContentUpdateType.COMMENT_DELETE, comment)
        );
        send(liberin, new CommentDeletedEvent(comment, deletedFilter));
    }

    @LiberinMapping
    public void deleted(CommentDeletedLiberin liberin) {
        Comment comment = liberin.getComment();
        Entry posting = comment.getPosting();

        commentInstants.deleted(comment);
        notifyReplyDeleted(posting, comment);
        notifyMentioned(
            posting,
            comment,
            null,
            Principal.PUBLIC,
            liberin.getLatestRevision(),
            comment.getViewE()
        );

        send(
            Directions.postingSubscribers(comment.getNodeId(), posting.getId(), generalVisibilityFilter(posting)),
            PostingCommentsUpdatedNotificationUtil.build(posting.getId(), posting.getTotalChildren())
        );
        send(
            Directions.postingCommentsSubscribers(
                comment.getNodeId(), posting.getId(), visibilityFilter(posting, comment)
            ),
            PostingCommentDeletedNotificationUtil.build(
                posting.getId(),
                comment.getId(),
                comment.getOwnerName(),
                comment.getOwnerFullName(),
                comment.getOwnerGender(),
                AvatarImageUtil.build(comment.getOwnerAvatarMediaFile(), comment.getOwnerAvatarShape())
            )
        );
        send(
            Directions.searchSubscribers(posting.getNodeId(), visibilityFilter(posting, comment)),
            SearchContentUpdatedNotificationUtil.buildCommentUpdate(SearchContentUpdateType.COMMENT_DELETE, comment)
        );

        send(liberin, new CommentDeletedEvent(comment, visibilityFilter(posting, comment)));
        send(liberin, new PostingCommentsChangedEvent(posting, generalVisibilityFilter(posting)));
    }

    private void notifySubscribersCommentAdded(Entry posting, Comment comment) {
        if (comment.getCurrentRevision().getSignature() != null) {
            tx.executeWriteQuietly(() -> {
                Entry aposting = entityManager.merge(posting);
                Comment acomment = entityManager.merge(comment);
                PostingInfo postingInfo = PostingInfoUtil.build(
                    aposting,
                    aposting.getStories(),
                    MediaAttachmentsProvider.NONE,
                    AccessCheckers.ADMIN,
                    universalContext.getOptions()
                );
                userListOperations.fillSheriffListMarks(postingInfo);
                CommentInfo commentInfo = CommentInfoUtil.build(
                    acomment, MediaAttachmentsProvider.NONE, AccessCheckers.ADMIN
                );
                userListOperations.fillSheriffListMarks(aposting, commentInfo);
                UUID repliedToId = comment.getRepliedTo() != null ? comment.getRepliedTo().getId() : null;
                send(
                    Directions.postingCommentsSubscribers(
                        posting.getNodeId(), posting.getId(), visibilityFilter(posting, comment)
                    ),
                    PostingCommentAddedNotificationUtil.build(
                        posting.getOwnerName(),
                        posting.getOwnerFullName(),
                        posting.getOwnerGender(),
                        postingInfo.getOwnerAvatar(),
                        posting.getId(),
                        postingInfo.getHeading(),
                        postingInfo.getSheriffs(),
                        postingInfo.getSheriffMarks(),
                        comment.getId(),
                        comment.getOwnerName(),
                        comment.getOwnerFullName(),
                        comment.getOwnerGender(),
                        commentInfo.getOwnerAvatar(),
                        commentInfo.getHeading(),
                        commentInfo.getSheriffMarks(),
                        repliedToId
                    )
                );
                commentInstants.added(comment);
            });
        }
    }

    private void notifyReplyAdded(Entry posting, Comment comment) {
        if (
            comment.getRepliedTo() == null
            || comment.getCurrentRevision().getSignature() == null
            || comment.getTotalRevisions() > 1
        ) {
            return;
        }
        tx.executeWriteQuietly(() -> {
            Entry aposting = entityManager.merge(posting);
            Comment acomment = entityManager.merge(comment);
            PostingInfo postingInfo = PostingInfoUtil.build(
                aposting,
                aposting.getStories(),
                MediaAttachmentsProvider.NONE,
                AccessCheckers.ADMIN,
                universalContext.getOptions()
            );
            userListOperations.fillSheriffListMarks(postingInfo);
            CommentInfo commentInfo = CommentInfoUtil.build(
                acomment, MediaAttachmentsProvider.NONE, AccessCheckers.ADMIN
            );
            userListOperations.fillSheriffListMarks(aposting, commentInfo);
            send(
                Directions.single(
                    acomment.getNodeId(), acomment.getRepliedToName(), visibilityFilter(aposting, acomment)
                ),
                ReplyCommentAddedNotificationUtil.build(
                    aposting.getOwnerName(),
                    aposting.getOwnerFullName(),
                    aposting.getOwnerGender(),
                    postingInfo.getOwnerAvatar(),
                    aposting.getId(),
                    acomment.getId(),
                    acomment.getRepliedTo().getId(),
                    postingInfo.getHeading(),
                    postingInfo.getSheriffs(),
                    postingInfo.getSheriffMarks(),
                    acomment.getOwnerName(),
                    acomment.getOwnerFullName(),
                    acomment.getOwnerGender(),
                    commentInfo.getOwnerAvatar(),
                    commentInfo.getHeading(),
                    commentInfo.getSheriffMarks(),
                    acomment.getRepliedToHeading()
                )
            );
        });
    }

    private void notifyReplyDeleted(Entry posting, Comment comment) {
        if (comment.getRepliedTo() == null || comment.getCurrentRevision().getSignature() == null) {
            return;
        }
        send(
            Directions.single(comment.getNodeId(), comment.getRepliedToName(), visibilityFilter(posting, comment)),
            ReplyCommentDeletedNotificationUtil.build(
                posting.getId(),
                comment.getId(),
                comment.getRepliedTo().getId(),
                comment.getOwnerName(),
                comment.getOwnerFullName(),
                comment.getOwnerGender(),
                AvatarImageUtil.build(comment.getOwnerAvatarMediaFile(), comment.getOwnerAvatarShape())
            )
        );
    }

    private void notifyMentioned(
        Entry posting,
        Comment comment,
        EntryRevision current,
        Principal currentView,
        EntryRevision latest,
        Principal latestView
    ) {
        // TODO it is better to do this only for signed revisions. But it this case 'latest' should be the latest
        // signed revision
        String ownerName = comment.getOwnerName();
        String ownerFullName = comment.getOwnerFullName();
        Set<String> currentMentions = current != null
            ? filterMentions(MentionsExtractor.extract(new Body(current.getBody())), ownerName, currentView)
            : Collections.emptySet();
        Set<String> latestMentions = latest != null
            ? filterMentions(MentionsExtractor.extract(new Body(latest.getBody())), ownerName, latestView)
            : Collections.emptySet();
        if (!currentMentions.isEmpty()) {
            PostingInfo postingInfo = PostingInfoUtil.build(
                posting,
                posting.getStories(),
                MediaAttachmentsProvider.NONE,
                AccessCheckers.ADMIN,
                universalContext.getOptions()
            );
            userListOperations.fillSheriffListMarks(postingInfo);
            CommentInfo commentInfo = CommentInfoUtil.build(
                comment, MediaAttachmentsProvider.NONE, AccessCheckers.ADMIN
            );
            userListOperations.fillSheriffListMarks(posting, commentInfo);
            currentMentions.stream()
                .filter(m -> !latestMentions.contains(m))
                .map(m -> Directions.single(posting.getNodeId(), m))
                .forEach(d ->
                    send(
                        d,
                        MentionCommentAddedNotificationUtil.build(
                            posting.getOwnerName(),
                            posting.getOwnerFullName(),
                            posting.getOwnerGender(),
                            postingInfo.getOwnerAvatar(),
                            posting.getId(),
                            comment.getId(),
                            postingInfo.getHeading(),
                            postingInfo.getSheriffs(),
                            postingInfo.getSheriffMarks(),
                            ownerName,
                            ownerFullName,
                            comment.getOwnerGender(),
                            commentInfo.getOwnerAvatar(),
                            commentInfo.getHeading(),
                            commentInfo.getSheriffMarks()
                        )
                    )
                );
        }
        latestMentions.stream()
            .filter(m -> !currentMentions.contains(m))
            .map(m -> Directions.single(posting.getNodeId(), m))
            .forEach(d -> send(d, MentionCommentDeletedNotificationUtil.build(posting.getId(), comment.getId())));
    }

    private Set<String> filterMentions(Set<String> mentions, String ownerName, Principal view) {
        return mentions.stream()
            .filter(m -> !Objects.equals(ownerName, m))
            .filter(m -> !m.equals(":"))
            .filter(m ->
                view.includes(
                    false,
                    m,
                    () -> subscribedCache.isSubscribed(m),
                    () -> friendCache.getClientGroupIds(m)
                )
            )
            .collect(Collectors.toSet());
    }

    private PrincipalExpression generalVisibilityFilter(Entry posting) {
        return posting.getViewE().a().and(posting.getViewCommentsE());
    }

    private PrincipalFilter visibilityFilter(Entry posting, Comment comment) {
        return generalVisibilityFilter(posting).and(comment.getViewE());
    }

    @LiberinMapping
    public void headingUpdated(CommentHeadingUpdatedLiberin liberin) {
        Comment comment = commentRepository.findByNodeIdAndId(liberin.getNodeId(), liberin.getCommentId()).orElse(null);
        if (comment == null) {
            return;
        }
        if (comment.getCurrentRevision().getId().equals(liberin.getRevisionId())) {
            send(
                Directions.searchSubscribers(liberin.getNodeId(), visibilityFilter(comment.getPosting(), comment)),
                SearchContentUpdatedNotificationUtil.buildCommentHeadingUpdate(
                    comment.getPosting().getId(), comment.getId(), liberin.getHeading()
                )
            );
        }
    }

    @LiberinMapping
    public void mediaTextUpdated(CommentMediaTextUpdatedLiberin liberin) {
        Comment comment = commentRepository.findByNodeIdAndId(liberin.getNodeId(), liberin.getCommentId()).orElse(null);
        if (comment == null) {
            return;
        }
        send(
            Directions.searchSubscribers(liberin.getNodeId(), visibilityFilter(comment.getPosting(), comment)),
            SearchContentUpdatedNotificationUtil.buildCommentMediaTextUpdate(
                comment.getPosting().getId(), comment.getId(), liberin.getMediaId(), liberin.getTextContent()
            )
        );
    }

}
