package org.moera.node.rest;

import java.util.UUID;
import javax.inject.Inject;

import org.moera.commons.util.LogUtil;
import org.moera.node.data.Comment;
import org.moera.node.data.CommentRepository;
import org.moera.node.global.ApiController;
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
@RequestMapping("/moera/api/postings/{postingId}/comments/{commentId}/reaction-totals")
public class CommentReactionTotalsController {

    private static Logger log = LoggerFactory.getLogger(CommentReactionTotalsController.class);

    @Inject
    private RequestContext requestContext;

    @Inject
    private CommentRepository commentRepository;

    @Inject
    private ReactionTotalOperations reactionTotalOperations;

    @GetMapping
    public ReactionTotalsInfo get(@PathVariable UUID postingId, @PathVariable UUID commentId) {
        log.info("GET /postings/{postingId}/comments/{commentId}/reaction-totals (postingId = {}, commentId = {})",
                LogUtil.format(postingId), LogUtil.format(commentId));

        Comment comment = commentRepository.findFullByNodeIdAndId(requestContext.nodeId(), commentId)
                .orElseThrow(() -> new ObjectNotFoundFailure("comment.not-found"));
        if (!comment.getPosting().getId().equals(postingId)) {
            throw new ObjectNotFoundFailure("comment.wrong-posting");
        }

        return reactionTotalOperations.getInfo(comment).getClientInfo();
    }

}
