package org.moera.node.rest;

import java.util.UUID;
import javax.inject.Inject;
import javax.transaction.Transactional;

import org.moera.commons.util.LogUtil;
import org.moera.node.data.Posting;
import org.moera.node.data.PostingRepository;
import org.moera.node.global.ApiController;
import org.moera.node.global.NoCache;
import org.moera.node.global.RequestContext;
import org.moera.node.model.ObjectNotFoundFailure;
import org.moera.node.model.ReactionTotalsInfo;
import org.moera.node.operations.ReactionTotalOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@ApiController
@RequestMapping("/moera/api/postings/{postingId}/reaction-totals")
@NoCache
public class PostingReactionTotalsController {

    private static final Logger log = LoggerFactory.getLogger(PostingReactionTotalsController.class);

    @Inject
    private RequestContext requestContext;

    @Inject
    private PostingRepository postingRepository;

    @Inject
    private ReactionTotalOperations reactionTotalOperations;

    @GetMapping
    @Transactional
    public ReactionTotalsInfo get(@PathVariable UUID postingId) {
        log.info("GET /postings/{postingId}/reaction-totals (postingId = {})", LogUtil.format(postingId));

        Posting posting = postingRepository.findFullByNodeIdAndId(requestContext.nodeId(), postingId)
                .orElseThrow(() -> new ObjectNotFoundFailure("posting.not-found"));
        if (!requestContext.isPrincipal(posting.getViewE())) {
            throw new ObjectNotFoundFailure("posting.not-found");
        }

        return reactionTotalOperations.getInfo(posting).getClientInfo();
    }

}
