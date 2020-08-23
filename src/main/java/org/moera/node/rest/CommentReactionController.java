package org.moera.node.rest;

import java.net.URI;
import java.util.UUID;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.Valid;

import org.moera.commons.util.LogUtil;
import org.moera.node.auth.AuthenticationException;
import org.moera.node.data.Comment;
import org.moera.node.data.CommentRepository;
import org.moera.node.data.Reaction;
import org.moera.node.global.ApiController;
import org.moera.node.global.RequestContext;
import org.moera.node.model.ObjectNotFoundFailure;
import org.moera.node.model.ReactionCreated;
import org.moera.node.model.ReactionDescription;
import org.moera.node.model.ReactionsSliceInfo;
import org.moera.node.model.ValidationFailure;
import org.moera.node.operations.ReactionOperations;
import org.moera.node.operations.ReactionTotalOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@ApiController
@RequestMapping("/moera/api/postings/{postingId}/comments/{commentId}/reactions")
public class CommentReactionController {

    private static Logger log = LoggerFactory.getLogger(CommentReactionController.class);

    @Inject
    private RequestContext requestContext;

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
            @Valid @RequestBody ReactionDescription reactionDescription) throws AuthenticationException {

        log.info("POST /postings/{postingId}/comments/{commentId}/reactions"
                        + " (postingId = {}, commentId = {}, negative = {}, emoji = {})",
                LogUtil.format(postingId),
                LogUtil.format(commentId),
                LogUtil.format(reactionDescription.isNegative()),
                LogUtil.format(reactionDescription.getEmoji()));

        Comment comment = commentRepository.findFullByNodeIdAndId(requestContext.nodeId(), commentId).orElse(null);
        if (comment == null) {
            throw new ObjectNotFoundFailure("comment.not-found");
        }
        if (!comment.getPosting().getId().equals(postingId)) {
            throw new ObjectNotFoundFailure("comment.wrong-posting");
        }

        reactionOperations.validate(reactionDescription, comment);
        Reaction reaction = reactionOperations.post(reactionDescription, comment, null, null); // TODO

        // TODO requestContext.send(new PostingReactionsChangedEvent(posting));

        var totalsInfo = reactionTotalOperations.getInfo(comment);
        return ResponseEntity.created(
                URI.create(String.format("/postings/%s/comments/%s/reactions/%s",
                        postingId, comment.getId(), reaction.getId())))
                .body(new ReactionCreated(reaction, totalsInfo.getClientInfo()));
    }

    @GetMapping
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

        Comment comment = commentRepository.findFullByNodeIdAndId(requestContext.nodeId(), commentId).orElse(null);
        if (comment == null) {
            throw new ObjectNotFoundFailure("comment.not-found");
        }
        if (!comment.getPosting().getId().equals(postingId)) {
            throw new ObjectNotFoundFailure("comment.wrong-posting");
        }
        if (!comment.isReactionsVisible() && !requestContext.isAdmin()
                && !requestContext.isClient(comment.getOwnerName())) {
            return ReactionsSliceInfo.EMPTY;
        }
        limit = limit != null && limit <= ReactionOperations.MAX_REACTIONS_PER_REQUEST
                ? limit : ReactionOperations.MAX_REACTIONS_PER_REQUEST;
        if (limit < 0) {
            throw new ValidationFailure("limit.invalid");
        }
        before = before != null ? before : Long.MAX_VALUE;
        return reactionOperations.getBefore(commentId, negative, emoji, before, limit);
    }

}
