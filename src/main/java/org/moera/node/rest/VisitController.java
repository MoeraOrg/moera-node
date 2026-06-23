package org.moera.node.rest;

import jakarta.inject.Inject;

import org.moera.lib.node.types.Result;
import org.moera.lib.node.types.VisitDetails;
import org.moera.lib.util.LogUtil;
import org.moera.node.global.ApiController;
import org.moera.node.global.NoCache;
import org.moera.node.operations.VisitOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@ApiController
@RequestMapping("/moera/api/visits")
@NoCache
public class VisitController {

    private static final Logger log = LoggerFactory.getLogger(VisitController.class);

    @Inject
    private VisitOperations visitOperations;

    @PostMapping
    public Result post(@RequestBody VisitDetails visitDetails) {
        log.info(
            "POST /visits (postingId = {}, commentId = {}, mediaId = {}, referrer = {})",
            LogUtil.format(visitDetails.getPostingId()),
            LogUtil.format(visitDetails.getCommentId()),
            LogUtil.format(visitDetails.getMediaId()),
            LogUtil.format(visitDetails.getReferrer(), 128)
        );

        visitDetails.validate();
        visitOperations.recordVisit(
            visitDetails.getPostingId(),
            visitDetails.getCommentId(),
            visitDetails.getMediaId(),
            visitDetails.getReferrer()
        );

        return Result.OK;
    }

}
