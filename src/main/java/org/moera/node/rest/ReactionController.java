package org.moera.node.rest;

import java.lang.reflect.Constructor;
import java.net.URI;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.Valid;

import org.moera.commons.crypto.CryptoUtil;
import org.moera.commons.crypto.Fingerprint;
import org.moera.commons.util.LogUtil;
import org.moera.node.auth.AuthenticationException;
import org.moera.node.auth.IncorrectSignatureException;
import org.moera.node.data.Entry;
import org.moera.node.data.Posting;
import org.moera.node.data.PostingRepository;
import org.moera.node.data.Reaction;
import org.moera.node.data.ReactionRepository;
import org.moera.node.data.ReactionTotal;
import org.moera.node.data.ReactionTotalRepository;
import org.moera.node.fingerprint.FingerprintManager;
import org.moera.node.fingerprint.FingerprintObjectType;
import org.moera.node.global.ApiController;
import org.moera.node.global.RequestContext;
import org.moera.node.model.ObjectNotFoundFailure;
import org.moera.node.model.ReactionDescription;
import org.moera.node.model.ReactionInfo;
import org.moera.node.model.ReactionTotalsInfo;
import org.moera.node.naming.NamingCache;
import org.moera.node.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@ApiController
@RequestMapping("/moera/api/postings/{postingId}/reactions")
public class ReactionController {

    private static Logger log = LoggerFactory.getLogger(ReactionController.class);

    private static final Duration UNSIGNED_TTL = Duration.of(15, ChronoUnit.MINUTES);

    @Inject
    private RequestContext requestContext;

    @Inject
    private ReactionRepository reactionRepository;

    @Inject
    private ReactionTotalRepository reactionTotalRepository;

    @Inject
    private PostingRepository postingRepository;

    @Inject
    private NamingCache namingCache;

    @Inject
    private FingerprintManager fingerprintManager;

    @PostMapping
    @Transactional
    public ResponseEntity<ReactionInfo> post(
            @PathVariable UUID postingId, @Valid @RequestBody ReactionDescription reactionDescription)
            throws AuthenticationException {

        log.info("POST /postings/{postingId}/reactions (postingId = {}, negative = {}, emoji = {})",
                LogUtil.format(postingId),
                reactionDescription.isNegative() ? "yes" : "no",
                LogUtil.format(reactionDescription.getEmoji()));

        Posting posting = postingRepository.findByNodeIdAndId(requestContext.nodeId(), postingId).orElse(null);
        if (posting == null) {
            throw new ObjectNotFoundFailure("reaction.posting-not-found");
        }

        if (reactionDescription.getSignature() == null) {
            String ownerName = requestContext.getClientName();
            if (StringUtils.isEmpty(ownerName)) {
                throw new AuthenticationException();
            }
            if (!StringUtils.isEmpty(reactionDescription.getOwnerName())
                    && !reactionDescription.getOwnerName().equals(ownerName)) {
                throw new AuthenticationException();
            }
            reactionDescription.setOwnerName(ownerName);
        } else {
            byte[] signingKey = namingCache.get(reactionDescription.getOwnerName()).getSigningKey();
            Constructor<? extends Fingerprint> constructor = fingerprintManager.getConstructor(
                    FingerprintObjectType.REACTION, reactionDescription.getSignatureVersion(),
                    ReactionDescription.class, byte[].class);
            if (!CryptoUtil.verify(
                    reactionDescription.getSignature(),
                    signingKey,
                    constructor,
                    reactionDescription,
                    posting.getCurrentRevision().getDigest())) {
                throw new IncorrectSignatureException();
            }
        }

        Reaction reaction = reactionRepository.findByEntryIdAndOwner(postingId, reactionDescription.getOwnerName());
        if (reaction == null || reaction.getDeadline() == null
                || reaction.isNegative() != reactionDescription.isNegative()
                || reaction.getEmoji() != reactionDescription.getEmoji()
                || reaction.getSignature() == null && reactionDescription.getSignature() != null) {

            if (reaction != null) {
                changeTotals(posting, reaction, -1);
                reaction.setDeletedAt(Util.now());
            }

            reaction = new Reaction();
            reaction.setId(UUID.randomUUID());
            reaction.setEntryRevision(posting.getCurrentRevision());
            reactionDescription.toReaction(reaction);
            if (reactionDescription.getSignature() == null) {
                reaction.setDeadline(Timestamp.from(Instant.now().plus(UNSIGNED_TTL)));
            }
            reaction = reactionRepository.save(reaction);

            changeTotals(posting, reaction, 1);
        }
        reactionRepository.flush();

        Set<ReactionTotal> totals = reactionTotalRepository.findAllByEntryId(postingId);
        return ResponseEntity.created(URI.create("/postings/" + postingId + "/reactions" + reaction.getId()))
                .body(new ReactionInfo(reaction, totals));
    }

    @DeleteMapping("/{ownerName}")
    public ReactionTotalsInfo delete(@PathVariable UUID postingId, @PathVariable String ownerName)
            throws AuthenticationException {

        log.info("DELETE /postings/{postingId}/reactions/{ownerName} (postingId = {}, ownerName = {})",
                LogUtil.format(postingId), LogUtil.format(ownerName));

        if (!requestContext.isAdmin() && !Objects.equals(requestContext.getClientName(), ownerName)) {
            throw new AuthenticationException();
        }

        Posting posting = postingRepository.findByNodeIdAndId(requestContext.nodeId(), postingId).orElse(null);
        if (posting == null) {
            throw new ObjectNotFoundFailure("reaction.posting-not-found");
        }

        Reaction reaction = reactionRepository.findByEntryIdAndOwner(postingId, ownerName);
        if (reaction != null) {
            changeTotals(posting, reaction, -1);
            reaction.setDeletedAt(Util.now());
        }
        reactionRepository.flush();

        Set<ReactionTotal> totals = reactionTotalRepository.findAllByEntryId(postingId);
        return new ReactionTotalsInfo(totals);
    }

    @Scheduled(fixedDelayString = "PT15M")
    @Transactional
    public void purgeExpired() {
        reactionRepository.findExpired(Util.now()).forEach(reaction -> {
            Entry entry = reaction.getEntryRevision().getEntry();
            if (reaction.getDeletedAt() == null) {
                List<Reaction> deleted = reactionRepository.findDeletedByEntryIdAndOwner(
                        entry.getId(),
                        reaction.getOwnerName(),
                        PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "deletedAt")));
                if (deleted.size() > 0) {
                    deleted.get(0).setDeletedAt(null);
                    changeTotals(entry, deleted.get(0), 1);
                }
                changeTotals(entry, reaction, -1);
            }
            reactionRepository.delete(reaction);
        });
    }

    private void changeTotals(Entry entry, Reaction reaction, int delta) {
        ReactionTotal total = reactionTotalRepository.findByEntryRevisionId(
                reaction.getEntryRevision().getId(), reaction.isNegative(), reaction.getEmoji());
        if (total == null) {
            total = new ReactionTotal();
            total.setId(UUID.randomUUID());
            total.setEntryRevision(reaction.getEntryRevision());
            reaction.toReactionTotal(total);
            total.setTotal(delta);
            reactionTotalRepository.save(total);
        } else {
            total.setTotal(total.getTotal() + delta);
        }

        total = reactionTotalRepository.findByEntryId(entry.getId(), reaction.isNegative(), reaction.getEmoji());
        if (total == null) {
            total = new ReactionTotal();
            total.setId(UUID.randomUUID());
            total.setEntry(entry);
            reaction.toReactionTotal(total);
            total.setTotal(delta);
            reactionTotalRepository.save(total);
        } else {
            total.setTotal(total.getTotal() + delta);
        }
    }

}
