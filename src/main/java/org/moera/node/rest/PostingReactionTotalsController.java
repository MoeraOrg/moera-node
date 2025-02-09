package org.moera.node.rest;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;

import org.moera.lib.util.LogUtil;
import org.moera.node.auth.Scope;
import org.moera.node.data.Posting;
import org.moera.node.data.PostingRepository;
import org.moera.node.global.ApiController;
import org.moera.node.global.NoCache;
import org.moera.node.global.RequestContext;
import org.moera.node.model.ObjectNotFoundFailure;
import org.moera.node.model.ReactionTotalsFilter;
import org.moera.node.model.ReactionTotalsInfo;
import org.moera.node.operations.ReactionTotalOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@ApiController
@RequestMapping("/moera/api/postings")
@NoCache
public class PostingReactionTotalsController {

    private static final Logger log = LoggerFactory.getLogger(PostingReactionTotalsController.class);

    @Inject
    private RequestContext requestContext;

    @Inject
    private PostingRepository postingRepository;

    @Inject
    private ReactionTotalOperations reactionTotalOperations;

    @GetMapping("/{postingId}/reaction-totals")
    @Transactional
    public ReactionTotalsInfo get(@PathVariable UUID postingId) {
        log.info("GET /postings/{postingId}/reaction-totals (postingId = {})", LogUtil.format(postingId));

        Posting posting = postingRepository.findByNodeIdAndId(requestContext.nodeId(), postingId)
                .orElseThrow(() -> new ObjectNotFoundFailure("posting.not-found"));
        if (!requestContext.isPrincipal(posting.getViewE(), Scope.VIEW_CONTENT)) {
            throw new ObjectNotFoundFailure("posting.not-found");
        }

        return reactionTotalOperations.getInfo(posting).getClientInfo();
    }

    @PostMapping("/reaction-totals/search")
    @Transactional
    public List<ReactionTotalsInfo> search(@Valid @RequestBody ReactionTotalsFilter filter) {
        log.info("POST /postings/reaction-totals/search");

        if (filter.getPostings() == null || filter.getPostings().isEmpty()) {
            return Collections.emptyList();
        }

        List<Posting> postings = postingRepository.findByNodeIdAndIds(requestContext.nodeId(), filter.getPostings())
                .stream()
                .filter(p -> requestContext.isPrincipal(p.getViewE(), Scope.VIEW_CONTENT))
                .collect(Collectors.toList());
        return reactionTotalOperations.getInfo(postings).stream()
                .map(ReactionTotalOperations.ReactionTotalsData::getClientInfo)
                .collect(Collectors.toList());
    }

}
