package org.moera.node.rest;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;

import org.moera.commons.util.LogUtil;
import org.moera.node.auth.Admin;
import org.moera.node.data.Draft;
import org.moera.node.data.DraftRepository;
import org.moera.node.data.DraftType;
import org.moera.node.global.ApiController;
import org.moera.node.global.NoCache;
import org.moera.node.global.RequestContext;
import org.moera.node.model.DraftInfo;
import org.moera.node.model.ValidationFailure;
import org.moera.node.operations.PostingOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@ApiController
@RequestMapping("/moera/api/drafts")
@NoCache
public class DraftController {

    private static Logger log = LoggerFactory.getLogger(DraftPostingController.class);

    @Inject
    private RequestContext requestContext;

    @Inject
    private DraftRepository draftRepository;

    @GetMapping
    @Admin
    public List<DraftInfo> getAll(
            @RequestParam DraftType draftType,
            @RequestParam String nodeName,
            @RequestParam(required = false) String postingId,
            @RequestParam(required = false) String commentId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer limit) {

        log.info("GET /drafts (draftType = {}, nodeName = {}, postingId = {}, commentId = {}, page = {}, limit = {})",
                LogUtil.format(draftType.toString()), LogUtil.format(nodeName), LogUtil.format(postingId),
                LogUtil.format(commentId), LogUtil.format(page), LogUtil.format(limit));

        if ((draftType == DraftType.POSTING_UPDATE || draftType == DraftType.NEW_COMMENT
                || draftType == DraftType.COMMENT_UPDATE) && StringUtils.isEmpty(postingId)) {
            throw new ValidationFailure("draft.postingId.blank");
        }
        if (draftType == DraftType.COMMENT_UPDATE && StringUtils.isEmpty(commentId)) {
            throw new ValidationFailure("draft.commentId.blank");
        }

        page = page != null ? page : 0;
        if (page < 0) {
            throw new ValidationFailure("page.invalid");
        }
        limit = limit != null && limit <= PostingOperations.MAX_POSTINGS_PER_REQUEST
                ? limit : PostingOperations.MAX_POSTINGS_PER_REQUEST;
        if (limit < 0) {
            throw new ValidationFailure("limit.invalid");
        }
        Pageable pageable = PageRequest.of(page, limit, Sort.by(Sort.Direction.DESC, "createdAt"));

        List<Draft> drafts;
        switch (draftType) {
            case NEW_POSTING:
                drafts = draftRepository.findAllNewPosting(
                        requestContext.nodeId(), requestContext.nodeName(), pageable);
                break;

            case POSTING_UPDATE: {
                Draft draft = draftRepository.findPostingUpdate(
                        requestContext.nodeId(), requestContext.nodeName(), postingId);
                drafts = draft != null ? List.of(draft) : Collections.emptyList();
                break;
            }

            case NEW_COMMENT:
                drafts = draftRepository.findAllNewComment(
                        requestContext.nodeId(), requestContext.nodeName(), postingId, pageable);
                break;

            case COMMENT_UPDATE: {
                Draft draft = draftRepository.findCommentUpdate(
                        requestContext.nodeId(), requestContext.nodeName(), postingId, commentId);
                drafts = draft != null ? List.of(draft) : Collections.emptyList();
                break;
            }

            default:
                drafts = Collections.emptyList();
                break;
        }
        return drafts.stream()
                .map(DraftInfo::new)
                .collect(Collectors.toList());
    }

}
