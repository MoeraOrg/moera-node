package org.moera.node.rest;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;

import org.moera.lib.node.types.BlockedOperation;
import org.moera.lib.node.types.ReactionCreated;
import org.moera.lib.node.types.ReactionInfo;
import org.moera.lib.node.types.ReactionOverride;
import org.moera.lib.node.types.ReactionTotalsInfo;
import org.moera.lib.node.types.ReactionsFilter;
import org.moera.lib.node.types.ReactionsSliceInfo;
import org.moera.lib.node.types.Result;
import org.moera.lib.node.types.Scope;
import org.moera.lib.util.LogUtil;
import org.moera.node.auth.AuthenticationException;
import org.moera.node.auth.UserBlockedException;
import org.moera.node.data.OwnReaction;
import org.moera.node.data.OwnReactionRepository;
import org.moera.node.data.Posting;
import org.moera.node.data.PostingRepository;
import org.moera.node.data.Reaction;
import org.moera.node.data.ReactionRepository;
import org.moera.node.global.ApiController;
import org.moera.node.global.NoCache;
import org.moera.node.global.RequestContext;
import org.moera.node.liberin.model.PostingReactionAddedLiberin;
import org.moera.node.liberin.model.PostingReactionDeletedLiberin;
import org.moera.node.liberin.model.PostingReactionOperationsUpdatedLiberin;
import org.moera.node.liberin.model.PostingReactionsDeletedAllLiberin;
import org.moera.node.model.ObjectNotFoundFailure;
import org.moera.node.model.ReactionCreatedUtil;
import org.moera.node.model.ReactionDescription;
import org.moera.node.model.ReactionInfoUtil;
import org.moera.node.model.ReactionOverrideUtil;
import org.moera.node.model.ReactionsSliceInfoUtil;
import org.moera.node.model.ValidationFailure;
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
@RequestMapping("/moera/api/postings")
public class PostingReactionController {

    private static final Logger log = LoggerFactory.getLogger(PostingReactionController.class);

    @Inject
    private RequestContext requestContext;

    @Inject
    private ReactionRepository reactionRepository;

    @Inject
    private OwnReactionRepository ownReactionRepository;

    @Inject
    private PostingRepository postingRepository;

    @Inject
    private ReactionOperations reactionOperations;

    @Inject
    private ReactionTotalOperations reactionTotalOperations;

    @Inject
    private BlockedUserOperations blockedUserOperations;

    @Inject
    private Transaction tx;

    private final ParametrizedLock<UUID> lock = new ParametrizedLock<>();

    @PostMapping("/{postingId}/reactions")
    public ResponseEntity<ReactionCreated> post(
            @PathVariable UUID postingId,
            @Valid @RequestBody ReactionDescription reactionDescription) {

        log.info("POST /postings/{postingId}/reactions (postingId = {}, negative = {}, emoji = {})",
                LogUtil.format(postingId),
                LogUtil.format(reactionDescription.isNegative()),
                LogUtil.format(reactionDescription.getEmoji()));

        lock.lock(postingId);
        try {
            return tx.executeWrite(() -> {
                Posting posting = postingRepository.findByNodeIdAndId(requestContext.nodeId(), postingId)
                        .orElseThrow(() -> new ObjectNotFoundFailure("posting.not-found"));
                if (posting.getCurrentRevision().getSignature() == null) {
                    throw new ValidationFailure("posting.not-signed");
                }
                reactionOperations.validate(reactionDescription, posting);
                if (!requestContext.isPrincipal(posting.getViewE(), Scope.VIEW_CONTENT)) {
                    throw new ObjectNotFoundFailure("posting.not-found");
                }
                OperationsValidator.validateOperations(reactionDescription::getPrincipal,
                        OperationsValidator.POSTING_REACTION_OPERATIONS, false,
                        "reactionDescription.operations.wrong-principal");

                if (posting.isOriginal()) {
                    return postToOriginal(reactionDescription, posting);
                } else if (requestContext.isOwner()) {
                    return postToPickedAtHome(reactionDescription, posting);
                } else {
                    return postToPicked(reactionDescription, posting);
                }
            });
        } finally {
            lock.unlock(postingId);
        }
    }

    private ResponseEntity<ReactionCreated> postToOriginal(ReactionDescription reactionDescription, Posting posting) {
        var liberin = new PostingReactionAddedLiberin(posting);

        Reaction reaction = reactionOperations.post(reactionDescription, posting, liberin::setDeletedReaction,
                liberin::setAddedReaction);
        var totalsInfo = reactionTotalOperations.getInfo(posting);

        liberin.setReactionTotals(totalsInfo.getPublicInfo());
        requestContext.send(liberin);

        return ResponseEntity.created(URI.create("/postings/" + posting.getId() + "/reactions" + reaction.getId()))
                .body(ReactionCreatedUtil.build(reaction, totalsInfo.getClientInfo(), requestContext));
    }

    private ResponseEntity<ReactionCreated> postToPickedAtHome(ReactionDescription reactionDescription,
                                                               Posting posting) {
        Optional<OwnReaction> ownReaction = ownReactionRepository.findByRemotePostingId(requestContext.nodeId(),
                posting.getReceiverName(), posting.getReceiverEntryId());
        ownReaction.ifPresent(r -> reactionTotalOperations.changeEntryTotal(posting, r.isNegative(), r.getEmoji(), -1));
        reactionTotalOperations.changeEntryTotal(posting, reactionDescription.isNegative(),
                reactionDescription.getEmoji(), 1);

        var totalsInfo = reactionTotalOperations.getInfo(posting);
        return ResponseEntity.created(URI.create("/postings/" + posting.getId() + "/reactions"))
                .body(ReactionCreatedUtil.build(null, totalsInfo.getClientInfo(), requestContext));
    }

    private ResponseEntity<ReactionCreated> postToPicked(ReactionDescription reactionDescription, Posting posting) {
        reactionTotalOperations.changeEntryTotal(posting, reactionDescription.isNegative(),
                reactionDescription.getEmoji(), 1);

        var totalsInfo = reactionTotalOperations.getInfo(posting);
        return ResponseEntity.created(URI.create("/postings/" + posting.getId() + "/reactions"))
                .body(ReactionCreatedUtil.build(null, totalsInfo.getClientInfo(), requestContext));
    }

    @PutMapping("/{postingId}/reactions/{ownerName}")
    @Transactional
    public ReactionInfo put(
        @PathVariable UUID postingId, @PathVariable String ownerName, @RequestBody ReactionOverride reactionOverride
    ) {
        log.info(
            "PUT /postings/{postingId}/reactions/{ownerName} (postingId = {}, ownerName = {})",
            LogUtil.format(postingId), LogUtil.format(ownerName)
        );

        Posting posting = postingRepository.findByNodeIdAndId(requestContext.nodeId(), postingId)
            .orElseThrow(() -> new ObjectNotFoundFailure("posting.not-found"));
        if (!requestContext.isPrincipal(posting.getViewE(), Scope.VIEW_CONTENT)) {
            throw new ObjectNotFoundFailure("posting.not-found");
        }
        if (
            reactionOverride.getOperations() != null
            && !reactionOverride.getOperations().isEmpty()
            && !requestContext.isClient(ownerName, Scope.REACT)
        ) {
            throw new AuthenticationException();
        }
        if (blockedUserOperations.isBlocked(BlockedOperation.POSTING)) {
            throw new UserBlockedException();
        }
        OperationsValidator.validateOperations(
            false,
            reactionOverride.getOperations(),
            false,
            "reactionOverride.operations.wrong-principal"
        );
        if (
            reactionOverride.getSeniorOperations() != null
            && !reactionOverride.getSeniorOperations().isEmpty()
            && !requestContext.isPrincipal(posting.getOverrideReactionE(), Scope.DELETE_OTHERS_CONTENT)
        ) {
            throw new AuthenticationException();
        }
        OperationsValidator.validateOperations(
            false,
            reactionOverride.getSeniorOperations(),
            true,
            "reactionOverride.seniorOperations.wrong-principal"
        );
        Reaction reaction = reactionRepository.findByEntryIdAndOwner(posting.getId(), ownerName);
        if (reaction == null) {
            throw new ObjectNotFoundFailure("reaction.not-found");
        }

        ReactionOverrideUtil.toPostingReaction(reactionOverride, reaction);

        requestContext.send(new PostingReactionOperationsUpdatedLiberin(posting, reaction));

        return ReactionInfoUtil.build(reaction, requestContext);
    }

    @GetMapping("/{postingId}/reactions")
    @NoCache
    @Transactional
    public ReactionsSliceInfo getAll(
            @PathVariable UUID postingId,
            @RequestParam(defaultValue = "false") boolean negative,
            @RequestParam(required = false) Integer emoji,
            @RequestParam(required = false) Long before,
            @RequestParam(required = false) Integer limit) {

        log.info("GET /postings/{postingId}/reactions"
                        + " (postingId = {}, negative = {} emoji = {} before = {}, limit = {})",
                LogUtil.format(postingId), LogUtil.format(negative), LogUtil.format(emoji), LogUtil.format(before),
                LogUtil.format(limit));

        Posting posting = postingRepository.findByNodeIdAndId(requestContext.nodeId(), postingId)
                .orElseThrow(() -> new ObjectNotFoundFailure("posting.not-found"));
        if (!requestContext.isPrincipal(posting.getViewE(), Scope.VIEW_CONTENT)) {
            throw new ObjectNotFoundFailure("posting.not-found");
        }
        if (!requestContext.isPrincipal(posting.getViewReactionsE(), Scope.VIEW_CONTENT)) {
            return ReactionsSliceInfoUtil.EMPTY;
        }
        if (negative && !requestContext.isPrincipal(posting.getViewNegativeReactionsE(), Scope.VIEW_CONTENT)) {
            return ReactionsSliceInfoUtil.EMPTY;
        }
        limit = limit != null && limit <= ReactionOperations.MAX_REACTIONS_PER_REQUEST
                ? limit : ReactionOperations.MAX_REACTIONS_PER_REQUEST;
        if (limit < 0) {
            throw new ValidationFailure("limit.invalid");
        }
        before = before != null ? before : SafeInteger.MAX_VALUE;
        return reactionOperations.getBefore(postingId, negative, emoji, before, limit);
    }

    @GetMapping("/{postingId}/reactions/{ownerName}")
    @Transactional
    public ReactionInfo get(@PathVariable UUID postingId, @PathVariable String ownerName) {
        log.info("GET /postings/{postingId}/reactions/{ownerName} (postingId = {}, ownerName = {})",
                LogUtil.format(postingId), LogUtil.format(ownerName));

        Posting posting = postingRepository.findByNodeIdAndId(requestContext.nodeId(), postingId)
                .orElseThrow(() -> new ObjectNotFoundFailure("posting.not-found"));
        if (!requestContext.isPrincipal(posting.getViewE(), Scope.VIEW_CONTENT)) {
            throw new ObjectNotFoundFailure("posting.not-found");
        }
        if (!requestContext.isPrincipal(posting.getViewReactionsE(), Scope.VIEW_CONTENT)
                && !requestContext.isClient(ownerName, Scope.VIEW_CONTENT)) {
            return ReactionInfoUtil.ofPosting(postingId); // FIXME ugly, return 404
        }

        Reaction reaction = reactionRepository.findByEntryIdAndOwner(postingId, ownerName);

        if (reaction == null
                || !requestContext.isPrincipal(reaction.getViewE(), Scope.VIEW_CONTENT)
                || reaction.isNegative()
                    && !requestContext.isPrincipal(posting.getViewNegativeReactionsE(), Scope.VIEW_CONTENT)) {
            return ReactionInfoUtil.ofPosting(postingId); // FIXME ugly, return 404
        }

        return ReactionInfoUtil.build(reaction, requestContext);
    }

    @PostMapping("/reactions/search")
    @Transactional
    public List<ReactionInfo> search(@RequestBody ReactionsFilter filter) {
        log.info("POST /postings/reactions/search (ownerName = {})", LogUtil.format(filter.getOwnerName()));

        if (
            ObjectUtils.isEmpty(filter.getOwnerName()) || filter.getPostings() == null || filter.getPostings().isEmpty()
        ) {
            return Collections.emptyList();
        }

        boolean own = requestContext.isClient(filter.getOwnerName(), Scope.VIEW_CONTENT);
        List<UUID> postingIds = filter.getPostings().stream()
            .map(Util::uuid)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .toList();
        Map<UUID, Posting> postings = postingRepository.findByNodeIdAndIds(requestContext.nodeId(), postingIds)
            .stream()
            .filter(p -> requestContext.isPrincipal(p.getViewE(), Scope.VIEW_CONTENT))
            .filter(p -> requestContext.isPrincipal(p.getViewReactionsE(), Scope.VIEW_CONTENT) || own)
            .collect(Collectors.toMap(Posting::getId, Function.identity()));

        return reactionRepository.findByEntryIdsAndOwner(postings.keySet(), filter.getOwnerName()).stream()
            .filter(r -> requestContext.isPrincipal(r.getViewE(), Scope.VIEW_CONTENT))
            .filter(
                r -> !r.isNegative()
                || requestContext.isPrincipal(
                    postings.get(r.getEntryRevision().getEntry().getId()).getViewNegativeReactionsE(),
                    Scope.VIEW_CONTENT
                )
            )
            .map(r -> ReactionInfoUtil.build(r, requestContext))
            .collect(Collectors.toList());
    }

    @DeleteMapping("/{postingId}/reactions")
    @Transactional
    public Result deleteAll(@PathVariable UUID postingId) {
        log.info("DELETE /postings/{postingId}/reactions (postingId = {})", LogUtil.format(postingId));

        Posting posting = postingRepository.findByNodeIdAndId(requestContext.nodeId(), postingId)
                .orElseThrow(() -> new ObjectNotFoundFailure("posting.not-found"));
        if (!requestContext.isAdmin(Scope.DELETE_OTHERS_CONTENT)
                && !requestContext.isClient(posting.getOwnerName(), Scope.DELETE_OTHERS_CONTENT)) {
            throw new AuthenticationException();
        }
        if (!requestContext.isPrincipal(posting.getViewE(), Scope.VIEW_CONTENT)) {
            throw new ObjectNotFoundFailure("posting.not-found");
        }
        if (blockedUserOperations.isBlocked(BlockedOperation.POSTING)) {
            throw new UserBlockedException();
        }

        reactionRepository.deleteAllByEntryId(postingId, Util.now());
        reactionTotalOperations.deleteAllByEntryId(postingId);

        requestContext.send(new PostingReactionsDeletedAllLiberin(posting));

        return Result.OK;
    }

    @DeleteMapping("/{postingId}/reactions/{ownerName}")
    @Transactional
    public ReactionTotalsInfo delete(@PathVariable UUID postingId, @PathVariable String ownerName) {
        log.info("DELETE /postings/{postingId}/reactions/{ownerName} (postingId = {}, ownerName = {})",
                LogUtil.format(postingId), LogUtil.format(ownerName));

        lock.lock(postingId);
        try {
            Posting posting = postingRepository.findByNodeIdAndId(requestContext.nodeId(), postingId)
                    .orElseThrow(() -> new ObjectNotFoundFailure("posting.not-found"));
            if (!requestContext.isPrincipal(posting.getViewE(), Scope.VIEW_CONTENT)) {
                throw new ObjectNotFoundFailure("posting.not-found");
            }
            if (blockedUserOperations.isBlocked(BlockedOperation.REACTION, postingId)) {
                throw new UserBlockedException();
            }

            ReactionTotalsInfo info = tx.executeWrite(() -> {
                if (posting.isOriginal()) {
                    return deleteFromOriginal(ownerName, posting);
                } else {
                    deleteFromPickedAtHome(posting);
                    return null;
                }
            });
            return info != null ? info : reactionTotalOperations.getInfo(posting).getClientInfo();
        } finally {
            lock.unlock(postingId);
        }
    }

    private ReactionTotalsInfo deleteFromOriginal(String ownerName, Posting posting) {
        var liberin = new PostingReactionDeletedLiberin(posting);

        reactionOperations.delete(ownerName, posting, liberin::setReaction);
        var totalsInfo = reactionTotalOperations.getInfo(posting);

        liberin.setReactionTotals(totalsInfo.getPublicInfo());
        requestContext.send(liberin);

        return totalsInfo.getClientInfo();
    }

    private void deleteFromPickedAtHome(Posting posting) {
        if (!requestContext.isAdmin(Scope.REMOTE_DELETE_CONTENT)) {
            throw new AuthenticationException();
        }

        Optional<OwnReaction> ownReaction = ownReactionRepository.findByRemotePostingId(requestContext.nodeId(),
                posting.getReceiverName(), posting.getReceiverEntryId());
        ownReaction.ifPresent(r -> reactionTotalOperations.changeEntryTotal(posting, r.isNegative(), r.getEmoji(), -1));
    }

}
