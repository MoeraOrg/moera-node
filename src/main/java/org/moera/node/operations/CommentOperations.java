package org.moera.node.operations;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
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

    private final MomentFinder momentFinder = new MomentFinder();

    public Comment newComment(Posting posting, CommentText commentText) {
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
        commentText.toEntry(comment);
        comment.setMoment(momentFinder.find(
                moment -> commentRepository.countMoments(posting.getId(), moment) == 0,
                Util.now()));

        posting.setChildrenTotal(posting.getChildrenTotal() + 1);

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

        notifyMentioned(posting, comment.getId(), current, latest);

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
            revision.setBodySrc(template.getBodySrc());
            revision.setBodySrcFormat(template.getBodySrcFormat());
            revision.setBody(template.getBody());
            revision.setHeading(template.getHeading());
        }

        return revision;
    }

    private void notifyMentioned(Posting posting, UUID commentId, EntryRevision current, EntryRevision latest) {
        Set<String> currentMentions = MentionsExtractor.extract(new Body(current.getBody()));
        Set<String> latestMentions = latest != null
                ? MentionsExtractor.extract(new Body(latest.getBody()))
                : Collections.emptySet();
        currentMentions.stream()
                .filter(m -> !requestContext.isClient(m))
                .filter(m -> !latestMentions.contains(m))
                .map(Directions::single)
                .forEach(d -> requestContext.send(d,
                        new MentionCommentAddedNotification(posting.getId(),
                                commentId, posting.getCurrentRevision().getHeading(), requestContext.getClientName(),
                                current.getHeading())));
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
        if (comment.getPosting().getChildrenTotal() > 0) {
            comment.getPosting().setChildrenTotal(comment.getPosting().getChildrenTotal() - 1);
        }

        requestContext.send(new CommentDeletedEvent(comment));
        requestContext.send(new PostingCommentsChangedEvent(comment.getPosting()));
        requestContext.send(Directions.postingSubscribers(comment.getPosting().getId()),
                new PostingCommentsUpdatedNotification(
                        comment.getPosting().getId(), comment.getPosting().getChildrenTotal()));
    }

}
