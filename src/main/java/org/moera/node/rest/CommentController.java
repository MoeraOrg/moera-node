package org.moera.node.rest;

import java.lang.reflect.Constructor;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
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
import org.moera.node.data.EntryRevision;
import org.moera.node.data.EntryRevisionRepository;
import org.moera.node.data.Posting;
import org.moera.node.data.PostingRepository;
import org.moera.node.data.Reaction;
import org.moera.node.data.ReactionRepository;
import org.moera.node.data.SourceFormat;
import org.moera.node.event.EventManager;
import org.moera.node.fingerprint.FingerprintManager;
import org.moera.node.fingerprint.FingerprintObjectType;
import org.moera.node.global.ApiController;
import org.moera.node.global.RequestContext;
import org.moera.node.instant.CommentInstants;
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
import org.moera.node.model.event.Event;
import org.moera.node.model.event.PostingCommentsChangedEvent;
import org.moera.node.model.notification.PostingCommentAddedNotification;
import org.moera.node.model.notification.PostingCommentsUpdatedNotification;
import org.moera.node.model.notification.PostingSubscriberNotification;
import org.moera.node.naming.NamingCache;
import org.moera.node.notification.send.Directions;
import org.moera.node.notification.send.NotificationSenderPool;
import org.moera.node.operations.CommentOperations;
import org.moera.node.text.TextConverter;
import org.moera.node.util.Transaction;
import org.moera.node.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.util.StringUtils;
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
public class CommentController {

    private static Logger log = LoggerFactory.getLogger(CommentController.class);

    private static final Duration CREATED_AT_MARGIN = Duration.ofMinutes(10);

    @Inject
    private RequestContext requestContext;

    @Inject
    private PostingRepository postingRepository;

    @Inject
    private CommentRepository commentRepository;

    @Inject
    private EntryRevisionRepository entryRevisionRepository;

    @Inject
    private ReactionRepository reactionRepository;

    @Inject
    private NamingCache namingCache;

    @Inject
    private FingerprintManager fingerprintManager;

    @Inject
    private CommentOperations commentOperations;

    @Inject
    private TextConverter textConverter;

    @Inject
    private EntityManager entityManager;

    @Inject
    private PlatformTransactionManager txManager;

    @Inject
    private EventManager eventManager;

    @Inject
    private NotificationSenderPool notificationSenderPool;

    @Inject
    private CommentInstants commentInstants;

    @PostMapping
    @Transactional
    public ResponseEntity<CommentCreated> post(@PathVariable UUID postingId,
            @Valid @RequestBody CommentText commentText) throws AuthenticationException {

        log.info("POST /postings/{postingId}/comments (postingId = {}, bodySrc = {}, bodySrcFormat = {})",
                LogUtil.format(postingId),
                LogUtil.format(commentText.getBodySrc(), 64),
                LogUtil.format(SourceFormat.toValue(commentText.getBodySrcFormat())));

        Posting posting = postingRepository.findFullByNodeIdAndId(requestContext.nodeId(), postingId).orElse(null);
        if (posting == null) {
            throw new ObjectNotFoundFailure("posting.not-found");
        }
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

        Comment comment = commentOperations.newComment(posting, commentText, repliedTo);
        try {
            comment = commentOperations.createOrUpdateComment(posting, comment, null, null,
                    revision -> commentText.toEntryRevision(revision, digest, textConverter));
        } catch (BodyMappingException e) {
            throw new ValidationFailure("commentText.bodySrc.wrong-encoding");
        }

        if (comment.getCurrentRevision().getSignature() != null) {
            commentInstants.added(comment);
            requestContext.send(Directions.postingCommentsSubscribers(posting.getId()),
                    new PostingCommentAddedNotification(posting.getId(), posting.getCurrentRevision().getHeading(),
                            comment.getId(), comment.getOwnerName(), comment.getCurrentRevision().getHeading()));
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
                           @Valid @RequestBody CommentText commentText) throws AuthenticationException {

        log.info("PUT /postings/{postingId}/comments/{commentId}"
                        + " (postingId = {}, commentId = {}, bodySrc = {}, bodySrcFormat = {})",
                LogUtil.format(postingId),
                LogUtil.format(commentId),
                LogUtil.format(commentText.getBodySrc(), 64),
                LogUtil.format(SourceFormat.toValue(commentText.getBodySrcFormat())));

        Comment comment = commentRepository.findFullByNodeIdAndId(requestContext.nodeId(), commentId).orElse(null);
        if (comment == null) {
            throw new ObjectNotFoundFailure("comment.not-found");
        }
        if (!comment.getPosting().getId().equals(postingId)) {
            throw new ObjectNotFoundFailure("comment.wrong-posting");
        }
        byte[] repliedToDigest = comment.getRepliedTo() != null
                ? comment.getRepliedTo().getCurrentRevision().getDigest() : null;
        byte[] digest = validateCommentText(comment.getPosting(), commentText, comment.getOwnerName(), repliedToDigest);

        entityManager.lock(comment, LockModeType.PESSIMISTIC_WRITE);
        commentText.toEntry(comment);
        try {
            comment = commentOperations.createOrUpdateComment(comment.getPosting(), comment,
                    comment.getCurrentRevision(), commentText::sameAsRevision,
                    revision -> commentText.toEntryRevision(revision, digest, textConverter));
        } catch (BodyMappingException e) {
            throw new ValidationFailure("commentText.bodySrc.wrong-encoding");
        }

        if (comment.getCurrentRevision().getSignature() != null) {
            commentInstants.added(comment);
            requestContext.send(Directions.postingCommentsSubscribers(postingId),
                    new PostingCommentAddedNotification(postingId,
                            comment.getPosting().getCurrentRevision().getHeading(), comment.getId(),
                            comment.getOwnerName(), comment.getCurrentRevision().getHeading()));
        }

        requestContext.send(new CommentUpdatedEvent(comment));

        return withClientReaction(new CommentInfo(comment, true));
    }

    private byte[] validateCommentText(Posting posting, CommentText commentText, String ownerName,
                                       byte[] repliedToDigest) throws AuthenticationException {

        byte[] digest = null;
        if (commentText.getSignature() == null) {
            String clientName = requestContext.getClientName();
            if (StringUtils.isEmpty(clientName)) {
                throw new AuthenticationException();
            }
            if (!StringUtils.isEmpty(ownerName) && !ownerName.equals(clientName)) {
                throw new AuthenticationException();
            }
            commentText.setOwnerName(clientName);

            if (StringUtils.isEmpty(commentText.getBodySrc())) {
                throw new ValidationFailure("commentText.bodySrc.blank");
            }
        } else {
            byte[] signingKey = namingCache.get(ownerName).getSigningKey();
            Constructor<? extends Fingerprint> constructor = fingerprintManager.getConstructor(
                    FingerprintObjectType.COMMENT, commentText.getSignatureVersion(),
                    CommentText.class, byte[].class, byte[].class);
            if (!CryptoUtil.verify(
                    commentText.getSignature(),
                    signingKey,
                    constructor,
                    commentText,
                    posting.getCurrentRevision().getDigest(),
                    repliedToDigest)) {
                throw new IncorrectSignatureException();
            }
            digest = CryptoUtil.digest(constructor, commentText, posting.getCurrentRevision().getDigest(),
                    repliedToDigest);

            if (commentText.getBody() == null || StringUtils.isEmpty(commentText.getBody().getEncoded())) {
                throw new ValidationFailure("commentText.body.blank");
            }
            if (StringUtils.isEmpty(commentText.getBodyFormat())) {
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

    @GetMapping
    public CommentsSliceInfo getAll(
            @PathVariable UUID postingId,
            @RequestParam(required = false) Long before,
            @RequestParam(required = false) Long after,
            @RequestParam(required = false) Integer limit) {

        log.info("GET /postings/{postingId}/comments (postingId = {}, before = {}, after = {}, limit = {})",
                LogUtil.format(postingId), LogUtil.format(before), LogUtil.format(after), LogUtil.format(limit));

        Posting posting = postingRepository.findByNodeIdAndId(requestContext.nodeId(), postingId).orElse(null);
        if (posting == null) {
            throw new ObjectNotFoundFailure("posting.not-found");
        }
        if (before != null && after != null) {
            throw new ValidationFailure("comments.before-after-exclusive");
        }

        limit = limit != null && limit <= CommentOperations.MAX_COMMENTS_PER_REQUEST
                ? limit : CommentOperations.MAX_COMMENTS_PER_REQUEST;
        if (limit < 0) {
            throw new ValidationFailure("limit.invalid");
        }
        if (after == null) {
            before = before != null ? before : Long.MAX_VALUE;
            return getCommentsBefore(postingId, before, limit);
        } else {
            return getCommentsAfter(postingId, after, limit);
        }
    }

    private CommentsSliceInfo getCommentsBefore(UUID postingId, long before, int limit) {
        Page<Comment> page = commentRepository.findSlice(requestContext.nodeId(), postingId, Long.MIN_VALUE, before,
                PageRequest.of(0, limit + 1, Sort.Direction.DESC, "moment"));
        CommentsSliceInfo sliceInfo = new CommentsSliceInfo();
        sliceInfo.setBefore(before);
        if (page.getNumberOfElements() < limit + 1) {
            sliceInfo.setAfter(Long.MIN_VALUE);
        } else {
            sliceInfo.setAfter(page.getContent().get(limit).getMoment());
        }
        fillSlice(sliceInfo, postingId, limit);
        return sliceInfo;
    }

    private CommentsSliceInfo getCommentsAfter(UUID postingId, long after, int limit) {
        Page<Comment> page = commentRepository.findSlice(requestContext.nodeId(), postingId, after, Long.MAX_VALUE,
                PageRequest.of(0, limit + 1, Sort.Direction.ASC, "moment"));
        CommentsSliceInfo sliceInfo = new CommentsSliceInfo();
        sliceInfo.setAfter(after);
        if (page.getNumberOfElements() < limit + 1) {
            sliceInfo.setBefore(Long.MAX_VALUE);
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
        if (!StringUtils.isEmpty(clientName)) {
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

    @GetMapping("/{commentId}")
    public CommentInfo get(@PathVariable UUID postingId, @PathVariable UUID commentId,
                           @RequestParam(required = false) String include) {
        log.info("GET /postings/{postingId}/comments/{commentId}, (postingId = {}, commentId = {}, include = {})",
                LogUtil.format(postingId), LogUtil.format(commentId), LogUtil.format(include));

        Set<String> includeSet = Util.setParam(include);

        Comment comment = commentRepository.findFullByNodeIdAndId(requestContext.nodeId(), commentId).orElse(null);
        if (comment == null) {
            throw new ObjectNotFoundFailure("comment.not-found");
        }
        if (!comment.getPosting().getId().equals(postingId)) {
            throw new ObjectNotFoundFailure("comment.wrong-posting");
        }

        return withClientReaction(new CommentInfo(comment, includeSet.contains("source"),
                requestContext.isAdmin() || requestContext.isClient(comment.getOwnerName())));
    }

    @DeleteMapping("/{commentId}")
    @Transactional
    public CommentTotalInfo delete(@PathVariable UUID postingId, @PathVariable UUID commentId)
            throws AuthenticationException {

        log.info("DELETE /postings/{postingId}/comments/{commentId} (postingId = {}, commentId = {})",
                LogUtil.format(postingId), LogUtil.format(commentId));

        Comment comment = commentRepository.findFullByNodeIdAndId(requestContext.nodeId(), commentId).orElse(null);
        if (comment == null) {
            throw new ObjectNotFoundFailure("comment.not-found");
        }
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
        commentInstants.deleted(comment);
        requestContext.send(new CommentDeletedEvent(comment));

        return new CommentTotalInfo(comment.getPosting().getTotalChildren());
    }

    private CommentInfo withClientReaction(CommentInfo commentInfo) {
        String clientName = requestContext.getClientName();
        if (StringUtils.isEmpty(clientName)) {
            return commentInfo;
        }
        Reaction reaction = reactionRepository.findByEntryIdAndOwner(UUID.fromString(commentInfo.getId()), clientName);
        commentInfo.setClientReaction(reaction != null ? new ClientReactionInfo(reaction) : null);
        return commentInfo;
    }

    @Scheduled(fixedDelayString = "PT15M")
    public void purgeExpired() throws Throwable {
        Map<UUID, List<Event>> events = new HashMap<>();
        List<PostingSubscriberNotification> notifications = new ArrayList<>();

        Transaction.execute(txManager, () -> {
            List<Comment> comments = commentRepository.findExpiredUnsigned(Util.now());
            comments.addAll(commentRepository.findExpired(Util.now()));
            for (Comment comment : comments) {
                List<Event> eventList = events.computeIfAbsent(comment.getNodeId(), id -> new ArrayList<>());
                if (comment.getDeletedAt() != null || comment.getTotalRevisions() <= 1) {
                    Posting posting = comment.getPosting();
                    posting.setTotalChildren(posting.getTotalChildren() - 1);
                    commentRepository.delete(comment);

                    eventList.add(new CommentDeletedEvent(comment));
                    eventList.add(new PostingCommentsChangedEvent(posting));
                    notifications.add(
                            new PostingCommentsUpdatedNotification(posting.getId(), posting.getTotalChildren()));
                } else {
                    EntryRevision revision = comment.getRevisions().stream()
                            .min(Comparator.comparing(EntryRevision::getCreatedAt))
                            .orElse(null);
                    if (revision != null) { // always
                        revision.setDeletedAt(null);
                        entryRevisionRepository.delete(comment.getCurrentRevision());
                        comment.setCurrentRevision(revision);
                        comment.setTotalRevisions(comment.getTotalRevisions() - 1);

                        eventList.add(new CommentUpdatedEvent(comment));
                    }
                }
            }

            return null;
        });

        for (var e : events.entrySet()) {
            e.getValue().forEach(event -> eventManager.send(e.getKey(), event));
        }
        notifications.forEach(notification -> notificationSenderPool.send(
                Directions.postingSubscribers(UUID.fromString(notification.getPostingId())), notification));
    }

}
