package org.moera.node.rest;

import java.net.URI;
import java.util.Optional;
import java.util.UUID;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.Valid;

import org.moera.commons.util.LogUtil;
import org.moera.node.auth.Admin;
import org.moera.node.auth.AuthenticationException;
import org.moera.node.data.OwnReaction;
import org.moera.node.data.OwnReactionRepository;
import org.moera.node.data.Posting;
import org.moera.node.data.PostingRepository;
import org.moera.node.data.Reaction;
import org.moera.node.data.ReactionRepository;
import org.moera.node.data.ReactionTotalRepository;
import org.moera.node.global.ApiController;
import org.moera.node.global.NoCache;
import org.moera.node.global.RequestContext;
import org.moera.node.liberin.model.PostingReactionAddedLiberin;
import org.moera.node.liberin.model.PostingReactionDeletedLiberin;
import org.moera.node.liberin.model.PostingReactionsDeletedAllLiberin;
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
@RequestMapping("/moera/api/postings/{postingId}/reactions")
public class PostingReactionController {

    private static final Logger log = LoggerFactory.getLogger(PostingReactionController.class);

    @Inject
    private RequestContext requestContext;

    @Inject
    private ReactionRepository reactionRepository;

    @Inject
    private ReactionTotalRepository reactionTotalRepository;

    @Inject
    private OwnReactionRepository ownReactionRepository;

    @Inject
    private PostingRepository postingRepository;

    @Inject
    private ReactionOperations reactionOperations;

    @Inject
    private ReactionTotalOperations reactionTotalOperations;

    @PostMapping
    @Transactional
    public ResponseEntity<ReactionCreated> post(
            @PathVariable UUID postingId, @Valid @RequestBody ReactionDescription reactionDescription) {

        log.info("POST /postings/{postingId}/reactions (postingId = {}, negative = {}, emoji = {})",
                LogUtil.format(postingId),
                LogUtil.format(reactionDescription.isNegative()),
                LogUtil.format(reactionDescription.getEmoji()));

        Posting posting = postingRepository.findFullByNodeIdAndId(requestContext.nodeId(), postingId)
                .orElseThrow(() -> new ObjectNotFoundFailure("posting.not-found"));
        if (!requestContext.isPrincipal(posting.getViewE())) {
            throw new ObjectNotFoundFailure("posting.not-found");
        }
        if (posting.getCurrentRevision().getSignature() == null) {
            throw new ValidationFailure("posting.not-signed");
        }

        reactionOperations.validate(reactionDescription, posting);

        if (posting.isOriginal()) {
            return postToOriginal(reactionDescription, posting);
        } else if (requestContext.isAdmin()) {
            return postToPickedAtHome(reactionDescription, posting);
        } else {
            return postToPicked(reactionDescription, posting);
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
                .body(new ReactionCreated(reaction, totalsInfo.getClientInfo()));
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
                .body(new ReactionCreated(null, totalsInfo.getClientInfo()));
    }

    private ResponseEntity<ReactionCreated> postToPicked(ReactionDescription reactionDescription, Posting posting) {
        reactionTotalOperations.changeEntryTotal(posting, reactionDescription.isNegative(),
                reactionDescription.getEmoji(), 1);

        var totalsInfo = reactionTotalOperations.getInfo(posting);
        return ResponseEntity.created(URI.create("/postings/" + posting.getId() + "/reactions"))
                .body(new ReactionCreated(null, totalsInfo.getClientInfo()));
    }

    @GetMapping
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

        Posting posting = postingRepository.findFullByNodeIdAndId(requestContext.nodeId(), postingId)
                .orElseThrow(() -> new ObjectNotFoundFailure("posting.not-found"));
        if (!requestContext.isPrincipal(posting.getViewE())) {
            throw new ObjectNotFoundFailure("posting.not-found");
        }
        if (!requestContext.isPrincipal(posting.getViewReactionsE())) {
            return ReactionsSliceInfo.EMPTY;
        }
        if (negative && !requestContext.isPrincipal(posting.getViewNegativeReactionsE())) {
            return ReactionsSliceInfo.EMPTY;
        }
        limit = limit != null && limit <= ReactionOperations.MAX_REACTIONS_PER_REQUEST
                ? limit : ReactionOperations.MAX_REACTIONS_PER_REQUEST;
        if (limit < 0) {
            throw new ValidationFailure("limit.invalid");
        }
        before = before != null ? before : SafeInteger.MAX_VALUE;
        return reactionOperations.getBefore(postingId, negative, emoji, before, limit);
    }

    @GetMapping("/{ownerName}")
    @Transactional
    public ReactionInfo get(@PathVariable UUID postingId, @PathVariable String ownerName) {
        log.info("GET /postings/{postingId}/reactions/{ownerName} (postingId = {}, ownerName = {})",
                LogUtil.format(postingId), LogUtil.format(ownerName));

        Posting posting = postingRepository.findFullByNodeIdAndId(requestContext.nodeId(), postingId)
                .orElseThrow(() -> new ObjectNotFoundFailure("posting.not-found"));
        if (!requestContext.isPrincipal(posting.getViewE())) {
            throw new ObjectNotFoundFailure("posting.not-found");
        }
        if (!requestContext.isPrincipal(posting.getViewReactionsE())) {
            return ReactionInfo.ofPosting(postingId); // FIXME ugly, return 404
        }

        Reaction reaction = reactionRepository.findByEntryIdAndOwner(postingId, ownerName);

        if (reaction == null || reaction.isNegative()
                && !requestContext.isPrincipal(posting.getViewNegativeReactionsE())) {
            return ReactionInfo.ofPosting(postingId); // FIXME ugly, return 404
        }

        return new ReactionInfo(reaction);
    }

    @DeleteMapping
    @Admin
    @Transactional
    public Result deleteAll(@PathVariable UUID postingId) {
        log.info("DELETE /postings/{postingId}/reactions (postingId = {})", LogUtil.format(postingId));

        Posting posting = postingRepository.findFullByNodeIdAndId(requestContext.nodeId(), postingId)
                .orElseThrow(() -> new ObjectNotFoundFailure("posting.not-found"));
        if (!requestContext.isPrincipal(posting.getViewE())) {
            throw new ObjectNotFoundFailure("posting.not-found");
        }

        reactionRepository.deleteAllByEntryId(postingId, Util.now());
        reactionTotalRepository.deleteAllByEntryId(postingId);

        requestContext.send(new PostingReactionsDeletedAllLiberin(posting));

        return Result.OK;
    }

    @DeleteMapping("/{ownerName}")
    @Transactional
    public ReactionTotalsInfo delete(@PathVariable UUID postingId, @PathVariable String ownerName) {
        log.info("DELETE /postings/{postingId}/reactions/{ownerName} (postingId = {}, ownerName = {})",
                LogUtil.format(postingId), LogUtil.format(ownerName));

        if (!requestContext.isAdmin() && !requestContext.isClient(ownerName)) {
            throw new AuthenticationException();
        }

        Posting posting = postingRepository.findFullByNodeIdAndId(requestContext.nodeId(), postingId)
                .orElseThrow(() -> new ObjectNotFoundFailure("posting.not-found"));
        if (!requestContext.isPrincipal(posting.getViewE())) {
            throw new ObjectNotFoundFailure("posting.not-found");
        }

        if (posting.isOriginal()) {
            return deleteFromOriginal(ownerName, posting);
        } else if (requestContext.isAdmin()) {
            deleteFromPickedAtHome(posting);
        }

        return reactionTotalOperations.getInfo(posting).getClientInfo();
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
        Optional<OwnReaction> ownReaction = ownReactionRepository.findByRemotePostingId(requestContext.nodeId(),
                posting.getReceiverName(), posting.getReceiverEntryId());
        ownReaction.ifPresent(r -> reactionTotalOperations.changeEntryTotal(posting, r.isNegative(), r.getEmoji(), -1));
    }

}
