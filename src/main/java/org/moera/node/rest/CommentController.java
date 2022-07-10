package org.moera.node.rest;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
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
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;
import javax.validation.Valid;

import com.querydsl.core.BooleanBuilder;
import org.moera.commons.crypto.CryptoUtil;
import org.moera.commons.crypto.Fingerprint;
import org.moera.commons.util.LogUtil;
import org.moera.node.auth.AuthenticationException;
import org.moera.node.auth.IncorrectSignatureException;
import org.moera.node.auth.principal.Principal;
import org.moera.node.data.Comment;
import org.moera.node.data.CommentRepository;
import org.moera.node.data.EntryAttachmentRepository;
import org.moera.node.data.EntryRevision;
import org.moera.node.data.MediaFileOwner;
import org.moera.node.data.MediaFileOwnerRepository;
import org.moera.node.data.Posting;
import org.moera.node.data.PostingRepository;
import org.moera.node.data.QComment;
import org.moera.node.data.Reaction;
import org.moera.node.data.ReactionRepository;
import org.moera.node.data.SourceFormat;
import org.moera.node.fingerprint.Fingerprints;
import org.moera.node.global.ApiController;
import org.moera.node.global.NoCache;
import org.moera.node.global.NoClientId;
import org.moera.node.global.RequestContext;
import org.moera.node.liberin.model.CommentAddedLiberin;
import org.moera.node.liberin.model.CommentDeletedLiberin;
import org.moera.node.liberin.model.CommentUpdatedLiberin;
import org.moera.node.media.MediaOperations;
import org.moera.node.model.ClientReactionInfo;
import org.moera.node.model.CommentCreated;
import org.moera.node.model.CommentInfo;
import org.moera.node.model.CommentMassAttributes;
import org.moera.node.model.CommentText;
import org.moera.node.model.CommentTotalInfo;
import org.moera.node.model.CommentsSliceInfo;
import org.moera.node.model.ObjectNotFoundFailure;
import org.moera.node.model.PostingInfo;
import org.moera.node.model.Result;
import org.moera.node.model.ValidationFailure;
import org.moera.node.model.body.BodyMappingException;
import org.moera.node.naming.NamingCache;
import org.moera.node.operations.CommentOperations;
import org.moera.node.operations.ContactOperations;
import org.moera.node.operations.OperationsValidator;
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
    private static final int MASS_UPDATE_PAGE_SIZE = 100;

    @Inject
    private RequestContext requestContext;

    @Inject
    private PostingRepository postingRepository;

    @Inject
    private CommentRepository commentRepository;

    @Inject
    private ReactionRepository reactionRepository;

    @Inject
    private EntryAttachmentRepository entryAttachmentRepository;

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
    @PersistenceContext
    private EntityManager entityManager;

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
        if (!requestContext.isPrincipal(posting.getViewE())) {
            throw new ObjectNotFoundFailure("posting.not-found");
        }
        if (posting.getCurrentRevision().getSignature() == null) {
            throw new ValidationFailure("posting.not-signed");
        }
        Comment repliedTo = null;
        if (commentText.getRepliedToId() != null) {
            repliedTo = commentRepository.findFullByNodeIdAndId(requestContext.nodeId(), commentText.getRepliedToId())
                    .orElse(null);
            if (repliedTo == null || !repliedTo.getPosting().getId().equals(posting.getId())
                    || !requestContext.isPrincipal(repliedTo.getViewE())) {
                throw new ObjectNotFoundFailure("commentText.repliedToId.not-found");
            }
        }
        byte[] repliedToDigest = repliedTo != null ? repliedTo.getCurrentRevision().getDigest() : null;
        byte[] digest = validateCommentText(posting, null, commentText, commentText.getOwnerName(),
                repliedToDigest);
        if (commentText.getSignature() != null) {
            requestContext.authenticatedWithSignature(commentText.getOwnerName());
        }
        if (!requestContext.isPrincipal(posting.getViewCommentsE())) {
            throw new AuthenticationException();
        }
        if (!requestContext.isPrincipal(posting.getAddCommentE())) {
            throw new AuthenticationException();
        }
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
        }

        requestContext.send(new CommentAddedLiberin(posting, comment));

        return ResponseEntity.created(URI.create("/postings/" + posting.getId() + "/comments" + comment.getId()))
                .body(new CommentCreated(comment, posting.getTotalChildren(), requestContext));
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
        EntryRevision latest = comment.getCurrentRevision();
        Principal latestView = comment.getViewE();
        if (!comment.getPosting().getId().equals(postingId)) {
            throw new ObjectNotFoundFailure("comment.wrong-posting");
        }
        byte[] repliedToDigest = comment.getRepliedTo() != null
                ? comment.getRepliedTo().getCurrentRevision().getDigest() : null;
        byte[] digest = validateCommentText(comment.getPosting(), comment, commentText, comment.getOwnerName(),
                repliedToDigest);
        if (commentText.getSignature() != null) {
            requestContext.authenticatedWithSignature(commentText.getOwnerName());
        }
        if (!requestContext.isPrincipal(comment.getPosting().getViewE())) {
            throw new ObjectNotFoundFailure("posting.not-found");
        }
        if (!requestContext.isPrincipal(comment.getPosting().getViewCommentsE())) {
            throw new ObjectNotFoundFailure("comment.not-found");
        }
        if (!requestContext.isPrincipal(comment.getViewE())) {
            throw new ObjectNotFoundFailure("comment.not-found");
        }
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
        if (requestContext.isPrincipal(comment.getEditE())) {
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
        } else {
            // senior, but not owner
            commentText.toEntrySenior(comment);
        }

        requestContext.send(new CommentUpdatedLiberin(comment, latest, latestView));

        return withSeniorReaction(withClientReaction(new CommentInfo(comment, requestContext)),
                comment.getPosting().getOwnerName());
    }

    private byte[] validateCommentText(Posting posting, Comment comment, CommentText commentText, String ownerName,
                                       byte[] repliedToDigest) {

        boolean isSenior = requestContext.isPrincipal(posting.getOverrideCommentE());

        byte[] digest = null;
        if (commentText.getSignature() == null) {
            String clientName = requestContext.getClientName();
            if (ObjectUtils.isEmpty(clientName)) {
                throw new AuthenticationException();
            }
            if (!ObjectUtils.isEmpty(ownerName) && !ownerName.equals(clientName)) {
                // posting owner may change seniorOperations
                if (comment == null || !isSenior) {
                    throw new AuthenticationException();
                }
            }
            commentText.setOwnerName(clientName);

            if (comment == null && ObjectUtils.isEmpty(commentText.getBodySrc())
                    && ObjectUtils.isEmpty(commentText.getMedia())) {
                throw new ValidationFailure("commentText.bodySrc.blank");
            }
            if (commentText.getBodySrc() != null && commentText.getBodySrc().length() > getMaxCommentSize()) {
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
        OperationsValidator.validateOperations(commentText::getPrincipal, OperationsValidator.COMMENT_OPERATIONS,
                false, "commentText.operations.wrong-principal");
        if (commentText.getSeniorOperations() != null && !commentText.getSeniorOperations().isEmpty() && !isSenior) {
            throw new AuthenticationException();
        }
        OperationsValidator.validateOperations(commentText::getSeniorPrincipal, OperationsValidator.COMMENT_OPERATIONS,
                true, "commentText.seniorOperations.wrong-principal");
        OperationsValidator.validateOperations(commentText::getPrincipal,
                OperationsValidator.COMMENT_REACTION_OPERATIONS, false,
                "commentText.reactionOperations.wrong-principal");

        return digest;
    }

    private byte[] mediaDigest(UUID id) {
        MediaFileOwner media = mediaFileOwnerRepository.findById(id).orElse(null);
        return media != null ? media.getMediaFile().getDigest() : null;
    }

    @PutMapping
    @Transactional
    @NoClientId
    public Result putAll(@PathVariable UUID postingId, @Valid @RequestBody CommentMassAttributes commentMassAttributes) {
        log.info("PUT /postings/{postingId}/comments (postingId = {})", LogUtil.format(postingId));

        Posting posting = postingRepository.findByNodeIdAndId(requestContext.nodeId(), postingId)
                .orElseThrow(() -> new ObjectNotFoundFailure("posting.not-found"));
        if (!requestContext.isPrincipal(posting.getViewE())) {
            throw new ObjectNotFoundFailure("posting.not-found");
        }
        if (!requestContext.isPrincipal(posting.getOverrideCommentE())) {
            throw new AuthenticationException();
        }
        OperationsValidator.validateOperations(commentMassAttributes::getSeniorPrincipal,
                OperationsValidator.COMMENT_OPERATIONS, true,
                "commentMassAttributes.seniorOperations.wrong-principal");

        Page<Comment> page;
        int n = 0;
        do {
            // TODO despite pagination, objects and liberins may still take a lot of memory
            // It is possible to perform the update in a single query, making a backup of the previous state
            // Then, use the backup to generate liberins
            page = commentRepository.findByNodeIdAndParentId(requestContext.nodeId(), postingId,
                    PageRequest.of(n++, MASS_UPDATE_PAGE_SIZE));
            for (Comment comment : page.getContent()) {
                if (!commentMassAttributes.sameAsEntry(comment)) {
                    Principal latestView = comment.getViewE();
                    commentMassAttributes.toEntry(comment);
                    requestContext.send(new CommentUpdatedLiberin(comment, comment.getCurrentRevision(), latestView));
                }
            }
            commentRepository.flush();
        } while (!page.isLast());

        return Result.OK;
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
        if (!requestContext.isPrincipal(posting.getViewE())) {
            throw new ObjectNotFoundFailure("posting.not-found");
        }
        if (!requestContext.isPrincipal(posting.getViewCommentsE())) {
            CommentsSliceInfo sliceInfo = new CommentsSliceInfo();
            sliceInfo.setBefore(SafeInteger.MAX_VALUE);
            sliceInfo.setAfter(SafeInteger.MIN_VALUE);
            sliceInfo.setComments(Collections.emptyList());
            return sliceInfo;
        }
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
            sliceInfo = getCommentsBefore(posting, before, limit);
        } else {
            sliceInfo = getCommentsAfter(posting, after, limit);
        }
        calcSliceTotals(sliceInfo, posting);

        return sliceInfo;
    }

    private CommentsSliceInfo getCommentsBefore(Posting posting, long before, int limit) {
        CommentsSliceInfo sliceInfo = new CommentsSliceInfo();
        sliceInfo.setBefore(before);
        long sliceBefore = before;
        do {
            Page<Comment> page = findSlice(requestContext.nodeId(), posting.getId(), SafeInteger.MIN_VALUE, sliceBefore,
                    limit + 1, Sort.Direction.DESC);
            if (page.getNumberOfElements() < limit + 1) {
                sliceInfo.setAfter(SafeInteger.MIN_VALUE);
            } else {
                sliceInfo.setAfter(page.getContent().get(limit).getMoment());
            }
            fillSlice(sliceInfo, posting, limit);
            sliceBefore = sliceInfo.getAfter();
        } while (sliceBefore > SafeInteger.MIN_VALUE && sliceInfo.getComments().size() < limit / 2);
        return sliceInfo;
    }

    private CommentsSliceInfo getCommentsAfter(Posting posting, long after, int limit) {
        CommentsSliceInfo sliceInfo = new CommentsSliceInfo();
        sliceInfo.setAfter(after);
        long sliceAfter = after;
        do {
            Page<Comment> page = findSlice(requestContext.nodeId(), posting.getId(), sliceAfter, SafeInteger.MAX_VALUE,
                    limit + 1, Sort.Direction.ASC);
            if (page.getNumberOfElements() < limit + 1) {
                sliceInfo.setBefore(SafeInteger.MAX_VALUE);
            } else {
                sliceInfo.setBefore(page.getContent().get(limit - 1).getMoment());
            }
            fillSlice(sliceInfo, posting, limit);
            sliceAfter = sliceInfo.getBefore();
        } while (sliceAfter < SafeInteger.MAX_VALUE && sliceInfo.getComments().size() < limit / 2);
        return sliceInfo;
    }

    private Page<Comment> findSlice(UUID nodeId, UUID parentId, long afterMoment, long beforeMoment, int limit,
                                  Sort.Direction direction) {
        QComment comment = QComment.comment;
        BooleanBuilder where = new BooleanBuilder();
        where.and(comment.nodeId.eq(nodeId))
                .and(comment.parent.id.eq(parentId))
                .and(comment.moment.gt(afterMoment))
                .and(comment.moment.loe(beforeMoment))
                .and(comment.deletedAt.isNotNull());
        if (!requestContext.isAdmin()) {
            var viewPrincipal = comment.viewPrincipal;
            BooleanBuilder visibility = new BooleanBuilder();
            visibility.or(viewPrincipal.eq(Principal.PUBLIC));
            if (!ObjectUtils.isEmpty(requestContext.getClientName())) {
                visibility.or(viewPrincipal.eq(Principal.SIGNED));
                BooleanBuilder secret = new BooleanBuilder();
                secret.and(viewPrincipal.eq(Principal.SECRET));
                secret.and(comment.parent.ownerName.eq(requestContext.getClientName()));
                visibility.or(secret);
                BooleanBuilder priv = new BooleanBuilder();
                priv.and(viewPrincipal.eq(Principal.PRIVATE));
                priv.and(comment.ownerName.eq(requestContext.getClientName()));
                visibility.or(priv);
            }
            where.and(visibility);
        }
        return commentRepository.findAll(where, PageRequest.of(0, limit + 1, direction, "moment"));
    }

    private void fillSlice(CommentsSliceInfo sliceInfo, Posting posting, int limit) {
        List<CommentInfo> comments = commentRepository.findInRange(
                requestContext.nodeId(), posting.getId(), sliceInfo.getAfter(), sliceInfo.getBefore())
                .stream()
                .filter(c -> requestContext.isPrincipal(c.getViewE()))
                .map(c -> new CommentInfo(c, requestContext))
                .sorted(Comparator.comparing(CommentInfo::getMoment))
                .collect(Collectors.toList());
        if (comments.size() > limit) {
            comments.remove(limit);
        }
        Map<String, CommentInfo> commentMap = comments.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(CommentInfo::getId, Function.identity(), (p1, p2) -> p1));
        String clientName = requestContext.getClientName();
        if (!ObjectUtils.isEmpty(clientName)) {
            reactionRepository.findByCommentsInRangeAndOwner(requestContext.nodeId(), posting.getId(),
                            sliceInfo.getAfter(), sliceInfo.getBefore(), clientName)
                    .stream()
                    .map(ClientReactionInfo::new)
                    .filter(r -> commentMap.containsKey(r.getEntryId()))
                    .forEach(r -> commentMap.get(r.getEntryId()).setClientReaction(r));
        }
        reactionRepository.findByCommentsInRangeAndOwner(requestContext.nodeId(), posting.getId(),
                        sliceInfo.getAfter(), sliceInfo.getBefore(), posting.getOwnerName())
                .stream()
                .map(ClientReactionInfo::new)
                .filter(r -> commentMap.containsKey(r.getEntryId()))
                .forEach(r -> commentMap.get(r.getEntryId()).setSeniorReaction(r));
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
        if (!requestContext.isPrincipal(comment.getViewE())) {
            throw new ObjectNotFoundFailure("comment.not-found");
        }
        if (!requestContext.isPrincipal(comment.getPosting().getViewE())) {
            throw new ObjectNotFoundFailure("posting.not-found");
        }
        if (!requestContext.isPrincipal(comment.getPosting().getViewCommentsE())) {
            throw new ObjectNotFoundFailure("comment.not-found");
        }
        if (!comment.getPosting().getId().equals(postingId)) {
            throw new ObjectNotFoundFailure("comment.wrong-posting");
        }

        return withSeniorReaction(
                withClientReaction(new CommentInfo(comment, includeSet.contains("source"), requestContext)),
                comment.getPosting().getOwnerName());
    }

    @DeleteMapping("/{commentId}")
    @Transactional
    public CommentTotalInfo delete(@PathVariable UUID postingId, @PathVariable UUID commentId) {
        log.info("DELETE /postings/{postingId}/comments/{commentId} (postingId = {}, commentId = {})",
                LogUtil.format(postingId), LogUtil.format(commentId));

        Comment comment = commentRepository.findFullByNodeIdAndId(requestContext.nodeId(), commentId)
                .orElseThrow(() -> new ObjectNotFoundFailure("comment.not-found"));
        EntryRevision latest = comment.getCurrentRevision();
        if (!requestContext.isPrincipal(comment.getViewE())) {
            throw new ObjectNotFoundFailure("comment.not-found");
        }
        if (!requestContext.isPrincipal(comment.getPosting().getViewE())) {
            throw new ObjectNotFoundFailure("posting.not-found");
        }
        if (!requestContext.isPrincipal(comment.getPosting().getViewCommentsE())) {
            throw new ObjectNotFoundFailure("comment.not-found");
        }
        if (!comment.getPosting().getId().equals(postingId)) {
            throw new ObjectNotFoundFailure("comment.wrong-posting");
        }
        if (!requestContext.isPrincipal(comment.getDeleteE())) {
            throw new AuthenticationException();
        }
        entityManager.lock(comment, LockModeType.PESSIMISTIC_WRITE);
        commentOperations.deleteComment(comment);
        if (comment.getOwnerName().equals(requestContext.nodeName())) {
            contactOperations.updateCloseness(comment.getRepliedToName(), -1);
        } else {
            contactOperations.updateCloseness(comment.getOwnerName(), -1);
        }

        requestContext.send(new CommentDeletedLiberin(comment, latest));

        return new CommentTotalInfo(comment.getPosting().getTotalChildren());
    }

    @GetMapping("/{commentId}/attached")
    @Transactional
    public List<PostingInfo> getAttached(@PathVariable UUID postingId, @PathVariable UUID commentId) {
        log.info("GET /postings/{postingId}/comments/{commentId}/attached, (postingId = {}, commentId = {})",
                LogUtil.format(postingId), LogUtil.format(commentId));

        Comment comment = commentRepository.findFullByNodeIdAndId(requestContext.nodeId(), commentId)
                .orElseThrow(() -> new ObjectNotFoundFailure("comment.not-found"));
        if (!requestContext.isPrincipal(comment.getViewE())) {
            throw new ObjectNotFoundFailure("comment.not-found");
        }
        if (!requestContext.isPrincipal(comment.getPosting().getViewE())) {
            throw new ObjectNotFoundFailure("posting.not-found");
        }
        if (!requestContext.isPrincipal(comment.getPosting().getViewCommentsE())) {
            throw new ObjectNotFoundFailure("comment.not-found");
        }
        if (!comment.getPosting().getId().equals(postingId)) {
            throw new ObjectNotFoundFailure("comment.wrong-posting");
        }
        List<Posting> attached = entryAttachmentRepository.findOwnAttachedPostings(
                requestContext.nodeId(), comment.getCurrentRevision().getId());
        return attached.stream()
                .map(p -> withClientReaction(new PostingInfo(p, false, requestContext)))
                .collect(Collectors.toList());
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

    private CommentInfo withSeniorReaction(CommentInfo commentInfo, String seniorName) {
        Reaction reaction = reactionRepository.findByEntryIdAndOwner(UUID.fromString(commentInfo.getId()), seniorName);
        commentInfo.setSeniorReaction(reaction != null ? new ClientReactionInfo(reaction) : null);
        return commentInfo;
    }

    private PostingInfo withClientReaction(PostingInfo postingInfo) {
        String clientName = requestContext.getClientName();
        if (ObjectUtils.isEmpty(clientName)) {
            return postingInfo;
        }
        Reaction reaction = reactionRepository.findByEntryIdAndOwner(UUID.fromString(postingInfo.getId()), clientName);
        postingInfo.setClientReaction(reaction != null ? new ClientReactionInfo(reaction) : null);
        return postingInfo;
    }

}
