package org.moera.node.liberin.receptor;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.inject.Inject;

import org.moera.node.auth.principal.Principal;
import org.moera.node.auth.principal.PrincipalExpression;
import org.moera.node.auth.principal.PrincipalFilter;
import org.moera.node.data.Comment;
import org.moera.node.data.EntryRevision;
import org.moera.node.data.Posting;
import org.moera.node.friends.FriendsCache;
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

    @Inject
    private FriendsCache friendsCache;

    @LiberinMapping
    public void added(CommentAddedLiberin liberin) {
        Comment comment = liberin.getComment();
        Posting posting = liberin.getPosting();

        notifySubscribersCommentAdded(posting, comment);
        notifyReplyAdded(posting, comment);
        notifyMentioned(posting, comment, comment.getCurrentRevision(), comment.getViewE(), null,
                Principal.PUBLIC);

        send(liberin, new CommentAddedEvent(comment, visibilityFilter(posting, comment)));
        send(liberin, new PostingCommentsChangedEvent(posting, generalVisibilityFilter(posting)));
        send(Directions.postingSubscribers(posting.getNodeId(), posting.getId(), generalVisibilityFilter(posting)),
                new PostingCommentsUpdatedNotification(posting.getId(), posting.getTotalChildren()));
    }

    @LiberinMapping
    public void updated(CommentUpdatedLiberin liberin) {
        Comment comment = liberin.getComment();
        Posting posting = comment.getPosting();

        notifySubscribersCommentAdded(posting, comment);
        notifyReplyAdded(posting, comment);
        notifyMentioned(posting, comment, comment.getCurrentRevision(), comment.getViewE(),
                liberin.getLatestRevision(), liberin.getLatestViewE());

        send(liberin, new CommentUpdatedEvent(comment, visibilityFilter(posting, comment)));
    }

    @LiberinMapping
    public void deleted(CommentDeletedLiberin liberin) {
        Comment comment = liberin.getComment();
        Posting posting = comment.getPosting();

        commentInstants.deleted(comment);
        notifyReplyDeleted(posting, comment);
        notifyMentioned(posting, comment, null, Principal.PUBLIC, liberin.getLatestRevision(),
                comment.getViewE());

        send(Directions.postingSubscribers(comment.getNodeId(), posting.getId(), generalVisibilityFilter(posting)),
                new PostingCommentsUpdatedNotification(
                        posting.getId(), posting.getTotalChildren()));
        send(Directions.postingCommentsSubscribers(comment.getNodeId(), posting.getId(),
                        visibilityFilter(posting, comment)),
                new PostingCommentDeletedNotification(posting.getId(), comment.getId(), comment.getOwnerName(),
                        comment.getOwnerFullName(), comment.getOwnerGender(),
                        new AvatarImage(comment.getOwnerAvatarMediaFile(), comment.getOwnerAvatarShape())));

        send(liberin, new CommentDeletedEvent(comment, visibilityFilter(posting, comment)));
        send(liberin, new PostingCommentsChangedEvent(posting, generalVisibilityFilter(posting)));
    }

    private void notifySubscribersCommentAdded(Posting posting, Comment comment) {
        if (comment.getCurrentRevision().getSignature() != null) {
            UUID repliedToId = comment.getRepliedTo() != null ? comment.getRepliedTo().getId() : null;
            AvatarImage postingOwnerAvatar = new AvatarImage(posting.getOwnerAvatarMediaFile(),
                    posting.getOwnerAvatarShape());
            AvatarImage commentOwnerAvatar = new AvatarImage(comment.getOwnerAvatarMediaFile(),
                    comment.getOwnerAvatarShape());
            send(Directions.postingCommentsSubscribers(posting.getNodeId(), posting.getId(),
                            visibilityFilter(posting, comment)),
                    new PostingCommentAddedNotification(posting.getOwnerName(), posting.getOwnerFullName(),
                            posting.getOwnerGender(), postingOwnerAvatar, posting.getId(),
                            posting.getCurrentRevision().getHeading(), comment.getId(), comment.getOwnerName(),
                            comment.getOwnerFullName(), comment.getOwnerGender(), commentOwnerAvatar,
                            comment.getCurrentRevision().getHeading(), repliedToId));
            commentInstants.added(comment);
        }
    }

    private void notifyReplyAdded(Posting posting, Comment comment) {
        if (comment.getRepliedTo() == null || comment.getCurrentRevision().getSignature() == null
                || comment.getRevisions().size() > 1) {
            return;
        }
        AvatarImage postingOwnerAvatar = new AvatarImage(posting.getOwnerAvatarMediaFile(),
                posting.getOwnerAvatarShape());
        AvatarImage commentOwnerAvatar = new AvatarImage(comment.getOwnerAvatarMediaFile(),
                comment.getOwnerAvatarShape());
        send(Directions.single(comment.getNodeId(), comment.getRepliedToName(), visibilityFilter(posting, comment)),
                new ReplyCommentAddedNotification(posting.getOwnerName(), posting.getOwnerFullName(),
                        posting.getOwnerGender(), postingOwnerAvatar, posting.getId(), comment.getId(),
                        comment.getRepliedTo().getId(), posting.getCurrentRevision().getHeading(),
                        comment.getOwnerName(), comment.getOwnerFullName(), comment.getOwnerGender(),
                        commentOwnerAvatar, comment.getCurrentRevision().getHeading(), comment.getRepliedToHeading()));
    }

    private void notifyReplyDeleted(Posting posting, Comment comment) {
        if (comment.getRepliedTo() == null || comment.getCurrentRevision().getSignature() == null) {
            return;
        }
        send(Directions.single(comment.getNodeId(), comment.getRepliedToName(), visibilityFilter(posting, comment)),
                new ReplyCommentDeletedNotification(posting.getId(), comment.getId(), comment.getRepliedTo().getId(),
                        comment.getOwnerName(), comment.getOwnerFullName(), comment.getOwnerGender(),
                        new AvatarImage(comment.getOwnerAvatarMediaFile(), comment.getOwnerAvatarShape())));
    }

    private void notifyMentioned(Posting posting, Comment comment, EntryRevision current, Principal currentView,
                                 EntryRevision latest, Principal latestView) {
        // TODO it is better to do this only for signed revisions. But it this case 'latest' should be the latest
        // signed revision
        AvatarImage postingOwnerAvatar = new AvatarImage(posting.getOwnerAvatarMediaFile(),
                posting.getOwnerAvatarShape());
        String ownerName = comment.getOwnerName();
        String ownerFullName = comment.getOwnerFullName();
        AvatarImage ownerAvatar = new AvatarImage(comment.getOwnerAvatarMediaFile(), comment.getOwnerAvatarShape());
        String currentHeading = current != null ? current.getHeading() : null;
        Set<String> currentMentions = current != null
                ? filterMentions(MentionsExtractor.extract(new Body(current.getBody())), ownerName, currentView)
                : Collections.emptySet();
        Set<String> latestMentions = latest != null
                ? filterMentions(MentionsExtractor.extract(new Body(latest.getBody())), ownerName, latestView)
                : Collections.emptySet();
        currentMentions.stream()
                .filter(m -> !latestMentions.contains(m))
                .map(m -> Directions.single(posting.getNodeId(), m))
                .forEach(d -> send(d,
                        new MentionCommentAddedNotification(posting.getOwnerName(), posting.getOwnerFullName(),
                                posting.getOwnerGender(), postingOwnerAvatar, posting.getId(), comment.getId(),
                                posting.getCurrentRevision().getHeading(), ownerName, ownerFullName,
                                comment.getOwnerGender(), ownerAvatar, currentHeading)));
        latestMentions.stream()
                .filter(m -> !currentMentions.contains(m))
                .map(m -> Directions.single(posting.getNodeId(), m))
                .forEach(d -> send(d, new MentionCommentDeletedNotification(posting.getId(), comment.getId())));
    }

    private Set<String> filterMentions(Set<String> mentions, String ownerName, Principal view) {
        return mentions.stream()
                .filter(m -> !Objects.equals(ownerName, m))
                .filter(m -> !m.equals(":"))
                .filter(m -> view.includes(false, m, () -> friendsCache.getFriends(m)))
                .collect(Collectors.toSet());
    }

    private PrincipalExpression generalVisibilityFilter(Posting posting) {
        return posting.getViewE().a().and(posting.getViewCommentsE());
    }

    private PrincipalFilter visibilityFilter(Posting posting, Comment comment) {
        return generalVisibilityFilter(posting).and(comment.getViewE());
    }

}
