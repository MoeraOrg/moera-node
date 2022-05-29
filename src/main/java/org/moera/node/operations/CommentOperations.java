package org.moera.node.operations;

import java.security.interfaces.ECPrivateKey;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Predicate;
import javax.inject.Inject;

import org.moera.commons.crypto.CryptoUtil;
import org.moera.commons.util.LogUtil;
import org.moera.node.data.Comment;
import org.moera.node.data.CommentRepository;
import org.moera.node.data.Entry;
import org.moera.node.data.EntryAttachment;
import org.moera.node.data.EntryAttachmentRepository;
import org.moera.node.data.EntryRevision;
import org.moera.node.data.EntryRevisionRepository;
import org.moera.node.data.MediaFileOwner;
import org.moera.node.data.Posting;
import org.moera.node.fingerprint.CommentFingerprint;
import org.moera.node.global.RequestContext;
import org.moera.node.liberin.Liberin;
import org.moera.node.liberin.LiberinManager;
import org.moera.node.liberin.model.CommentDeletedLiberin;
import org.moera.node.liberin.model.CommentUpdatedLiberin;
import org.moera.node.model.CommentText;
import org.moera.node.model.body.Body;
import org.moera.node.text.MediaExtractor;
import org.moera.node.util.ExtendedDuration;
import org.moera.node.util.MomentFinder;
import org.moera.node.util.Transaction;
import org.moera.node.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;

@Component
public class CommentOperations {

    public static final int MAX_COMMENTS_PER_REQUEST = 200;
    private static final Duration UNSIGNED_TTL = Duration.of(15, ChronoUnit.MINUTES);

    private static final Logger log = LoggerFactory.getLogger(CommentOperations.class);

    @Inject
    private RequestContext requestContext;

    @Inject
    private CommentRepository commentRepository;

    @Inject
    private EntryRevisionRepository entryRevisionRepository;

    @Inject
    private EntryAttachmentRepository entryAttachmentRepository;

    @Inject
    private CommentPublicPageOperations commentPublicPageOperations;

    @Inject
    private PlatformTransactionManager txManager;

    @Inject
    private LiberinManager liberinManager;

    private final MomentFinder momentFinder = new MomentFinder();

    public Comment newComment(Posting posting, CommentText commentText, Comment repliedTo) {
        commentText.initAcceptedReactionsDefaults();

        Comment comment = new Comment();
        comment.setId(UUID.randomUUID());
        comment.setNodeId(requestContext.nodeId());
        comment.setOwnerName(commentText.getOwnerName());
        comment.setOwnerFullName(commentText.getOwnerFullName());
        if (commentText.getOwnerAvatar() != null) {
            if (commentText.getOwnerAvatarMediaFile() != null) {
                comment.setOwnerAvatarMediaFile(commentText.getOwnerAvatarMediaFile());
            }
            if (commentText.getOwnerAvatar().getShape() != null) {
                comment.setOwnerAvatarShape(commentText.getOwnerAvatar().getShape());
            }
        }
        comment.setPosting(posting);
        if (repliedTo != null) {
            comment.setRepliedTo(repliedTo);
            comment.setRepliedToRevision(repliedTo.getCurrentRevision());
            comment.setRepliedToName(repliedTo.getOwnerName());
            comment.setRepliedToFullName(repliedTo.getOwnerFullName());
            if (repliedTo.getOwnerAvatarMediaFile() != null) {
                comment.setRepliedToAvatarMediaFile(repliedTo.getOwnerAvatarMediaFile());
                comment.setRepliedToAvatarShape(repliedTo.getOwnerAvatarShape());
            }
            comment.setRepliedToHeading(repliedTo.getCurrentRevision().getHeading());
            comment.setRepliedToDigest(repliedTo.getCurrentRevision().getDigest());
        }
        commentText.toEntry(comment);
        comment.setMoment(momentFinder.find(
                moment -> commentRepository.countMoments(posting.getId(), moment) == 0,
                Util.now()));

        log.debug("Total comments for posting {} = {} + 1: new comment {}",
                LogUtil.format(posting.getId()),
                LogUtil.format(posting.getTotalChildren()),
                LogUtil.format(comment.getId()));
        posting.setTotalChildren(posting.getTotalChildren() + 1);

        return commentRepository.save(comment);
    }

    public Comment createOrUpdateComment(Posting posting, Comment comment, EntryRevision revision,
                                         List<MediaFileOwner> media, Predicate<EntryRevision> isNothingChanged,
                                         Consumer<EntryRevision> revisionUpdater,
                                         Consumer<Entry> mediaEntryUpdater) {
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
            }
        }

        EntryRevision current = newCommentRevision(comment, revision);
        current.setParent(posting.getCurrentRevision());
        if (revisionUpdater != null) {
            revisionUpdater.accept(current);
        }

        if (media.size() > 0) {
            Set<String> embedded = MediaExtractor.extractMediaFileIds(new Body(current.getBody()));
            int ordinal = 0;
            for (MediaFileOwner mfo : media) {
                EntryAttachment attachment = new EntryAttachment(current, mfo, ordinal++);
                attachment.setEmbedded(embedded.contains(mfo.getMediaFile().getId()));
                attachment = entryAttachmentRepository.save(attachment);
                current.addAttachment(attachment);

                if (mediaEntryUpdater != null) {
                    Posting mediaPosting = mfo.getPosting(null);
                    if (mediaPosting != null) {
                        mediaEntryUpdater.accept(mediaPosting);
                    }
                }
            }
        }

        comment.setEditedAt(Util.now());
        comment = commentRepository.saveAndFlush(comment);
        signIfOwned(comment);

        commentPublicPageOperations.updatePublicPages(comment.getPosting().getId(), comment.getMoment());

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

    private void signIfOwned(Comment comment) {
        EntryRevision current = comment.getCurrentRevision();

        if (current.getSignature() == null) {
            if (comment.getOwnerName().equals(requestContext.nodeName())) {
                CommentFingerprint fingerprint = new CommentFingerprint(comment);
                current.setDigest(CryptoUtil.digest(fingerprint));
                current.setSignature(CryptoUtil.sign(fingerprint, getSigningKey()));
                current.setSignatureVersion(CommentFingerprint.VERSION);
            } else {
                current.setDeadline(Timestamp.from(Instant.now().plus(UNSIGNED_TTL)));
            }
        }
    }

    private ECPrivateKey getSigningKey() {
        return (ECPrivateKey) requestContext.getOptions().getPrivateKey("profile.signing-key");
    }

    public void deleteComment(Comment comment) {
        comment.setDeletedAt(Util.now());
        ExtendedDuration postingTtl = requestContext.getOptions().getDuration("comment.deleted.lifetime");
        if (!postingTtl.isNever()) {
            comment.setDeadline(Timestamp.from(Instant.now().plus(postingTtl.getDuration())));
        }
        comment.getCurrentRevision().setDeletedAt(Util.now());
        if (comment.getPosting().getTotalChildren() > 0) {
            log.debug("Total comments for posting {} = {} - 1: deleted comment {}",
                    LogUtil.format(comment.getPosting().getId()),
                    LogUtil.format(comment.getPosting().getTotalChildren()),
                    LogUtil.format(comment.getId()));
            comment.getPosting().setTotalChildren(comment.getPosting().getTotalChildren() - 1);
        } else {
            log.debug("Total comments for posting {} = 0 before deleting comment {}",
                    LogUtil.format(comment.getPosting().getId()),
                    LogUtil.format(comment.getId()));
        }
    }

    @Scheduled(fixedDelayString = "PT15M")
    public void purgeExpired() throws Throwable {
        List<Liberin> liberins = new ArrayList<>();

        Transaction.execute(txManager, () -> {
            List<Comment> comments = commentRepository.findExpiredUnsigned(Util.now());
            comments.addAll(commentRepository.findExpired(Util.now()));
            for (Comment comment : comments) {
                EntryRevision latest = comment.getCurrentRevision();
                Posting posting = comment.getPosting();
                if (comment.getDeletedAt() != null || comment.getTotalRevisions() <= 1) {
                    if (comment.getDeletedAt() == null) {
                        log.debug("Total comments for posting {} = {} - 1: purging expired unsigned comment {}",
                                LogUtil.format(posting.getId()),
                                LogUtil.format(posting.getTotalChildren()),
                                LogUtil.format(comment.getId()));
                        posting.setTotalChildren(posting.getTotalChildren() - 1);
                    }
                    commentRepository.delete(comment);

                    liberins.add(new CommentDeletedLiberin(comment, latest).withNodeId(posting.getNodeId()));
                } else {
                    EntryRevision revision = comment.getRevisions().stream()
                            .min(Comparator.comparing(EntryRevision::getCreatedAt))
                            .orElse(null);
                    if (revision != null) { // always
                        revision.setDeletedAt(null);
                        entryRevisionRepository.delete(comment.getCurrentRevision());
                        comment.setCurrentRevision(revision);
                        comment.setTotalRevisions(comment.getTotalRevisions() - 1);

                        liberins.add(new CommentUpdatedLiberin(comment, latest, comment.getViewE())
                                .withNodeId(posting.getNodeId()));
                    }
                }
            }

            return null;
        });

        liberins.forEach(liberinManager::send);
    }

}
