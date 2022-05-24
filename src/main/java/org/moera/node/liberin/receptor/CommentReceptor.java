package org.moera.node.liberin.receptor;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import javax.inject.Inject;

import org.moera.node.auth.principal.PrincipalFilter;
import org.moera.node.data.Comment;
import org.moera.node.data.EntryRevision;
import org.moera.node.data.Posting;
import org.moera.node.instant.CommentInstants;
import org.moera.node.liberin.LiberinMapping;
import org.moera.node.liberin.LiberinReceptor;
import org.moera.node.liberin.LiberinReceptorBase;
import org.moera.node.liberin.model.CommentAddedLiberin;
import org.moera.node.liberin.model.CommentDeletedLiberin;
import org.moera.node.liberin.model.CommentUpdatedLiberin;
import org.moera.node.model.AvatarImage;
import org.moera.node.model.body.Body;
import org.moera.node.model.event.CommentAddedEvent;
import org.moera.node.model.event.CommentDeletedEvent;
import org.moera.node.model.event.CommentUpdatedEvent;
import org.moera.node.model.event.PostingCommentsChangedEvent;
import org.moera.node.model.notification.MentionCommentAddedNotification;
import org.moera.node.model.notification.MentionCommentDeletedNotification;
import org.moera.node.model.notification.PostingCommentAddedNotification;
import org.moera.node.model.notification.PostingCommentDeletedNotification;
import org.moera.node.model.notification.PostingCommentsUpdatedNotification;
import org.moera.node.model.notification.ReplyCommentAddedNotification;
import org.moera.node.model.notification.ReplyCommentDeletedNotification;
import org.moera.node.notification.send.Directions;
import org.moera.node.text.MentionsExtractor;

@LiberinReceptor
public class CommentReceptor extends LiberinReceptorBase {

    @Inject
    private CommentInstants commentInstants;

    @LiberinMapping
    public void added(CommentAddedLiberin liberin) {
        Comment comment = liberin.getComment();
        Posting posting = liberin.getPosting();

        notifySubscribersCommentAdded(posting, comment);
        notifyReplyAdded(posting, comment);
        notifyMentioned(posting, comment.getId(), comment.getOwnerName(), comment.getOwnerFullName(),
                new AvatarImage(comment.getOwnerAvatarMediaFile(), comment.getOwnerAvatarShape()),
                comment.getCurrentRevision(), null);

        send(liberin, new CommentAddedEvent(comment, visibilityFilter(posting)));
        send(liberin, new PostingCommentsChangedEvent(posting, visibilityFilter(posting)));
        send(Directions.postingSubscribers(posting.getNodeId(), posting.getId(), visibilityFilter(posting)),
                new PostingCommentsUpdatedNotification(posting.getId(), posting.getTotalChildren()));
    }

    @LiberinMapping
    public void updated(CommentUpdatedLiberin liberin) {
        Comment comment = liberin.getComment();
        Posting posting = comment.getPosting();

        notifySubscribersCommentAdded(posting, comment);
        notifyReplyAdded(posting, comment);
        notifyMentioned(posting, comment.getId(), comment.getOwnerName(), comment.getOwnerFullName(),
                new AvatarImage(comment.getOwnerAvatarMediaFile(), comment.getOwnerAvatarShape()),
                comment.getCurrentRevision(), liberin.getLatestRevision());

        send(liberin, new CommentUpdatedEvent(comment, visibilityFilter(posting)));
    }

    @LiberinMapping
    public void deleted(CommentDeletedLiberin liberin) {
        Comment comment = liberin.getComment();
        Posting posting = comment.getPosting();

        commentInstants.deleted(comment);
        notifyReplyDeleted(posting, comment);
        notifyMentioned(posting, comment.getId(), comment.getOwnerName(), comment.getOwnerFullName(),
                new AvatarImage(comment.getOwnerAvatarMediaFile(), comment.getOwnerAvatarShape()), null,
                liberin.getLatestRevision());

        send(Directions.postingSubscribers(comment.getNodeId(), posting.getId(), visibilityFilter(posting)),
                new PostingCommentsUpdatedNotification(
                        posting.getId(), posting.getTotalChildren()));
        send(Directions.postingCommentsSubscribers(comment.getNodeId(), posting.getId(), visibilityFilter(posting)),
                new PostingCommentDeletedNotification(posting.getId(), comment.getId(), comment.getOwnerName(),
                        comment.getOwnerFullName(),
                        new AvatarImage(comment.getOwnerAvatarMediaFile(), comment.getOwnerAvatarShape())));

        send(liberin, new CommentDeletedEvent(comment, visibilityFilter(posting)));
        send(liberin, new PostingCommentsChangedEvent(posting, visibilityFilter(posting)));
    }

    private PrincipalFilter visibilityFilter(Posting posting) {
        return posting.getViewPrincipalAbsolute().a().and(posting.getViewCommentsPrincipalAbsolute());
    }

    private void notifySubscribersCommentAdded(Posting posting, Comment comment) {
        if (comment.getCurrentRevision().getSignature() != null) {
            UUID repliedToId = comment.getRepliedTo() != null ? comment.getRepliedTo().getId() : null;
            send(Directions.postingCommentsSubscribers(posting.getNodeId(), posting.getId(), visibilityFilter(posting)),
                    new PostingCommentAddedNotification(posting.getId(), posting.getCurrentRevision().getHeading(),
                            comment.getId(), comment.getOwnerName(), comment.getOwnerFullName(),
                            new AvatarImage(comment.getOwnerAvatarMediaFile(), comment.getOwnerAvatarShape()),
                            comment.getCurrentRevision().getHeading(), repliedToId));
            commentInstants.added(comment);
        }
    }

    private void notifyReplyAdded(Posting posting, Comment comment) {
        if (comment.getRepliedTo() == null || comment.getCurrentRevision().getSignature() == null
                || comment.getRevisions().size() > 1) {
            return;
        }
        send(Directions.single(comment.getNodeId(), comment.getRepliedToName(), visibilityFilter(posting)),
                new ReplyCommentAddedNotification(posting.getId(), comment.getId(), comment.getRepliedTo().getId(),
                        posting.getCurrentRevision().getHeading(), comment.getOwnerName(), comment.getOwnerFullName(),
                        new AvatarImage(comment.getOwnerAvatarMediaFile(), comment.getOwnerAvatarShape()),
                        comment.getCurrentRevision().getHeading(), comment.getRepliedToHeading()));
    }

    private void notifyReplyDeleted(Posting posting, Comment comment) {
        if (comment.getRepliedTo() == null || comment.getCurrentRevision().getSignature() == null) {
            return;
        }
        send(Directions.single(comment.getNodeId(), comment.getRepliedToName(), visibilityFilter(posting)),
                new ReplyCommentDeletedNotification(posting.getId(), comment.getId(), comment.getRepliedTo().getId(),
                        comment.getOwnerName(), comment.getOwnerFullName(),
                        new AvatarImage(comment.getOwnerAvatarMediaFile(), comment.getOwnerAvatarShape())));
    }

    private void notifyMentioned(Posting posting, UUID commentId, String ownerName, String ownerFullName,
                                 AvatarImage ownerAvatar, EntryRevision current, EntryRevision latest) {
        // TODO it is better to do this only for signed revisions. But it this case 'latest' should be the latest
        // signed revision
        String currentHeading = current != null ? current.getHeading() : null;
        Set<String> currentMentions = current != null
                ? MentionsExtractor.extract(new Body(current.getBody()))
                : Collections.emptySet();
        Set<String> latestMentions = latest != null
                ? MentionsExtractor.extract(new Body(latest.getBody()))
                : Collections.emptySet();
        currentMentions.stream()
                .filter(m -> !Objects.equals(ownerName, m))
                .filter(m -> !m.equals(":"))
                .filter(m -> !latestMentions.contains(m))
                .map(m -> Directions.single(posting.getNodeId(), m, visibilityFilter(posting)))
                .forEach(d -> send(d,
                        new MentionCommentAddedNotification(posting.getId(), commentId,
                                posting.getCurrentRevision().getHeading(), ownerName, ownerFullName, ownerAvatar,
                                currentHeading)));
        latestMentions.stream()
                .filter(m -> !Objects.equals(ownerName, m))
                .filter(m -> !m.equals(":"))
                .filter(m -> !currentMentions.contains(m))
                .map(m -> Directions.single(posting.getNodeId(), m, visibilityFilter(posting)))
                .forEach(d -> send(d, new MentionCommentDeletedNotification(posting.getId(), commentId)));
    }

}
