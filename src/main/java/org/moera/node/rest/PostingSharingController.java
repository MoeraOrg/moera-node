package org.moera.node.rest;

import java.util.UUID;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import org.moera.lib.node.types.Scope;
import org.moera.lib.node.types.SharingsSliceInfo;
import org.moera.lib.node.types.principal.Principal;
import org.moera.lib.node.types.validate.ValidationUtil;
import org.moera.lib.util.LogUtil;
import org.moera.node.data.Posting;
import org.moera.node.data.PostingRepository;
import org.moera.node.global.ApiController;
import org.moera.node.global.NoCache;
import org.moera.node.global.RequestContext;
import org.moera.node.model.ObjectNotFoundFailure;
import org.moera.node.model.SharingsSliceInfoUtil;
import org.moera.node.operations.SharingOperations;
import org.moera.node.util.SafeInteger;
import org.moera.node.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@ApiController
@RequestMapping("/moera/api/postings")
public class PostingSharingController {

    private static final Logger log = LoggerFactory.getLogger(PostingSharingController.class);

    @Inject
    private RequestContext requestContext;

    @Inject
    private PostingRepository postingRepository;

    @Inject
    private SharingOperations sharingOperations;

    @GetMapping("/{postingId}/sharings")
    @NoCache
    @Transactional
    public SharingsSliceInfo get(
        @PathVariable String postingId,
        @RequestParam(required = false) Long before,
        @RequestParam(required = false) Integer limit
    ) {
        log.info(
            "GET /postings/{postingId}/sharings (postingId = {}, before = {}, limit = {})",
            LogUtil.format(postingId), LogUtil.format(before), LogUtil.format(limit)
        );

        UUID id = Util.uuid(postingId).orElseThrow(() -> new ObjectNotFoundFailure("posting.not-found"));
        Posting posting = postingRepository.findByNodeIdAndId(requestContext.nodeId(), id)
            .orElseThrow(() -> new ObjectNotFoundFailure("posting.not-found"));
        if (!requestContext.isPrincipal(posting.getViewE(), Scope.VIEW_CONTENT)) {
            throw new ObjectNotFoundFailure("posting.not-found");
        }
        if (!requestContext.isPrincipal(posting.getViewSharingsE(), Scope.VIEW_CONTENT)) {
            return SharingsSliceInfoUtil.EMPTY;
        }

        limit = limit != null && limit <= SharingOperations.MAX_SHARINGS_PER_REQUEST
            ? limit
            : SharingOperations.MAX_SHARINGS_PER_REQUEST;
        ValidationUtil.assertion(limit >= 0, "limit.invalid");
        before = before != null ? before : SafeInteger.MAX_VALUE;
        SharingsSliceInfo sliceInfo = sharingOperations.getBefore(id, before, limit);
        Principal viewTotalSharings = posting.isOriginal()
            ? posting.getViewTotalSharingsE()
            : posting.getReceiverViewTotalSharingsE();
        sliceInfo.setTotal(
            requestContext.isPrincipal(viewTotalSharings, Scope.VIEW_CONTENT) ? posting.getTotalSharings() : 0
        );
        return sliceInfo;
    }

}
