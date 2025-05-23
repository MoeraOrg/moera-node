package org.moera.node.rest;

import java.net.URI;
import java.util.UUID;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import org.moera.lib.node.types.BlockedOperation;
import org.moera.lib.node.types.ReactionCreated;
import org.moera.lib.node.types.ReactionDescription;
import org.moera.lib.node.types.ReactionInfo;
import org.moera.lib.node.types.ReactionOverride;
import org.moera.lib.node.types.ReactionTotalsInfo;
import org.moera.lib.node.types.ReactionsSliceInfo;
import org.moera.lib.node.types.Result;
import org.moera.lib.node.types.Scope;
import org.moera.lib.node.types.validate.ValidationUtil;
import org.moera.lib.util.LogUtil;
import org.moera.node.auth.Admin;
import org.moera.node.auth.AuthenticationException;
import org.moera.node.auth.UserBlockedException;
import org.moera.node.data.Comment;
import org.moera.node.data.CommentRepository;
import org.moera.node.data.Reaction;
import org.moera.node.data.ReactionRepository;
import org.moera.node.global.ApiController;
import org.moera.node.global.NoCache;
import org.moera.node.global.RequestContext;
import org.moera.node.liberin.model.CommentReactionAddedLiberin;
import org.moera.node.liberin.model.CommentReactionDeletedLiberin;
import org.moera.node.liberin.model.CommentReactionOperationsUpdatedLiberin;
import org.moera.node.liberin.model.CommentReactionsDeletedAllLiberin;
import org.moera.node.model.ObjectNotFoundFailure;
import org.moera.node.model.ReactionCreatedUtil;
import org.moera.node.model.ReactionInfoUtil;
import org.moera.node.model.ReactionOverrideUtil;
import org.moera.node.model.ReactionsSliceInfoUtil;
import org.moera.node.operations.BlockedUserOperations;
import org.moera.node.operations.OperationsValidator;
import org.moera.node.operations.ReactionOperations;
import org.moera.node.operations.ReactionTotalOperations;
import org.moera.node.util.ParametrizedLock;
import org.moera.node.util.SafeInteger;
import org.moera.node.util.Transaction;
import org.moera.node.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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
    private CommentRepository commentRepository;

    @Inject
    private ReactionOperations reactionOperations;

    @Inject
    private ReactionTotalOperations reactionTotalOperations;

    @Inject
    private BlockedUserOperations blockedUserOperations;

    @Inject
    private Transaction tx;

    private final ParametrizedLock<UUID> lock = new ParametrizedLock<>();

    @PostMapping
    public ResponseEntity<ReactionCreated> post(
        @PathVariable UUID postingId, @PathVariable UUID commentId,
        @RequestBody ReactionDescription reactionDescription
    ) {
        log.info(
            "POST /postings/{postingId}/comments/{commentId}/reactions"
                + " (postingId = {}, commentId = {}, negative = {}, emoji = {})",
            LogUtil.format(postingId),
            LogUtil.format(commentId),
            LogUtil.format(reactionDescription.isNegative()),
            LogUtil.format(reactionDescription.getEmoji())
        );

        try (var ignored = lock.lock(postingId)) {
            return tx.executeWrite(() -> {
                Comment comment = commentRepository.findFullByNodeIdAndId(requestContext.nodeId(), commentId)
                    .orElseThrow(() -> new ObjectNotFoundFailure("comment.not-found"));
                if (!comment.getPosting().getId().equals(postingId)) {
                    throw new ObjectNotFoundFailure("comment.wrong-posting");
                }
                ValidationUtil.notNull(comment.getCurrentRevision().getSignature(), "comment.not-signed");
                reactionOperations.validate(reactionDescription, comment);
                if (!requestContext.isPrincipal(comment.getViewE(), Scope.VIEW_CONTENT)) {
                    throw new ObjectNotFoundFailure("comment.not-found");
                }
                if (!requestContext.isPrincipal(comment.getPosting().getViewE(), Scope.VIEW_CONTENT)) {
                    throw new ObjectNotFoundFailure("posting.not-found");
                }
                if (!requestContext.isPrincipal(comment.getPosting().getViewCommentsE(), Scope.VIEW_CONTENT)) {
                    throw new ObjectNotFoundFailure("comment.not-found");
                }
                if (blockedUserOperations.isBlocked(BlockedOperation.REACTION, postingId)) {
                    throw new UserBlockedException();
                }
                OperationsValidator.validateOperations(
                    true,
                    reactionDescription.getOperations(),
                    false,
                    "reaction.operations.wrong-principal"
                );

                var liberin = new CommentReactionAddedLiberin(comment);
                Reaction reaction = reactionOperations.post(
                    reactionDescription, comment, liberin::setDeletedReaction, liberin::setAddedReaction
                );
                requestContext.send(liberin);

                var totalsInfo = reactionTotalOperations.getInfo(comment);
                return ResponseEntity
                    .created(
                        URI.create(
                            "/postings/%s/comments/%s/reactions/%s"
                                .formatted(postingId, comment.getId(), reaction.getId())
                        )
                    )
                    .body(ReactionCreatedUtil.build(reaction, totalsInfo.getClientInfo(), requestContext));
            });
        }
    }

    @PutMapping("/{ownerName}")
    @Transactional
    public ReactionInfo put(
        @PathVariable UUID postingId,
        @PathVariable UUID commentId,
        @PathVariable String ownerName,
        @RequestBody ReactionOverride reactionOverride
    ) {
        log.info(
            "PUT /postings/{postingId}/comments/{commentId}/reactions/{ownerName}"
                + " (postingId = {}, commentId = {}, ownerName = {})",
            LogUtil.format(postingId), LogUtil.format(commentId), LogUtil.format(ownerName)
        );

        Comment comment = commentRepository.findFullByNodeIdAndId(requestContext.nodeId(), commentId)
            .orElseThrow(() -> new ObjectNotFoundFailure("comment.not-found"));
        if (!requestContext.isPrincipal(comment.getViewE(), Scope.VIEW_CONTENT)) {
            throw new ObjectNotFoundFailure("comment.not-found");
        }
        if (!requestContext.isPrincipal(comment.getPosting().getViewE(), Scope.VIEW_CONTENT)) {
            throw new ObjectNotFoundFailure("posting.not-found");
        }
        if (!requestContext.isPrincipal(comment.getPosting().getViewCommentsE(), Scope.VIEW_CONTENT)) {
            throw new ObjectNotFoundFailure("comment.not-found");
        }
        if (!comment.getPosting().getId().equals(postingId)) {
            throw new ObjectNotFoundFailure("comment.wrong-posting");
        }
        if (
            reactionOverride.getOperations() != null
            && !reactionOverride.getOperations().isEmpty()
            && !requestContext.isClient(ownerName, Scope.REACT)
        ) {
            throw new AuthenticationException();
        }
        if (blockedUserOperations.isBlocked(BlockedOperation.COMMENT, postingId)) {
            throw new UserBlockedException();
        }
        OperationsValidator.validateOperations(
            true,
            reactionOverride.getOperations(),
            false,
            "reaction.operations.wrong-principal"
        );
        if (
            reactionOverride.getSeniorOperations() != null
            && !reactionOverride.getSeniorOperations().isEmpty()
            && !requestContext.isPrincipal(comment.getOverrideReactionE(), Scope.DELETE_OTHERS_CONTENT)
        ) {
            throw new AuthenticationException();
        }
        OperationsValidator.validateOperations(
            true,
            reactionOverride.getSeniorOperations(),
            true,
            "reaction.senior-operations.wrong-principal"
        );
        if (
            reactionOverride.getSeniorOperations() != null
            && !reactionOverride.getSeniorOperations().isEmpty()
            && !requestContext.isPrincipal(
                comment.getPosting().getOverrideCommentReactionE(), Scope.DELETE_OTHERS_CONTENT
            )
        ) {
            throw new AuthenticationException();
        }
        OperationsValidator.validateOperations(
            true,
            reactionOverride.getMajorOperations(),
            true,
            "reaction.major-operations.wrong-principal"
        );
        Reaction reaction = reactionRepository.findByEntryIdAndOwner(comment.getId(), ownerName);
        if (reaction == null) {
            throw new ObjectNotFoundFailure("reaction.not-found");
        }

        requestContext.send(new CommentReactionOperationsUpdatedLiberin(comment, reaction));

        ReactionOverrideUtil.toCommentReaction(reactionOverride, reaction);

        return ReactionInfoUtil.build(reaction, requestContext);
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
        if (!requestContext.isPrincipal(comment.getViewE(), Scope.VIEW_CONTENT)) {
            throw new ObjectNotFoundFailure("comment.not-found");
        }
        if (!requestContext.isPrincipal(comment.getPosting().getViewE(), Scope.VIEW_CONTENT)) {
            throw new ObjectNotFoundFailure("posting.not-found");
        }
        if (!requestContext.isPrincipal(comment.getPosting().getViewCommentsE(), Scope.VIEW_CONTENT)) {
            throw new ObjectNotFoundFailure("comment.not-found");
        }
        if (!comment.getPosting().getId().equals(postingId)) {
            throw new ObjectNotFoundFailure("comment.wrong-posting");
        }
        if (!requestContext.isPrincipal(comment.getViewReactionsE(), Scope.VIEW_CONTENT)) {
            return ReactionsSliceInfoUtil.EMPTY;
        }
        if (negative && !requestContext.isPrincipal(comment.getViewNegativeReactionsE(), Scope.VIEW_CONTENT)) {
            return ReactionsSliceInfoUtil.EMPTY;
        }
        limit = limit != null && limit <= ReactionOperations.MAX_REACTIONS_PER_REQUEST
                ? limit : ReactionOperations.MAX_REACTIONS_PER_REQUEST;
        ValidationUtil.assertion(limit >= 0, "limit.invalid");
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
        if (!requestContext.isPrincipal(comment.getViewE(), Scope.VIEW_CONTENT)) {
            throw new ObjectNotFoundFailure("comment.not-found");
        }
        if (!requestContext.isPrincipal(comment.getPosting().getViewE(), Scope.VIEW_CONTENT)) {
            throw new ObjectNotFoundFailure("posting.not-found");
        }
        if (!requestContext.isPrincipal(comment.getPosting().getViewCommentsE(), Scope.VIEW_CONTENT)) {
            throw new ObjectNotFoundFailure("comment.not-found");
        }
        if (!comment.getPosting().getId().equals(postingId)) {
            throw new ObjectNotFoundFailure("comment.wrong-posting");
        }
        if (!requestContext.isPrincipal(comment.getViewReactionsE(), Scope.VIEW_CONTENT)
                && !requestContext.isClient(ownerName, Scope.VIEW_CONTENT)) {
            return ReactionInfoUtil.ofComment(commentId); // FIXME ugly, return 404
        }

        Reaction reaction = reactionRepository.findByEntryIdAndOwner(commentId, ownerName);

        if (reaction == null
                || !requestContext.isPrincipal(reaction.getViewE(), Scope.VIEW_CONTENT)
                || reaction.isNegative()
                    && !requestContext.isPrincipal(comment.getViewNegativeReactionsE(), Scope.VIEW_CONTENT)) {
            return ReactionInfoUtil.ofComment(commentId); // FIXME ugly, return 404
        }

        return ReactionInfoUtil.build(reaction, requestContext);
    }

    @DeleteMapping
    @Admin(Scope.DELETE_OTHERS_CONTENT)
    @Transactional
    public Result deleteAll(@PathVariable UUID postingId, @PathVariable UUID commentId) {
        log.info("DELETE /postings/{postingId}/comments/{commentId}/reactions (postingId = {}, commentId = {})",
                LogUtil.format(postingId), LogUtil.format(commentId));

        Comment comment = commentRepository.findFullByNodeIdAndId(requestContext.nodeId(), commentId)
                .orElseThrow(() -> new ObjectNotFoundFailure("comment.not-found"));
        if (!requestContext.isPrincipal(comment.getViewE(), Scope.VIEW_CONTENT)) {
            throw new ObjectNotFoundFailure("comment.not-found");
        }
        if (!requestContext.isPrincipal(comment.getPosting().getViewE(), Scope.VIEW_CONTENT)) {
            throw new ObjectNotFoundFailure("posting.not-found");
        }
        if (!requestContext.isPrincipal(comment.getPosting().getViewCommentsE(), Scope.VIEW_CONTENT)) {
            throw new ObjectNotFoundFailure("comment.not-found");
        }
        if (!comment.getPosting().getId().equals(postingId)) {
            throw new ObjectNotFoundFailure("comment.wrong-posting");
        }
        if (blockedUserOperations.isBlocked(BlockedOperation.COMMENT, postingId)) {
            throw new UserBlockedException();
        }

        reactionRepository.deleteAllByEntryId(commentId, Util.now());
        reactionTotalOperations.deleteAllByEntryId(commentId);

        requestContext.send(new CommentReactionsDeletedAllLiberin(comment));

        return Result.OK;
    }

    @DeleteMapping("/{ownerName}")
    public ReactionTotalsInfo delete(@PathVariable UUID postingId, @PathVariable UUID commentId,
                                     @PathVariable String ownerName) {

        log.info("DELETE /postings/{postingId}/comments/{commentId}/reactions/{ownerName}"
                        + " (postingId = {}, commentId = {}, ownerName = {})",
                LogUtil.format(postingId), LogUtil.format(commentId), LogUtil.format(ownerName));

        try (var ignored = lock.lock(postingId)) {
            return tx.executeWrite(() -> {
                Comment comment = commentRepository.findFullByNodeIdAndId(requestContext.nodeId(), commentId)
                        .orElseThrow(() -> new ObjectNotFoundFailure("comment.not-found"));
                if (!requestContext.isPrincipal(comment.getViewE(), Scope.VIEW_CONTENT)) {
                    throw new ObjectNotFoundFailure("comment.not-found");
                }
                if (!requestContext.isPrincipal(comment.getPosting().getViewE(), Scope.VIEW_CONTENT)) {
                    throw new ObjectNotFoundFailure("posting.not-found");
                }
                if (!requestContext.isPrincipal(comment.getPosting().getViewCommentsE(), Scope.VIEW_CONTENT)) {
                    throw new ObjectNotFoundFailure("comment.not-found");
                }
                if (!comment.getPosting().getId().equals(postingId)) {
                    throw new ObjectNotFoundFailure("comment.wrong-posting");
                }
                if (blockedUserOperations.isBlocked(BlockedOperation.REACTION, postingId)) {
                    throw new UserBlockedException();
                }

                var liberin = new CommentReactionDeletedLiberin(comment);
                reactionOperations.delete(ownerName, comment, liberin::setReaction);
                requestContext.send(liberin);

                return reactionTotalOperations.getInfo(comment).getClientInfo();
            });
        }
    }

}
