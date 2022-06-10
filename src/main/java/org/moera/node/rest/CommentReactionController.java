package org.moera.node.rest;

import java.net.URI;
import java.util.UUID;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.Valid;

import org.moera.commons.util.LogUtil;
import org.moera.node.auth.Admin;
import org.moera.node.data.Comment;
import org.moera.node.data.CommentRepository;
import org.moera.node.data.Reaction;
import org.moera.node.data.ReactionRepository;
import org.moera.node.data.ReactionTotalRepository;
import org.moera.node.global.ApiController;
import org.moera.node.global.NoCache;
import org.moera.node.global.RequestContext;
import org.moera.node.liberin.model.CommentReactionAddedLiberin;
import org.moera.node.liberin.model.CommentReactionDeletedLiberin;
import org.moera.node.liberin.model.CommentReactionsDeletedAllLiberin;
import org.moera.node.model.ObjectNotFoundFailure;
import org.moera.node.model.ReactionCreated;
import org.moera.node.model.ReactionDescription;
import org.moera.node.model.ReactionInfo;
import org.moera.node.model.ReactionTotalsInfo;
import org.moera.node.model.ReactionsSliceInfo;
import org.moera.node.model.Result;
import org.moera.node.model.ValidationFailure;
import org.moera.node.operations.ReactionOperations;
import org.moera.node.operations.ReactionTotalOperations;
import org.moera.node.util.SafeInteger;
import org.moera.node.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@ApiController
@RequestMapping("/moera/api/postings/{postingId}/comments/{commentId}/reactions")
@NoCache
public class CommentReactionController {

    private static final Logger log = LoggerFactory.getLogger(CommentReactionController.class);

    @Inject
    private RequestContext requestContext;

    @Inject
    private ReactionRepository reactionRepository;

    @Inject
    private ReactionTotalRepository reactionTotalRepository;

    @Inject
    private CommentRepository commentRepository;

    @Inject
    private ReactionOperations reactionOperations;

    @Inject
    private ReactionTotalOperations reactionTotalOperations;

    @PostMapping
    @Transactional
    public ResponseEntity<ReactionCreated> post(
            @PathVariable UUID postingId, @PathVariable UUID commentId,
            @Valid @RequestBody ReactionDescription reactionDescription) {

        log.info("POST /postings/{postingId}/comments/{commentId}/reactions"
                        + " (postingId = {}, commentId = {}, negative = {}, emoji = {})",
                LogUtil.format(postingId),
                LogUtil.format(commentId),
                LogUtil.format(reactionDescription.isNegative()),
                LogUtil.format(reactionDescription.getEmoji()));

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
        if (comment.getCurrentRevision().getSignature() == null) {
            throw new ValidationFailure("comment.not-signed");
        }

        reactionOperations.validate(reactionDescription, comment);
        var liberin = new CommentReactionAddedLiberin(comment);
        Reaction reaction = reactionOperations.post(reactionDescription, comment, liberin::setDeletedReaction,
                liberin::setAddedReaction);
        requestContext.send(liberin);

        var totalsInfo = reactionTotalOperations.getInfo(comment);
        return ResponseEntity.created(
                URI.create(String.format("/postings/%s/comments/%s/reactions/%s",
                        postingId, comment.getId(), reaction.getId())))
                .body(new ReactionCreated(reaction, totalsInfo.getClientInfo(), requestContext));
    }

    @GetMapping
    @Transactional
    public ReactionsSliceInfo getAll(
            @PathVariable UUID postingId,
            @PathVariable UUID commentId,
            @RequestParam(defaultValue = "false") boolean negative,
            @RequestParam(required = false) Integer emoji,
            @RequestParam(required = false) Long before,
            @RequestParam(required = false) Integer limit) {

        log.info("GET /postings/{postingId}/comments/{commentId}/reactions"
                        + " (postingId = {}, commentId = {}, negative = {} emoji = {} before = {}, limit = {})",
                LogUtil.format(postingId), LogUtil.format(commentId), LogUtil.format(negative), LogUtil.format(emoji),
                LogUtil.format(before), LogUtil.format(limit));

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
        if (!requestContext.isPrincipal(comment.getViewReactionsE())) {
            return ReactionsSliceInfo.EMPTY;
        }
        if (negative && !requestContext.isPrincipal(comment.getViewNegativeReactionsE())) {
            return ReactionsSliceInfo.EMPTY;
        }
        limit = limit != null && limit <= ReactionOperations.MAX_REACTIONS_PER_REQUEST
                ? limit : ReactionOperations.MAX_REACTIONS_PER_REQUEST;
        if (limit < 0) {
            throw new ValidationFailure("limit.invalid");
        }
        before = before != null ? before : SafeInteger.MAX_VALUE;
        return reactionOperations.getBefore(commentId, negative, emoji, before, limit);
    }

    @GetMapping("/{ownerName}")
    @Transactional
    public ReactionInfo get(@PathVariable UUID postingId, @PathVariable UUID commentId, @PathVariable String ownerName) {
        log.info("GET /postings/{postingId}/comments/{commentId}/reactions/{ownerName}"
                        + " (postingId = {}, commentId = {}, ownerName = {})",
                LogUtil.format(postingId), LogUtil.format(commentId), LogUtil.format(ownerName));

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
        if (!requestContext.isPrincipal(comment.getViewReactionsE())) {
            return ReactionInfo.ofComment(commentId); // FIXME ugly, return 404
        }

        Reaction reaction = reactionRepository.findByEntryIdAndOwner(commentId, ownerName);

        if (reaction == null
                || !requestContext.isPrincipal(reaction.getViewE())
                || reaction.isNegative() && !requestContext.isPrincipal(comment.getViewNegativeReactionsE())) {
            return ReactionInfo.ofComment(commentId); // FIXME ugly, return 404
        }

        return new ReactionInfo(reaction, requestContext);
    }

    @DeleteMapping
    @Admin
    @Transactional
    public Result deleteAll(@PathVariable UUID postingId, @PathVariable UUID commentId) {
        log.info("DELETE /postings/{postingId}/comments/{commentId}/reactions (postingId = {}, commentId = {})",
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

        reactionRepository.deleteAllByEntryId(commentId, Util.now());
        reactionTotalRepository.deleteAllByEntryId(commentId);

        requestContext.send(new CommentReactionsDeletedAllLiberin(comment));

        return Result.OK;
    }

    @DeleteMapping("/{ownerName}")
    @Transactional
    public ReactionTotalsInfo delete(@PathVariable UUID postingId, @PathVariable UUID commentId,
                                     @PathVariable String ownerName) {

        log.info("DELETE /postings/{postingId}/comments/{commentId}/reactions/{ownerName}"
                        + " (postingId = {}, commentId = {}, ownerName = {})",
                LogUtil.format(postingId), LogUtil.format(commentId), LogUtil.format(ownerName));

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

        var liberin = new CommentReactionDeletedLiberin(comment);
        reactionOperations.delete(ownerName, comment, liberin::setReaction);
        requestContext.send(liberin);

        return reactionTotalOperations.getInfo(comment).getClientInfo();
    }

}
