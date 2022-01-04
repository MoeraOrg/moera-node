package org.moera.node.rest;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.transaction.Transactional;
import javax.validation.Valid;

import org.moera.commons.crypto.CryptoUtil;
import org.moera.commons.crypto.Fingerprint;
import org.moera.commons.util.LogUtil;
import org.moera.node.auth.AuthenticationException;
import org.moera.node.auth.IncorrectSignatureException;
import org.moera.node.data.Comment;
import org.moera.node.data.CommentRepository;
import org.moera.node.data.MediaFileOwner;
import org.moera.node.data.MediaFileOwnerRepository;
import org.moera.node.data.Posting;
import org.moera.node.data.PostingRepository;
import org.moera.node.data.Reaction;
import org.moera.node.data.ReactionRepository;
import org.moera.node.data.SourceFormat;
import org.moera.node.fingerprint.Fingerprints;
import org.moera.node.global.ApiController;
import org.moera.node.global.NoCache;
import org.moera.node.global.RequestContext;
import org.moera.node.instant.CommentInstants;
import org.moera.node.media.MediaOperations;
import org.moera.node.model.AvatarImage;
import org.moera.node.model.BodyMappingException;
import org.moera.node.model.ClientReactionInfo;
import org.moera.node.model.CommentCreated;
import org.moera.node.model.CommentInfo;
import org.moera.node.model.CommentText;
import org.moera.node.model.CommentTotalInfo;
import org.moera.node.model.CommentsSliceInfo;
import org.moera.node.model.ObjectNotFoundFailure;
import org.moera.node.model.ValidationFailure;
import org.moera.node.model.event.CommentAddedEvent;
import org.moera.node.model.event.CommentDeletedEvent;
import org.moera.node.model.event.CommentUpdatedEvent;
import org.moera.node.model.event.PostingCommentsChangedEvent;
import org.moera.node.model.notification.PostingCommentAddedNotification;
import org.moera.node.model.notification.PostingCommentDeletedNotification;
import org.moera.node.model.notification.PostingCommentsUpdatedNotification;
import org.moera.node.naming.NamingCache;
import org.moera.node.notification.send.Directions;
import org.moera.node.operations.CommentOperations;
import org.moera.node.operations.ContactOperations;
import org.moera.node.text.TextConverter;
import org.moera.node.util.SafeInteger;
import org.moera.node.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@ApiController
@RequestMapping("/moera/api/postings/{postingId}/comments")
@NoCache
public class CommentController {

    private static final Logger log = LoggerFactory.getLogger(CommentController.class);

    private static final Duration CREATED_AT_MARGIN = Duration.ofMinutes(10);

    @Inject
    private RequestContext requestContext;

    @Inject
    private PostingRepository postingRepository;

    @Inject
    private CommentRepository commentRepository;

    @Inject
    private ReactionRepository reactionRepository;

    @Inject
    private MediaFileOwnerRepository mediaFileOwnerRepository;

    @Inject
    private NamingCache namingCache;

    @Inject
    private CommentOperations commentOperations;

    @Inject
    private ContactOperations contactOperations;

    @Inject
    private MediaOperations mediaOperations;

    @Inject
    private TextConverter textConverter;

    @Inject
    private EntityManager entityManager;

    @Inject
    private CommentInstants commentInstants;

    private int getMaxCommentSize() {
        return Math.min(requestContext.getOptions().getInt("comment.max-size"),
                requestContext.getOptions().getInt("comment.max-size.soft"));
    }

    @PostMapping
    @Transactional
    public ResponseEntity<CommentCreated> post(@PathVariable UUID postingId,
            @Valid @RequestBody CommentText commentText) {

        log.info("POST /postings/{postingId}/comments (postingId = {}, bodySrc = {}, bodySrcFormat = {})",
                LogUtil.format(postingId),
                LogUtil.format(commentText.getBodySrc(), 64),
                LogUtil.format(SourceFormat.toValue(commentText.getBodySrcFormat())));

        Posting posting = postingRepository.findFullByNodeIdAndId(requestContext.nodeId(), postingId)
                .orElseThrow(() -> new ObjectNotFoundFailure("posting.not-found"));
        Comment repliedTo = null;
        if (commentText.getRepliedToId() != null) {
            repliedTo = commentRepository.findFullByNodeIdAndId(requestContext.nodeId(), commentText.getRepliedToId())
                    .orElse(null);
            if (repliedTo == null || !repliedTo.getPosting().getId().equals(posting.getId())) {
                throw new ObjectNotFoundFailure("commentText.repliedToId.not-found");
            }
        }
        byte[] repliedToDigest = repliedTo != null ? repliedTo.getCurrentRevision().getDigest() : null;
        byte[] digest = validateCommentText(posting, commentText, commentText.getOwnerName(), repliedToDigest);
        mediaOperations.validateAvatar(
                commentText.getOwnerAvatar(),
                commentText::setOwnerAvatarMediaFile,
                () -> new ValidationFailure("commentText.ownerAvatar.mediaId.not-found"));
        List<MediaFileOwner> media = mediaOperations.validateAttachments(
                commentText.getMedia(),
                () -> new ValidationFailure("commentText.media.not-found"),
                () -> new ValidationFailure("commentText.media.not-compressed"),
                requestContext.isAdmin(),
                commentText.getOwnerName());

        Comment comment = commentOperations.newComment(posting, commentText, repliedTo);
        try {
            comment = commentOperations.createOrUpdateComment(posting, comment, null, media, null,
                    revision -> commentText.toEntryRevision(revision, digest, textConverter, media),
                    commentText::toEntry);
        } catch (BodyMappingException e) {
            String field = e.getField() != null ? e.getField() : "bodySrc";
            throw new ValidationFailure(String.format("commentText.%s.wrong-encoding", field));
        }

        if (comment.getCurrentRevision().getSignature() != null) {
            if (comment.getOwnerName().equals(requestContext.nodeName())) {
                contactOperations.updateCloseness(comment.getRepliedToName(), 1);
            } else {
                contactOperations.updateCloseness(comment.getOwnerName(), 1);
            }
            commentInstants.added(comment);
            UUID repliedToId = comment.getRepliedTo() != null ? comment.getRepliedTo().getId() : null;
            requestContext.send(Directions.postingCommentsSubscribers(posting.getId()),
                    new PostingCommentAddedNotification(posting.getId(), posting.getCurrentRevision().getHeading(),
                            comment.getId(), comment.getOwnerName(), comment.getOwnerFullName(),
                            new AvatarImage(comment.getOwnerAvatarMediaFile(), comment.getOwnerAvatarShape()),
                            comment.getCurrentRevision().getHeading(), repliedToId));
        }

        requestContext.send(new CommentAddedEvent(comment));
        requestContext.send(new PostingCommentsChangedEvent(posting));
        requestContext.send(Directions.postingSubscribers(posting.getId()),
                new PostingCommentsUpdatedNotification(posting.getId(), posting.getTotalChildren()));

        return ResponseEntity.created(URI.create("/postings/" + posting.getId() + "/comments" + comment.getId()))
                .body(new CommentCreated(comment, posting.getTotalChildren()));
    }

    @PutMapping("/{commentId}")
    @Transactional
    public CommentInfo put(@PathVariable UUID postingId, @PathVariable UUID commentId,
                           @Valid @RequestBody CommentText commentText) {

        log.info("PUT /postings/{postingId}/comments/{commentId}"
                        + " (postingId = {}, commentId = {}, bodySrc = {}, bodySrcFormat = {})",
                LogUtil.format(postingId),
                LogUtil.format(commentId),
                LogUtil.format(commentText.getBodySrc(), 64),
                LogUtil.format(SourceFormat.toValue(commentText.getBodySrcFormat())));

        Comment comment = commentRepository.findFullByNodeIdAndId(requestContext.nodeId(), commentId)
                .orElseThrow(() -> new ObjectNotFoundFailure("comment.not-found"));
        if (!comment.getPosting().getId().equals(postingId)) {
            throw new ObjectNotFoundFailure("comment.wrong-posting");
        }
        byte[] repliedToDigest = comment.getRepliedTo() != null
                ? comment.getRepliedTo().getCurrentRevision().getDigest() : null;
        byte[] digest = validateCommentText(comment.getPosting(), commentText, comment.getOwnerName(), repliedToDigest);
        mediaOperations.validateAvatar(
                commentText.getOwnerAvatar(),
                commentText::setOwnerAvatarMediaFile,
                () -> new ValidationFailure("commentText.ownerAvatar.mediaId.not-found"));
        boolean isAdmin = requestContext.isAdmin() || comment.getOwnerName().equals(requestContext.nodeName());
        List<MediaFileOwner> media = mediaOperations.validateAttachments(
                commentText.getMedia(),
                () -> new ValidationFailure("commentText.media.not-found"),
                () -> new ValidationFailure("commentText.media.not-compressed"),
                isAdmin,
                comment.getOwnerName());

        entityManager.lock(comment, LockModeType.PESSIMISTIC_WRITE);
        commentText.toEntry(comment);
        try {
            comment = commentOperations.createOrUpdateComment(comment.getPosting(), comment,
                    comment.getCurrentRevision(), media, commentText::sameAsRevision,
                    revision -> commentText.toEntryRevision(revision, digest, textConverter, media),
                    commentText::toEntry);
        } catch (BodyMappingException e) {
            String field = e.getField() != null ? e.getField() : "bodySrc";
            throw new ValidationFailure(String.format("commentText.%s.wrong-encoding", field));
        }

        if (comment.getCurrentRevision().getSignature() != null) {
            commentInstants.added(comment);
            UUID repliedToId = comment.getRepliedTo() != null ? comment.getRepliedTo().getId() : null;
            requestContext.send(Directions.postingCommentsSubscribers(postingId),
                    new PostingCommentAddedNotification(postingId,
                            comment.getPosting().getCurrentRevision().getHeading(), comment.getId(),
                            comment.getOwnerName(), comment.getOwnerFullName(),
                            new AvatarImage(comment.getOwnerAvatarMediaFile(), comment.getOwnerAvatarShape()),
                            comment.getCurrentRevision().getHeading(), repliedToId));
        }

        requestContext.send(new CommentUpdatedEvent(comment));

        return withClientReaction(new CommentInfo(comment, true));
    }

    private byte[] validateCommentText(Posting posting, CommentText commentText, String ownerName,
                                       byte[] repliedToDigest) {

        byte[] digest = null;
        if (commentText.getSignature() == null) {
            String clientName = requestContext.getClientName();
            if (ObjectUtils.isEmpty(clientName)) {
                throw new AuthenticationException();
            }
            if (!ObjectUtils.isEmpty(ownerName) && !ownerName.equals(clientName)) {
                throw new AuthenticationException();
            }
            commentText.setOwnerName(clientName);

            if (ObjectUtils.isEmpty(commentText.getBodySrc()) && ObjectUtils.isEmpty(commentText.getMedia())) {
                throw new ValidationFailure("commentText.bodySrc.blank");
            }
            if (commentText.getBodySrc().length() > getMaxCommentSize()) {
                throw new ValidationFailure("commentText.bodySrc.wrong-size");
            }
        } else {
            byte[] signingKey = namingCache.get(ownerName).getSigningKey();
            Fingerprint fingerprint = Fingerprints.comment(commentText.getSignatureVersion())
                    .create(commentText, posting.getCurrentRevision().getDigest(), repliedToDigest, this::mediaDigest);
            if (!CryptoUtil.verify(fingerprint, commentText.getSignature(), signingKey)) {
                throw new IncorrectSignatureException();
            }
            digest = CryptoUtil.digest(fingerprint);

            if (ObjectUtils.isEmpty(commentText.getBody()) && ObjectUtils.isEmpty(commentText.getMedia())) {
                throw new ValidationFailure("commentText.body.blank");
            }
            if (commentText.getBody().length() > getMaxCommentSize()) {
                throw new ValidationFailure("commentText.body.wrong-size");
            }
            if (ObjectUtils.isEmpty(commentText.getBodyFormat())) {
                throw new ValidationFailure("commentText.bodyFormat.blank");
            }
            if (commentText.getCreatedAt() == null) {
                throw new ValidationFailure("commentText.createdAt.blank");
            }
            if (Duration.between(Instant.ofEpochSecond(commentText.getCreatedAt()), Instant.now()).abs()
                    .compareTo(CREATED_AT_MARGIN) > 0) {
                throw new ValidationFailure("commentText.createdAt.out-of-range");
            }
        }
        return digest;
    }

    private byte[] mediaDigest(UUID id) {
        MediaFileOwner media = mediaFileOwnerRepository.findById(id).orElse(null);
        return media != null ? media.getMediaFile().getDigest() : null;
    }

    @GetMapping
    @Transactional
    public CommentsSliceInfo getAll(
            @PathVariable UUID postingId,
            @RequestParam(required = false) Long before,
            @RequestParam(required = false) Long after,
            @RequestParam(required = false) Integer limit) {

        log.info("GET /postings/{postingId}/comments (postingId = {}, before = {}, after = {}, limit = {})",
                LogUtil.format(postingId), LogUtil.format(before), LogUtil.format(after), LogUtil.format(limit));

        Posting posting = postingRepository.findByNodeIdAndId(requestContext.nodeId(), postingId)
                .orElseThrow(() -> new ObjectNotFoundFailure("posting.not-found"));
        if (before != null && after != null) {
            throw new ValidationFailure("comments.before-after-exclusive");
        }

        limit = limit != null && limit <= CommentOperations.MAX_COMMENTS_PER_REQUEST
                ? limit : CommentOperations.MAX_COMMENTS_PER_REQUEST;
        if (limit < 0) {
            throw new ValidationFailure("limit.invalid");
        }

        CommentsSliceInfo sliceInfo;
        if (after == null) {
            before = before != null ? before : SafeInteger.MAX_VALUE;
            sliceInfo = getCommentsBefore(postingId, before, limit);
        } else {
            sliceInfo = getCommentsAfter(postingId, after, limit);
        }
        calcSliceTotals(sliceInfo, posting);

        return sliceInfo;
    }

    private CommentsSliceInfo getCommentsBefore(UUID postingId, long before, int limit) {
        Page<Comment> page = commentRepository.findSlice(requestContext.nodeId(), postingId,
                SafeInteger.MIN_VALUE, before,
                PageRequest.of(0, limit + 1, Sort.Direction.DESC, "moment"));
        CommentsSliceInfo sliceInfo = new CommentsSliceInfo();
        sliceInfo.setBefore(before);
        if (page.getNumberOfElements() < limit + 1) {
            sliceInfo.setAfter(SafeInteger.MIN_VALUE);
        } else {
            sliceInfo.setAfter(page.getContent().get(limit).getMoment());
        }
        fillSlice(sliceInfo, postingId, limit);
        return sliceInfo;
    }

    private CommentsSliceInfo getCommentsAfter(UUID postingId, long after, int limit) {
        Page<Comment> page = commentRepository.findSlice(requestContext.nodeId(), postingId,
                after, SafeInteger.MAX_VALUE,
                PageRequest.of(0, limit + 1, Sort.Direction.ASC, "moment"));
        CommentsSliceInfo sliceInfo = new CommentsSliceInfo();
        sliceInfo.setAfter(after);
        if (page.getNumberOfElements() < limit + 1) {
            sliceInfo.setBefore(SafeInteger.MAX_VALUE);
        } else {
            sliceInfo.setBefore(page.getContent().get(limit - 1).getMoment());
        }
        fillSlice(sliceInfo, postingId, limit);
        return sliceInfo;
    }

    private void fillSlice(CommentsSliceInfo sliceInfo, UUID postingId, int limit) {
        List<CommentInfo> comments = commentRepository.findInRange(
                requestContext.nodeId(), postingId, sliceInfo.getAfter(), sliceInfo.getBefore())
                .stream()
                .map(c -> new CommentInfo(c, requestContext.isAdmin() || requestContext.isClient(c.getOwnerName())))
                .sorted(Comparator.comparing(CommentInfo::getMoment))
                .collect(Collectors.toList());
        String clientName = requestContext.getClientName();
        if (!ObjectUtils.isEmpty(clientName)) {
            Map<String, CommentInfo> commentMap = comments.stream()
                    .filter(Objects::nonNull)
                    .collect(Collectors.toMap(CommentInfo::getId, Function.identity(), (p1, p2) -> p1));
            reactionRepository.findByCommentsInRangeAndOwner(
                    requestContext.nodeId(), postingId, sliceInfo.getAfter(), sliceInfo.getBefore(), clientName)
                    .stream()
                    .map(ClientReactionInfo::new)
                    .filter(r -> commentMap.containsKey(r.getEntryId()))
                    .forEach(r -> commentMap.get(r.getEntryId()).setClientReaction(r));
        }
        if (comments.size() > limit) {
            comments.remove(limit);
        }
        sliceInfo.setComments(comments);
    }

    private void calcSliceTotals(CommentsSliceInfo sliceInfo, Posting posting) {
        sliceInfo.setTotal(posting.getTotalChildren());
        if (sliceInfo.getAfter() <= SafeInteger.MIN_VALUE) {
            sliceInfo.setTotalInPast(0);
            sliceInfo.setTotalInFuture(posting.getTotalChildren() - sliceInfo.getComments().size());
        } else if (sliceInfo.getBefore() >= SafeInteger.MAX_VALUE) {
            sliceInfo.setTotalInFuture(0);
            sliceInfo.setTotalInPast(posting.getTotalChildren() - sliceInfo.getComments().size());
        } else {
            int totalInFuture = commentRepository.countInRange(requestContext.nodeId(), posting.getId(),
                    sliceInfo.getBefore(), SafeInteger.MAX_VALUE);
            sliceInfo.setTotalInFuture(totalInFuture);
            sliceInfo.setTotalInPast(posting.getTotalChildren() - totalInFuture - sliceInfo.getComments().size());
        }
    }

    @GetMapping("/{commentId}")
    @Transactional
    public CommentInfo get(@PathVariable UUID postingId, @PathVariable UUID commentId,
                           @RequestParam(required = false) String include) {
        log.info("GET /postings/{postingId}/comments/{commentId}, (postingId = {}, commentId = {}, include = {})",
                LogUtil.format(postingId), LogUtil.format(commentId), LogUtil.format(include));

        Set<String> includeSet = Util.setParam(include);

        Comment comment = commentRepository.findFullByNodeIdAndId(requestContext.nodeId(), commentId)
                .orElseThrow(() -> new ObjectNotFoundFailure("comment.not-found"));
        if (!comment.getPosting().getId().equals(postingId)) {
            throw new ObjectNotFoundFailure("comment.wrong-posting");
        }

        return withClientReaction(new CommentInfo(comment, includeSet.contains("source"),
                requestContext.isAdmin() || requestContext.isClient(comment.getOwnerName())));
    }

    @DeleteMapping("/{commentId}")
    @Transactional
    public CommentTotalInfo delete(@PathVariable UUID postingId, @PathVariable UUID commentId) {
        log.info("DELETE /postings/{postingId}/comments/{commentId} (postingId = {}, commentId = {})",
                LogUtil.format(postingId), LogUtil.format(commentId));

        Comment comment = commentRepository.findFullByNodeIdAndId(requestContext.nodeId(), commentId)
                .orElseThrow(() -> new ObjectNotFoundFailure("comment.not-found"));
        if (!comment.getPosting().getId().equals(postingId)) {
            throw new ObjectNotFoundFailure("comment.wrong-posting");
        }
        if (!requestContext.isAdmin()
                && !requestContext.isClient(comment.getPosting().getOwnerName())
                && !requestContext.isClient(comment.getOwnerName())) {
            throw new AuthenticationException();
        }
        entityManager.lock(comment, LockModeType.PESSIMISTIC_WRITE);
        commentOperations.deleteComment(comment);
        if (comment.getOwnerName().equals(requestContext.nodeName())) {
            contactOperations.updateCloseness(comment.getRepliedToName(), -1);
        } else {
            contactOperations.updateCloseness(comment.getOwnerName(), -1);
        }
        commentInstants.deleted(comment);
        requestContext.send(Directions.postingCommentsSubscribers(postingId),
                new PostingCommentDeletedNotification(postingId, comment.getId(), comment.getOwnerName(),
                        comment.getOwnerFullName(),
                        new AvatarImage(comment.getOwnerAvatarMediaFile(), comment.getOwnerAvatarShape())));
        requestContext.send(new CommentDeletedEvent(comment));

        return new CommentTotalInfo(comment.getPosting().getTotalChildren());
    }

    private CommentInfo withClientReaction(CommentInfo commentInfo) {
        String clientName = requestContext.getClientName();
        if (ObjectUtils.isEmpty(clientName)) {
            return commentInfo;
        }
        Reaction reaction = reactionRepository.findByEntryIdAndOwner(UUID.fromString(commentInfo.getId()), clientName);
        commentInfo.setClientReaction(reaction != null ? new ClientReactionInfo(reaction) : null);
        return commentInfo;
    }

}
