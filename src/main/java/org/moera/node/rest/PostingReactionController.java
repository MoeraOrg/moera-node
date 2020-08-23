package org.moera.node.rest;

import java.net.URI;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.Valid;

import org.moera.commons.util.LogUtil;
import org.moera.node.auth.Admin;
import org.moera.node.auth.AuthenticationException;
import org.moera.node.data.Entry;
import org.moera.node.data.OwnReaction;
import org.moera.node.data.OwnReactionRepository;
import org.moera.node.data.Posting;
import org.moera.node.data.PostingRepository;
import org.moera.node.data.Reaction;
import org.moera.node.data.ReactionRepository;
import org.moera.node.data.ReactionTotalRepository;
import org.moera.node.event.EventManager;
import org.moera.node.global.ApiController;
import org.moera.node.global.RequestContext;
import org.moera.node.model.ObjectNotFoundFailure;
import org.moera.node.model.ReactionCreated;
import org.moera.node.model.ReactionDescription;
import org.moera.node.model.ReactionInfo;
import org.moera.node.model.ReactionTotalsInfo;
import org.moera.node.model.ReactionsSliceInfo;
import org.moera.node.model.Result;
import org.moera.node.model.ValidationFailure;
import org.moera.node.model.event.PostingReactionsChangedEvent;
import org.moera.node.model.notification.PostingReactionsUpdatedNotification;
import org.moera.node.notification.send.Directions;
import org.moera.node.notification.send.NotificationSenderPool;
import org.moera.node.operations.InstantOperations;
import org.moera.node.operations.ReactionOperations;
import org.moera.node.operations.ReactionTotalOperations;
import org.moera.node.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
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

    private static Logger log = LoggerFactory.getLogger(PostingReactionController.class);

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
    private EventManager eventManager;

    @Inject
    private NotificationSenderPool notificationSenderPool;

    @Inject
    private ReactionOperations reactionOperations;

    @Inject
    private ReactionTotalOperations reactionTotalOperations;

    @Inject
    private InstantOperations instantOperations;

    @PostMapping
    @Transactional
    public ResponseEntity<ReactionCreated> post(
            @PathVariable UUID postingId, @Valid @RequestBody ReactionDescription reactionDescription)
            throws AuthenticationException {

        log.info("POST /postings/{postingId}/reactions (postingId = {}, negative = {}, emoji = {})",
                LogUtil.format(postingId),
                LogUtil.format(reactionDescription.isNegative()),
                LogUtil.format(reactionDescription.getEmoji()));

        Posting posting = postingRepository.findFullByNodeIdAndId(requestContext.nodeId(), postingId).orElse(null);
        if (posting == null) {
            throw new ObjectNotFoundFailure("reaction.posting-not-found");
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
        Reaction reaction = reactionOperations.post(reactionDescription, posting,
                instantOperations::reactionDeleted,
                r -> instantOperations.reactionAdded(posting, r));

        requestContext.send(new PostingReactionsChangedEvent(posting));

        var totalsInfo = reactionTotalOperations.getInfo(posting);
        requestContext.send(Directions.postingSubscribers(posting.getId()),
                new PostingReactionsUpdatedNotification(posting.getId(), totalsInfo.getPublicInfo()));

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

        Posting posting = postingRepository.findFullByNodeIdAndId(requestContext.nodeId(), postingId).orElse(null);
        if (posting == null) {
            throw new ObjectNotFoundFailure("reaction.posting-not-found");
        }
        if (!posting.isReactionsVisible() && !requestContext.isAdmin()
                && !requestContext.isClient(posting.getOwnerName())) {
            return ReactionsSliceInfo.EMPTY;
        }
        limit = limit != null && limit <= ReactionOperations.MAX_REACTIONS_PER_REQUEST
                ? limit : ReactionOperations.MAX_REACTIONS_PER_REQUEST;
        if (limit < 0) {
            throw new ValidationFailure("limit.invalid");
        }
        before = before != null ? before : Long.MAX_VALUE;
        return getReactionsBefore(postingId, negative, emoji, before, limit);
    }

    private ReactionsSliceInfo getReactionsBefore(UUID postingId, boolean negative, Integer emoji, long before,
                                                  int limit) {
        Pageable pageable = PageRequest.of(0, limit + 1, Sort.Direction.DESC, "moment");
        Page<Reaction> page = emoji == null
                ? reactionRepository.findSlice(postingId, negative, Long.MIN_VALUE, before, pageable)
                : reactionRepository.findSliceWithEmoji(postingId, negative, emoji, Long.MIN_VALUE, before, pageable);
        ReactionsSliceInfo sliceInfo = new ReactionsSliceInfo();
        sliceInfo.setBefore(before);
        if (page.getNumberOfElements() < limit + 1) {
            sliceInfo.setAfter(Long.MIN_VALUE);
        } else {
            sliceInfo.setAfter(page.getContent().get(limit).getMoment());
        }
        sliceInfo.setTotal((int) page.getTotalElements());
        sliceInfo.setReactions(page.stream().map(ReactionInfo::new).collect(Collectors.toList()));
        return sliceInfo;
    }

    @GetMapping("/{ownerName}")
    public ReactionInfo get(@PathVariable UUID postingId, @PathVariable String ownerName) {
        log.info("GET /postings/{postingId}/reactions/{ownerName} (postingId = {}, ownerName = {})",
                LogUtil.format(postingId), LogUtil.format(ownerName));

        Posting posting = postingRepository.findFullByNodeIdAndId(requestContext.nodeId(), postingId).orElse(null);
        if (posting == null) {
            throw new ObjectNotFoundFailure("reaction.posting-not-found");
        }
        if (!posting.isReactionsVisible() && !requestContext.isAdmin()
                && !requestContext.isClient(posting.getOwnerName())) {
            return new ReactionInfo(postingId);
        }

        Reaction reaction = reactionRepository.findByEntryIdAndOwner(postingId, ownerName);

        return reaction != null ? new ReactionInfo(reaction) : new ReactionInfo(postingId);
    }

    @DeleteMapping
    @Admin
    @Transactional
    public Result deleteAll(@PathVariable UUID postingId) {
        log.info("DELETE /postings/{postingId}/reactions (postingId = {})", LogUtil.format(postingId));

        Posting posting = postingRepository.findFullByNodeIdAndId(requestContext.nodeId(), postingId).orElse(null);
        if (posting == null) {
            throw new ObjectNotFoundFailure("reaction.posting-not-found");
        }

        reactionRepository.deleteAllByEntryId(postingId, Util.now());
        reactionTotalRepository.deleteAllByEntryId(postingId);
        instantOperations.reactionsDeletedAll(postingId);

        requestContext.send(new PostingReactionsChangedEvent(posting));
        var totalsInfo = reactionTotalOperations.getInfo(posting);
        requestContext.send(Directions.postingSubscribers(postingId),
                new PostingReactionsUpdatedNotification(postingId, totalsInfo.getPublicInfo()));

        return Result.OK;
    }

    @DeleteMapping("/{ownerName}")
    @Transactional
    public ReactionTotalsInfo delete(@PathVariable UUID postingId, @PathVariable String ownerName)
            throws AuthenticationException {

        log.info("DELETE /postings/{postingId}/reactions/{ownerName} (postingId = {}, ownerName = {})",
                LogUtil.format(postingId), LogUtil.format(ownerName));

        if (!requestContext.isAdmin() && !requestContext.isClient(ownerName)) {
            throw new AuthenticationException();
        }

        Posting posting = postingRepository.findFullByNodeIdAndId(requestContext.nodeId(), postingId).orElse(null);
        if (posting == null) {
            throw new ObjectNotFoundFailure("reaction.posting-not-found");
        }

        if (posting.isOriginal()) {
            return deleteFromOriginal(ownerName, posting);
        } else if (requestContext.isAdmin()) {
            deleteFromPickedAtHome(posting);
        }

        return reactionTotalOperations.getInfo(posting).getClientInfo();
    }

    private ReactionTotalsInfo deleteFromOriginal(String ownerName, Posting posting) {
        Reaction reaction = reactionRepository.findByEntryIdAndOwner(posting.getId(), ownerName);
        if (reaction != null) {
            reactionTotalOperations.changeTotals(posting, reaction, -1);
            reaction.setDeletedAt(Util.now());
            Duration reactionTtl = requestContext.getOptions().getDuration("reaction.deleted.lifetime");
            reaction.setDeadline(Timestamp.from(Instant.now().plus(reactionTtl)));
            instantOperations.reactionDeleted(reaction);
        }
        reactionRepository.flush();

        requestContext.send(new PostingReactionsChangedEvent(posting));
        var totalsInfo = reactionTotalOperations.getInfo(posting);
        requestContext.send(Directions.postingSubscribers(posting.getId()),
                new PostingReactionsUpdatedNotification(posting.getId(), totalsInfo.getPublicInfo()));

        return totalsInfo.getClientInfo();
    }

    private void deleteFromPickedAtHome(Posting posting) {
        Optional<OwnReaction> ownReaction = ownReactionRepository.findByRemotePostingId(requestContext.nodeId(),
                posting.getReceiverName(), posting.getReceiverEntryId());
        ownReaction.ifPresent(r -> reactionTotalOperations.changeEntryTotal(posting, r.isNegative(), r.getEmoji(), -1));
    }

    @Scheduled(fixedDelayString = "PT15M")
    @Transactional
    public void purgeExpired() {
        Set<Entry> changed = new HashSet<>();
        reactionRepository.findExpired(Util.now()).forEach(reaction -> {
            Entry entry = reaction.getEntryRevision().getEntry();
            if (reaction.getDeletedAt() == null) {
                List<Reaction> deleted = reactionRepository.findDeletedByEntryIdAndOwner(
                        entry.getId(),
                        reaction.getOwnerName(),
                        PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "deletedAt")));
                if (deleted.size() > 0) {
                    Reaction next = deleted.get(0);
                    next.setDeletedAt(null);
                    if (next.getSignature() != null) {
                        next.setDeadline(null);
                    }
                    reactionTotalOperations.changeTotals(entry, next, 1);
                }
                reactionTotalOperations.changeTotals(entry, reaction, -1);
                changed.add(entry);
            }
            reactionRepository.delete(reaction);
        });
        for (Entry entry : changed) {
            Posting posting = (Posting) entry;
            eventManager.send(posting.getNodeId(), new PostingReactionsChangedEvent(posting));
            var totalsInfo = reactionTotalOperations.getInfo(posting);
            notificationSenderPool.send(Directions.postingSubscribers(posting.getId()),
                    new PostingReactionsUpdatedNotification(posting.getId(), totalsInfo.getPublicInfo()));
        }
    }

}
