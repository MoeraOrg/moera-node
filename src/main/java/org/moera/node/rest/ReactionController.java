package org.moera.node.rest;

import java.net.URI;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.Valid;

import org.moera.commons.util.LogUtil;
import org.moera.node.data.Posting;
import org.moera.node.data.PostingRepository;
import org.moera.node.data.Reaction;
import org.moera.node.data.ReactionRepository;
import org.moera.node.data.ReactionTotal;
import org.moera.node.data.ReactionTotalRepository;
import org.moera.node.global.ApiController;
import org.moera.node.global.RequestContext;
import org.moera.node.model.ObjectNotFoundFailure;
import org.moera.node.model.ReactionDescription;
import org.moera.node.model.ReactionInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
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

    @PostMapping
    @Transactional
    public ResponseEntity<ReactionInfo> post(@PathVariable UUID postingId,
                                             @Valid @RequestBody ReactionDescription reactionDescription) {
        log.info("POST /postings/{postingId}/reactions (postingId = {}, negative = {}, emoji = {}",
                LogUtil.format(postingId),
                reactionDescription.isNegative() ? "yes" : "no",
                LogUtil.format(reactionDescription.getEmoji()));

        Posting posting = postingRepository.findByNodeIdAndId(requestContext.nodeId(), postingId).orElse(null);
        if (posting == null) {
            throw new ObjectNotFoundFailure("reaction.posting-not-found");
        }
        String ownerName = "balu-dev_0"; // TODO
        int counterChange = 1;
        Reaction reaction = reactionRepository.findByPostingAndOwner(postingId, ownerName);
        if (reaction != null) {
            reactionRepository.delete(reaction);
            counterChange = 0;
        }

        reaction = new Reaction();
        reaction.setId(UUID.randomUUID());
        reaction.setOwnerName(ownerName);
        reaction.setEntryRevision(posting.getCurrentRevision());
        reactionDescription.toReaction(reaction);
        reaction.setDeadline(Timestamp.from(Instant.now().plus(UNSIGNED_TTL))); // TODO signature
        reactionRepository.save(reaction);

        ReactionTotal total = reactionTotalRepository.findByEntryRevision(
                posting.getCurrentRevision().getId(), reaction.isNegative(), reaction.getEmoji());
        if (total == null) {
            total = new ReactionTotal();
            total.setId(UUID.randomUUID());
            total.setEntryRevision(posting.getCurrentRevision());
            reactionDescription.toReactionTotal(total);
            total.setTotal(1);
            reactionTotalRepository.save(total);
        } else {
            total.setTotal(total.getTotal() + counterChange);
        }

        total = reactionTotalRepository.findByEntry(posting.getId(), reaction.isNegative(), reaction.getEmoji());
        if (total == null) {
            total = new ReactionTotal();
            total.setId(UUID.randomUUID());
            total.setEntry(posting);
            reactionDescription.toReactionTotal(total);
            total.setTotal(1);
            reactionTotalRepository.save(total);
        } else {
            total.setTotal(total.getTotal() + counterChange);
        }

        return ResponseEntity.created(URI.create("/postings/" + postingId + "/reactions" + reaction.getId()))
                .body(new ReactionInfo(reaction));
    }

}
