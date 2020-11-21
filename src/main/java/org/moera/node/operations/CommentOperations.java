package org.moera.node.operations;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Predicate;
import javax.inject.Inject;

import org.moera.node.data.Comment;
import org.moera.node.data.CommentRepository;
import org.moera.node.data.EntryRevision;
import org.moera.node.data.EntryRevisionRepository;
import org.moera.node.data.Posting;
import org.moera.node.global.RequestContext;
import org.moera.node.model.AcceptedReactions;
import org.moera.node.model.Body;
import org.moera.node.model.CommentText;
import org.moera.node.model.event.CommentDeletedEvent;
import org.moera.node.model.event.PostingCommentsChangedEvent;
import org.moera.node.model.notification.MentionCommentAddedNotification;
import org.moera.node.model.notification.MentionCommentDeletedNotification;
import org.moera.node.model.notification.PostingCommentsUpdatedNotification;
import org.moera.node.model.notification.ReplyCommentAddedNotification;
import org.moera.node.model.notification.ReplyCommentDeletedNotification;
import org.moera.node.notification.send.Directions;
import org.moera.node.text.MentionsExtractor;
import org.moera.node.util.MomentFinder;
import org.moera.node.util.Util;
import org.springframework.stereotype.Component;

@Component
public class CommentOperations {

    public static final int MAX_COMMENTS_PER_REQUEST = 200;
    private static final Duration UNSIGNED_TTL = Duration.of(15, ChronoUnit.MINUTES);

    @Inject
    private RequestContext requestContext;

    @Inject
    private CommentRepository commentRepository;

    @Inject
    private EntryRevisionRepository entryRevisionRepository;

    @Inject
    private CommentPublicPageOperations commentPublicPageOperations;

    private final MomentFinder momentFinder = new MomentFinder();

    public Comment newComment(Posting posting, CommentText commentText, Comment repliedTo) {
        if (commentText.getAcceptedReactions() == null) {
            commentText.setAcceptedReactions(new AcceptedReactions());
        }
        if (commentText.getAcceptedReactions().getPositive() == null) {
            commentText.getAcceptedReactions().setPositive("");
        }
        if (commentText.getAcceptedReactions().getNegative() == null) {
            commentText.getAcceptedReactions().setNegative("");
        }

        Comment comment = new Comment();
        comment.setId(UUID.randomUUID());
        comment.setNodeId(requestContext.nodeId());
        comment.setOwnerName(commentText.getOwnerName());
        comment.setPosting(posting);
        if (repliedTo != null) {
            comment.setRepliedTo(repliedTo);
            comment.setRepliedToRevision(repliedTo.getCurrentRevision());
            comment.setRepliedToName(repliedTo.getOwnerName());
            comment.setRepliedToHeading(repliedTo.getCurrentRevision().getHeading());
            comment.setRepliedToDigest(repliedTo.getCurrentRevision().getDigest());
        }
        commentText.toEntry(comment);
        comment.setMoment(momentFinder.find(
                moment -> commentRepository.countMoments(posting.getId(), moment) == 0,
                Util.now()));

        posting.setTotalChildren(posting.getTotalChildren() + 1);

        return commentRepository.save(comment);
    }

    public Comment createOrUpdateComment(Posting posting, Comment comment, EntryRevision revision,
                                         Predicate<EntryRevision> isNothingChanged,
                                         Consumer<EntryRevision> revisionUpdater) {
        EntryRevision latest = comment.getCurrentRevision();
        if (latest != null) {
            if (isNothingChanged != null && isNothingChanged.test(latest)) {
                return commentRepository.saveAndFlush(comment);
            }
            if (latest.getSignature() == null) {
                comment.removeRevision(latest);
                comment.setTotalRevisions(comment.getTotalRevisions() - 1);
                comment.setCurrentRevision(null);
                entryRevisionRepository.delete(latest);
                latest = null;
            }
        }

        EntryRevision current = newCommentRevision(comment, revision);
        current.setParent(posting.getCurrentRevision());
        if (revisionUpdater != null) {
            revisionUpdater.accept(current);
        }
        comment.setEditedAt(Util.now());
        if (current.getSignature() == null) {
            current.setDeadline(Timestamp.from(Instant.now().plus(UNSIGNED_TTL)));
        }
        comment = commentRepository.saveAndFlush(comment);
        commentPublicPageOperations.updatePublicPages(comment.getPosting().getId(), comment.getMoment());
        notifyReplyAdded(posting, comment);
        notifyMentioned(posting, comment.getId(), comment.getOwnerName(), current, latest);

        return comment;
    }

    private EntryRevision newCommentRevision(Comment comment, EntryRevision template) {
        EntryRevision revision;

        if (template == null) {
            revision = newRevision(comment, null);
            comment.setTotalRevisions(1);
        } else {
            revision = newRevision(comment, template);
            if (comment.getCurrentRevision() != null && comment.getCurrentRevision().getDeletedAt() == null) {
                comment.getCurrentRevision().setDeletedAt(Util.now());
            }
            comment.setTotalRevisions(comment.getTotalRevisions() + 1);
        }
        comment.setCurrentRevision(revision);

        return revision;
    }

    private EntryRevision newRevision(Comment comment, EntryRevision template) {
        EntryRevision revision = new EntryRevision();
        revision.setId(UUID.randomUUID());
        revision.setEntry(comment);
        revision = entryRevisionRepository.save(revision);
        comment.addRevision(revision);

        if (template != null) {
            revision.setBodyPreview(template.getBodyPreview());
            revision.setSaneBodyPreview(template.getSaneBodyPreview());
            revision.setBodySrc(template.getBodySrc());
            revision.setBodySrcFormat(template.getBodySrcFormat());
            revision.setBody(template.getBody());
            revision.setSaneBody(template.getSaneBody());
            revision.setHeading(template.getHeading());
        }

        return revision;
    }

    private void notifyReplyAdded(Posting posting, Comment comment) {
        if (comment.getRepliedTo() == null || comment.getCurrentRevision().getSignature() == null
                || comment.getRevisions().size() > 1) {
            return;
        }
        requestContext.send(Directions.single(comment.getRepliedToName()),
                new ReplyCommentAddedNotification(posting.getId(), comment.getId(), comment.getRepliedTo().getId(),
                        posting.getCurrentRevision().getHeading(), comment.getOwnerName(),
                        comment.getCurrentRevision().getHeading(), comment.getRepliedToHeading()));
    }

    private void notifyReplyDeleted(Posting posting, Comment comment) {
        if (comment.getRepliedTo() == null || comment.getCurrentRevision().getSignature() == null) {
            return;
        }
        requestContext.send(Directions.single(comment.getRepliedToName()),
                new ReplyCommentDeletedNotification(posting.getId(), comment.getId(), comment.getRepliedTo().getId(),
                        comment.getOwnerName()));
    }

    private void notifyMentioned(Posting posting, UUID commentId, String ownerName, EntryRevision current,
                                 EntryRevision latest) {
        Set<String> currentMentions = MentionsExtractor.extract(new Body(current.getBody()));
        Set<String> latestMentions = latest != null
                ? MentionsExtractor.extract(new Body(latest.getBody()))
                : Collections.emptySet();
        notifyMentioned(posting, commentId, ownerName, current.getHeading(), currentMentions, latestMentions);
    }

    private void notifyMentioned(Posting posting, UUID commentId, String ownerName, String currentHeading,
                                 Set<String> currentMentions, Set<String> latestMentions) {
        currentMentions.stream()
                .filter(m -> !Objects.equals(ownerName, m))
                .filter(m -> !latestMentions.contains(m))
                .map(Directions::single)
                .forEach(d -> requestContext.send(d,
                        new MentionCommentAddedNotification(posting.getId(), commentId,
                                posting.getCurrentRevision().getHeading(), ownerName, currentHeading)));
        latestMentions.stream()
                .filter(m -> !m.equals(requestContext.nodeName()))
                .filter(m -> !currentMentions.contains(m))
                .map(Directions::single)
                .forEach(d -> requestContext.send(d,
                        new MentionCommentDeletedNotification(posting.getId(), commentId)));
    }

    public void deleteComment(Comment comment) {
        comment.setDeletedAt(Util.now());
        Duration postingTtl = requestContext.getOptions().getDuration("comment.deleted.lifetime");
        comment.setDeadline(Timestamp.from(Instant.now().plus(postingTtl)));
        comment.getCurrentRevision().setDeletedAt(Util.now());
        if (comment.getPosting().getTotalChildren() > 0) {
            comment.getPosting().setTotalChildren(comment.getPosting().getTotalChildren() - 1);
        }

        Set<String> latestMentions = MentionsExtractor.extract(new Body(comment.getCurrentRevision().getBody()));
        notifyReplyDeleted(comment.getPosting(), comment);
        notifyMentioned(comment.getPosting(), comment.getId(), requestContext.getClientName(), null,
                Collections.emptySet(), latestMentions);

        requestContext.send(new CommentDeletedEvent(comment));
        requestContext.send(new PostingCommentsChangedEvent(comment.getPosting()));
        requestContext.send(Directions.postingSubscribers(comment.getPosting().getId()),
                new PostingCommentsUpdatedNotification(
                        comment.getPosting().getId(), comment.getPosting().getTotalChildren()));
    }

}
