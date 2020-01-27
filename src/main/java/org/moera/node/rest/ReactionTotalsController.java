package org.moera.node.rest;

import java.util.Set;
import java.util.UUID;

import javax.inject.Inject;

import org.moera.commons.util.LogUtil;
import org.moera.node.data.Posting;
import org.moera.node.data.PostingRepository;
import org.moera.node.data.ReactionTotal;
import org.moera.node.data.ReactionTotalRepository;
import org.moera.node.global.ApiController;
import org.moera.node.global.RequestContext;
import org.moera.node.model.ObjectNotFoundFailure;
import org.moera.node.model.ReactionTotalsInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@ApiController
@RequestMapping("/moera/api/postings/{postingId}/reaction-totals")
public class ReactionTotalsController {

    private static Logger log = LoggerFactory.getLogger(ReactionTotalsController.class);

    @Inject
    private RequestContext requestContext;

    @Inject
    private ReactionTotalRepository reactionTotalRepository;

    @Inject
    private PostingRepository postingRepository;

    @GetMapping
    public ReactionTotalsInfo get(@PathVariable UUID postingId) {
        log.info("GET /postings/{postingId}/reaction-totals (postingId = {})", LogUtil.format(postingId));

        Posting posting = postingRepository.findByNodeIdAndId(requestContext.nodeId(), postingId).orElse(null);
        if (posting == null) {
            throw new ObjectNotFoundFailure("reaction-totals.posting-not-found");
        }

        Set<ReactionTotal> totals = reactionTotalRepository.findAllByEntryId(postingId);
        return new ReactionTotalsInfo(totals, posting.isReactionTotalsVisible() || requestContext.isAdmin()
                || requestContext.isClient(posting.getOwnerName()));
    }

}
