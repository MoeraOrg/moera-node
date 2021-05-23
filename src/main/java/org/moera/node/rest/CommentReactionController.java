package org.moera.node.rest;

import java.net.URI;
import java.util.UUID;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.Valid;

import org.moera.commons.util.LogUtil;
import org.moera.node.auth.Admin;
import org.moera.node.auth.AuthenticationException;
import org.moera.node.data.Comment;
import org.moera.node.data.CommentRepository;
import org.moera.node.data.Reaction;
import org.moera.node.data.ReactionRepository;
import org.moera.node.data.ReactionTotalRepository;
import org.moera.node.global.ApiController;
import org.moera.node.global.RequestContext;
import org.moera.node.model.AvatarImage;
import org.moera.node.model.ObjectNotFoundFailure;
import org.moera.node.model.ReactionCreated;
import org.moera.node.model.ReactionDescription;
import org.moera.node.model.ReactionInfo;
import org.moera.node.model.ReactionTotalsInfo;
import org.moera.node.model.ReactionsSliceInfo;
import org.moera.node.model.Result;
import org.moera.node.model.ValidationFailure;
import org.moera.node.model.event.CommentReactionsChangedEvent;
import org.moera.node.model.notification.CommentReactionAddedNotification;
import org.moera.node.model.notification.CommentReactionDeletedAllNotification;
import org.moera.node.model.notification.CommentReactionDeletedNotification;
import org.moera.node.notification.send.Directions;
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
public class CommentReactionController {

    private static Logger log = LoggerFactory.getLogger(CommentReactionController.class);

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
        if (!comment.getPosting().getId().equals(postingId)) {
            throw new ObjectNotFoundFailure("comment.wrong-posting");
        }
        if (comment.getCurrentRevision().getSignature() == null) {
            throw new ValidationFailure("comment.not-signed");
        }

        reactionOperations.validate(reactionDescription, comment);
        Reaction reaction = reactionOperations.post(reactionDescription, comment, r -> notifyDeleted(comment, r),
                r -> notifyAdded(comment, r));
        requestContext.send(new CommentReactionsChangedEvent(comment));

        var totalsInfo = reactionTotalOperations.getInfo(comment);
        return ResponseEntity.created(
                URI.create(String.format("/postings/%s/comments/%s/reactions/%s",
                        postingId, comment.getId(), reaction.getId())))
                .body(new ReactionCreated(reaction, totalsInfo.getClientInfo()));
    }

    private void notifyAdded(Comment comment, Reaction reaction) {
        if (reaction.getSignature() == null) {
            return;
        }
        requestContext.send(Directions.single(comment.getOwnerName()),
                new CommentReactionAddedNotification(comment.getPosting().getId(), comment.getId(),
                        comment.getPosting().getCurrentRevision().getHeading(),
                        comment.getCurrentRevision().getHeading(), reaction.getOwnerName(), reaction.getOwnerFullName(),
                        new AvatarImage(reaction.getOwnerAvatarMediaFile(), reaction.getOwnerAvatarShape()),
                        reaction.isNegative(), reaction.getEmoji()));
    }

    private void notifyDeleted(Comment comment, Reaction reaction) {
        requestContext.send(Directions.single(comment.getOwnerName()),
                new CommentReactionDeletedNotification(comment.getPosting().getId(), comment.getId(),
                        reaction.getOwnerName(), reaction.getOwnerFullName(),
                        new AvatarImage(reaction.getOwnerAvatarMediaFile(), reaction.getOwnerAvatarShape()),
                        reaction.isNegative()));
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

        Comment comment = commentRepository.findFullByNodeIdAndId(requestContext.nodeId(), commentId)
                .orElseThrow(() -> new ObjectNotFoundFailure("comment.not-found"));
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
        before = before != null ? before : SafeInteger.MAX_VALUE;
        return reactionOperations.getBefore(commentId, negative, emoji, before, limit);
    }

    @GetMapping("/{ownerName}")
    public ReactionInfo get(@PathVariable UUID postingId, @PathVariable UUID commentId, @PathVariable String ownerName) {
        log.info("GET /postings/{postingId}/comments/{commentId}/reactions/{ownerName}"
                        + " (postingId = {}, commentId = {}, ownerName = {})",
                LogUtil.format(postingId), LogUtil.format(commentId), LogUtil.format(ownerName));

        Comment comment = commentRepository.findFullByNodeIdAndId(requestContext.nodeId(), commentId)
                .orElseThrow(() -> new ObjectNotFoundFailure("comment.not-found"));
        if (!comment.getPosting().getId().equals(postingId)) {
            throw new ObjectNotFoundFailure("comment.wrong-posting");
        }
        if (!comment.isReactionsVisible() && !requestContext.isAdmin()
                && !requestContext.isClient(comment.getOwnerName())) {
            return ReactionInfo.ofComment(commentId);
        }

        Reaction reaction = reactionRepository.findByEntryIdAndOwner(commentId, ownerName);

        return reaction != null ? new ReactionInfo(reaction) : ReactionInfo.ofComment(commentId);
    }

    @DeleteMapping
    @Admin
    @Transactional
    public Result deleteAll(@PathVariable UUID postingId, @PathVariable UUID commentId) {
        log.info("DELETE /postings/{postingId}/comments/{commentId}/reactions (postingId = {}, commentId = {})",
                LogUtil.format(postingId), LogUtil.format(commentId));

        Comment comment = commentRepository.findFullByNodeIdAndId(requestContext.nodeId(), commentId)
                .orElseThrow(() -> new ObjectNotFoundFailure("comment.not-found"));
        if (!comment.getPosting().getId().equals(postingId)) {
            throw new ObjectNotFoundFailure("comment.wrong-posting");
        }

        reactionRepository.deleteAllByEntryId(commentId, Util.now());
        reactionTotalRepository.deleteAllByEntryId(commentId);
        notifyDeletedAll(comment);
        requestContext.send(new CommentReactionsChangedEvent(comment));

        return Result.OK;
    }

    private void notifyDeletedAll(Comment comment) {
        requestContext.send(Directions.single(comment.getOwnerName()),
                new CommentReactionDeletedAllNotification(comment.getPosting().getId(), comment.getId()));
    }

    @DeleteMapping("/{ownerName}")
    @Transactional
    public ReactionTotalsInfo delete(@PathVariable UUID postingId, @PathVariable UUID commentId,
                                     @PathVariable String ownerName) {

        log.info("DELETE /postings/{postingId}/comments/{commentId}/reactions/{ownerName}"
                        + " (postingId = {}, commentId = {}, ownerName = {})",
                LogUtil.format(postingId), LogUtil.format(commentId), LogUtil.format(ownerName));

        if (!requestContext.isAdmin() && !requestContext.isClient(ownerName)) {
            throw new AuthenticationException();
        }

        Comment comment = commentRepository.findFullByNodeIdAndId(requestContext.nodeId(), commentId)
                .orElseThrow(() -> new ObjectNotFoundFailure("comment.not-found"));
        if (!comment.getPosting().getId().equals(postingId)) {
            throw new ObjectNotFoundFailure("comment.wrong-posting");
        }

        reactionOperations.delete(ownerName, comment, r -> notifyDeleted(comment, r));
        requestContext.send(new CommentReactionsChangedEvent(comment));

        return reactionTotalOperations.getInfo(comment).getClientInfo();
    }

}
